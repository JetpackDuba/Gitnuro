extern crate notify;

use std::io::Write;
use std::path::Path;
use std::sync::mpsc::{channel, RecvTimeoutError};
use std::sync::RwLock;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use kotars::{jni_class, jni_data_class, jni_interface, jni_struct_impl};
use kotars::jni_init;
use libssh_rs::{PollStatus, SshOption};
#[allow(unused_imports)]
use libssh_rs::AuthStatus;
use notify::{Config, Error, ErrorKind, Event, RecommendedWatcher, RecursiveMode, Watcher};

mod t;

jni_init!("");

#[jni_class]
struct FileWatcher {
    keep_watching: bool,
}

impl Drop for FileWatcher {
    fn drop(&mut self) {
        println!("File watcher dropped!");
    }
}

#[jni_data_class]
struct FileChanged {
    path: String,
}

impl From<String> for FileChanged {
    fn from(value: String) -> Self {
        FileChanged { path: value }
    }
}

#[jni_struct_impl]
impl FileWatcher {
    fn watch(
        &self,
        path: String,
        git_dir_path: String,
        notifier: &impl WatchDirectoryNotifier,
    ) {
        println!("Starting to watch directory {path}");

        // Create a channel to receive the events.
        let (tx, rx) = channel();

        // Create a watcher object, delivering debounced events.
        // The notification back-end is selected based on the platform.
        let config = Config::default();
        config.with_poll_interval(Duration::from_secs(3600));

        let watcher =
            RecommendedWatcher::new(tx, config);

        let mut watcher = match watcher {
            Ok(watcher) => watcher,
            Err(e) => {
                // TODO Hardcoded nums should be changed to an enum or sth similar once Kotars supports them
                let code = error_to_code(e.kind);
                notifier.on_error(code);
                return;
            }
        };

        // Add a path to be watched. All files and directories at that path and
        // below will be monitored for changes.
        let res = watcher
            .watch(Path::new(path.as_str()), RecursiveMode::Recursive);

        if let Err(e) = res {

            // TODO Hardcoded nums should be changed to an enum or sth similar once Kotars supports them
            let code = error_to_code(e.kind);
            notifier.on_error(code);
            return;
        }

        let mut paths_cached: Vec<String> = Vec::new();
        let mut last_update: u128 = 0;
        while notifier.should_keep_looping() {
            match rx.recv_timeout(Duration::from_millis(WATCH_TIMEOUT)) {
                Ok(e) => {
                    if let Some(paths) = get_paths_from_event_result(&e, &git_dir_path) {
                        let mut paths_without_dirs: Vec<String> = paths
                            .into_iter()
                            .collect();

                        paths_cached.append(&mut paths_without_dirs);

                        let current_time = current_time_as_millis();

                        if last_update != 0 &&
                            current_time - last_update > MIN_TIME_IN_MS_BETWEEN_REFRESHES &&
                            !paths_cached.is_empty() {
                            notify_paths_changed(&mut paths_cached, notifier);
                            last_update = current_time_as_millis();
                        }

                        println!("Event: {e:?}");
                    }
                }
                Err(e) => {
                    match e {
                        RecvTimeoutError::Timeout => {
                            if !paths_cached.is_empty() {
                                notify_paths_changed(&mut paths_cached, notifier);
                            }
                            last_update = current_time_as_millis();
                        }
                        RecvTimeoutError::Disconnected => {
                            println!("Watch error: {:?}", e);
                        }
                    }
                }
            };
        }

        // TODO If unwatch fails it's probably because we no longer have access to it. We probably don't care about it but double check in the future
        let _ = watcher
            .unwatch(Path::new(path.as_str()));

        println!("Watch finishing...");
    }

    fn new() -> FileWatcher {
        FileWatcher {
            keep_watching: true,
        }
    }

    fn stop_watching(&mut self) {
        println!("Keep watching set to false");
        self.keep_watching = false
    }
}

fn notify_paths_changed(paths_cached: &mut Vec<String>, notifier: &impl WatchDirectoryNotifier) {
    println!("Sending paths cached to Kotlin side");
    let paths = paths_cached.clone();
    paths_cached.clear(); // TODO Until this is executed, items are duplicated in memory, this can be easily optimized later on
    notifier.detected_change(paths);
}

fn current_time_as_millis() -> u128 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .expect("We need a TARDIS to fix this")
        .as_millis()
}

const MIN_TIME_IN_MS_BETWEEN_REFRESHES: u128 = 500;
const WATCH_TIMEOUT: u64 = 500;


fn error_to_code(error_kind: ErrorKind) -> i32 {
    match error_kind {
        ErrorKind::Generic(_) => 1,
        ErrorKind::Io(_) => 2,
        ErrorKind::PathNotFound => 3,
        ErrorKind::WatchNotFound => 4,
        ErrorKind::InvalidConfig(_) => 5,
        ErrorKind::MaxFilesWatch => 6,
    }
}

pub fn get_paths_from_event_result(event_result: &Result<Event, Error>, git_dir_path: &str) -> Option<Vec<String>> {
    match event_result {
        Ok(event) => {
            let events: Vec<String> = event
                .paths
                .clone()
                .into_iter()
                .filter_map(|path| {
                    // Directories are not tracked by Git so we don't care about them (just about their content)
                    // We won't be able to check if it's a dir if it has been deleted but that's good enough
                    // if path.is_dir() {
                    //     println!("Ignoring directory {path:#?}");
                    //     None
                    // } else {
                    let path_str = path.into_os_string()
                        .into_string()
                        .ok()?;

                    // JGit may create .probe-UUID files for its internal stuff, we don't care about it
                    let probe_prefix = format!("{git_dir_path}.probe-");
                    if path_str.starts_with(probe_prefix.as_str()) {
                        None
                    } else {
                        Some(path_str)
                    }
                    // }
                })
                .collect();

            if events.is_empty() {
                None
            } else {
                Some(events)
            }
        }
        Err(err) => {
            println!("{:?}", err);
            None
        }
    }
}

#[jni_interface]
pub trait WatchDirectoryNotifier {
    fn should_keep_looping(&self) -> bool;
    fn detected_change(&self, paths: Vec<String>);
    fn on_error(&self, code: i32);
}

const ACCEPTED_SSH_TYPES: &str = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,ssh-rsa,rsa-sha2-512,rsa-sha2-256,ssh-dss";

#[jni_class]
pub struct Session {
    pub session: RwLock<libssh_rs::Session>,
}

#[jni_struct_impl]
impl Session {
    pub fn new() -> Option<Session> {
        let session = libssh_rs::Session::new().ok()?;

        Some(
            Session {
                session: RwLock::new(session)
            }
        )
    }

    pub fn setup(&self, host: String, user: String, port: Option<i32>) -> String {
        let session = match self.session.write() {
            Ok(s) => s,
            Err(e) => {
                return format!("Something failed obtaining write session: {e:?}");
            }
        };

        if let Err(e) = session.set_option(SshOption::Hostname(host)) {
            let message = libssh_error_to_message(&e);
            return format!("SSH Hostname option failed: {message}");
        }

        if !user.is_empty() {
            if let Err(e) = session.set_option(SshOption::User(Some(user))) {
                let message = libssh_error_to_message(&e);
                return format!("SSH User option failed: {message}");
            }
        }

        if let Some(port) = port {
            if let Err(e) = session.set_option(SshOption::Port(port as u16)) {
                let message = libssh_error_to_message(&e);
                return format!("SSH Port option failed: {message}");
            }
        }

        if let Err(e) = session.set_option(SshOption::PublicKeyAcceptedTypes(ACCEPTED_SSH_TYPES.to_string())) {
            let message = libssh_error_to_message(&e);
            return format!("SSH Public keys option failed: {message}");
        }

        if let Err(e) = session.options_parse_config(None) {
            let message = libssh_error_to_message(&e);
            return format!("SSH Configuration parsing failed: {message}");
        }

        if let Err(e) = session.connect() {
            let message = libssh_error_to_message(&e);
            return format!("Server connection failed: {message}");
        }

        String::new()
    }

    pub fn public_key_auth(&self, password: String) -> i32 { //AuthStatus {
        println!("Public key auth");

        let session = match self.session.write() {
            Ok(s) => s,
            Err(e) => {
                println!("Something failed obtaining write session: {e:?}");
                return -1;
            }
        };

        let status = match session.userauth_public_key_auto(None, Some(&password)) {
            Ok(s) => s,
            Err(e) => {
                let message = libssh_error_to_message(&e);
                println!("Something failed when using public key auto auth: {message}");
                return -2;
            }
        };

        println!("Status is {status:?}");

        to_int(status) // TODO remove this cast
    }

    pub fn password_auth(&self, password: String) -> i32 { //AuthStatus {
        let session = match self.session.write() {
            Ok(s) => s,
            Err(e) => {
                println!("Something failed obtaining write session: {e:?}");
                return -1;
            }
        };

        let status = match session.userauth_password(None, Some(&password)) {
            Ok(s) => s,
            Err(e) => {
                let message = libssh_error_to_message(&e);
                println!("An error occurred when using user auth with password: {message}");
                return -2;
            }
        };
        to_int(status) // TODO remove this cast
    }

    pub fn disconnect(&self) {
        match self.session.write() {
            Ok(session) => session.disconnect(),
            Err(e) => println!("Session disconnection failed due to: {e:#?}"),
        };
    }
}

#[jni_class]
pub struct Channel {
    channel: RwLock<libssh_rs::Channel>,
}

#[jni_struct_impl]
impl Channel {
    pub fn new(session: &mut Session) -> Option<Channel> {
        let session = session.session.write().ok()?;
        let channel = session.new_channel().ok()?;

        Some(
            Channel {
                channel: RwLock::new(channel)
            }
        )
    }

    pub fn open_session(&self) -> String {
        let channel = match self.channel.write() {
            Ok(c) => c,
            Err(e) => return format!("{e:#}")
        };

        if let Err(e) = channel.open_session() {
            let message = libssh_error_to_message(&e);
            return format!("Channel open session failed: {message}");
        };

        String::new()
    }

    pub fn is_open(&self) -> bool {
        let channel = match self.channel.write() {
            Ok(s) => s,
            Err(e) => {
                println!("Something failed obtaining write channel: {e:?}");
                return false;
            }
        };

        channel.is_open()
    }

    pub fn close_channel(&self) -> String {
        let channel = match self.channel.write() {
            Ok(s) => s,
            Err(e) => {
                return format!("Something failed obtaining write channel: {e:?}");
            }
        };


        match channel.close() {
            Ok(_) => String::new(),
            Err(e) => {
                let message = libssh_error_to_message(&e);
                format!("Channel closing failed: {message}")
            }
        }
    }

    pub fn request_exec(&self, command: String) -> String {
        let channel = match self.channel.write() {
            Ok(s) => s,
            Err(e) => {
                return format!("Something failed obtaining write channel: {e:?}");
            }
        };

        match channel.request_exec(&command) {
            Ok(_) => String::new(),
            Err(e) => {
                let message = libssh_error_to_message(&e);
                format!("Channel request exec failed: {message}")
            }
        }
    }

    pub fn poll_has_bytes(&self, is_stderr: bool) -> bool {
        let channel = match self.channel.write() {
            Ok(s) => s,
            Err(e) => {
                println!("Something failed obtaining write channel: {e:?}");
                return false;
            }
        };

        let poll_timeout = match channel.poll_timeout(is_stderr, None) {
            Ok(timeout) => timeout,
            Err(e) => {
                let message = libssh_error_to_message(&e);
                println!("{message}");
                return false;
            }
        };

        match poll_timeout {
            PollStatus::AvailableBytes(count) => count > 0,
            PollStatus::EndOfFile => false
        }
    }

    pub fn read(&self, is_stderr: bool, len: u64) -> Option<ReadResult> {
        let ulen = len as usize;

        let channel = match self.channel.write() {
            Ok(s) => s,
            Err(e) => {
                println!("Something failed obtaining write channel: {e:?}");
                return None;
            }
        };

        let mut buffer = vec![0; ulen];
        let read = match channel.read_timeout(&mut buffer, is_stderr, None) {
            Ok(s) => s,
            Err(e) => {
                let message = libssh_error_to_message(&e);
                println!("Something failed reading SSH channel: {message}");
                return None;
            }
        };

        Some(
            ReadResult {
                read_count: read as u64,
                data: buffer,
            }
        )
    }

    pub fn write_byte(&self, byte: i32) -> String {
        let channel = match self.channel.write() {
            Ok(c) => c,
            Err(e) => {
                return format!("Something failed obtaining write channel: {e:?}");
            }
        };

        let res = channel.stdin().write_all(&byte.to_ne_bytes());

        match res {
            Ok(_) => String::new(),
            Err(e) => {
                format!("Something failed writing to channel STDIN: {e:?}")
            }
        }
    }

    pub fn write_bytes(&self, data: &Vec<u8>) -> String {
        let channel = match self.channel.write() {
            Ok(c) => c,
            Err(e) => {
                return format!("Something failed obtaining write channel: {e:?}");
            }
        };

        let res = channel.stdin().write_all(data);

        match res {
            Ok(_) => String::new(),
            Err(e) => {
                format!("Something failed writing to channel STDIN: {e:?}")
            }
        }
    }
}

fn to_int(auth_status: AuthStatus) -> i32 {
    match auth_status {
        AuthStatus::Success => 1,
        AuthStatus::Denied => 2,
        AuthStatus::Partial => 3,
        AuthStatus::Info => 4,
        AuthStatus::Again => 5,
    }
}

fn libssh_error_to_message(err: &libssh_rs::Error) -> String {
    match err {
        libssh_rs::Error::RequestDenied(message) => message.clone(),
        libssh_rs::Error::Fatal(message) => message.clone(),
        libssh_rs::Error::TryAgain => "Something went wrong, please try again".to_string(),
        libssh_rs::Error::Sftp(_) => "Sftp not supported".to_string(),
    }
}

#[jni_data_class]
pub struct ReadResult {
    pub read_count: u64,
    pub data: Vec<u8>,
}