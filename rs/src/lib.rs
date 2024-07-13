extern crate notify;

use std::io::Write;
use std::path::Path;
use std::rc::Rc;
use std::sync::mpsc::{channel, RecvTimeoutError};
use std::sync::RwLock;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use kotars::{jni_class, jni_data_class, jni_interface, jni_struct_impl};
use kotars::jni_init;
use libssh_rs::{PollStatus, SshOption};
#[allow(unused_imports)]
use libssh_rs::AuthStatus;
use notify::{Config, Error, Event, RecommendedWatcher, RecursiveMode, Watcher};

jni_init!("");

#[jni_class]
struct FileWatcher {}

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
        watch_directory(path, git_dir_path, notifier);
    }
    
    fn new() -> FileWatcher {
        FileWatcher {}
    }
}


const MIN_TIME_IN_MS_BETWEEN_REFRESHES: u128 = 500;
const WATCH_TIMEOUT: u64 = 500;


pub fn watch_directory(
    path: String,
    git_dir_path: String,
    notifier: &impl WatchDirectoryNotifier,
) {
    // Create a channel to receive the events.
    let (tx, rx) = channel();

    // Create a watcher object, delivering debounced events.
    // The notification back-end is selected based on the platform.
    let config = Config::default();
    config.with_poll_interval(Duration::from_secs(3600));

    let mut watcher =
        RecommendedWatcher::new(tx, config).expect("Init watcher failed");

    // Add a path to be watched. All files and directories at that path and
    // below will be monitored for changes.
    watcher
        .watch(Path::new(path.as_str()), RecursiveMode::Recursive)
        .expect("Start watching failed");

    let mut paths_cached: Vec<String> = Vec::new();

    let mut last_update: u128 = 0;

    while true {
        match rx.recv_timeout(Duration::from_millis(WATCH_TIMEOUT)) {
            Ok(e) => {
                if let Some(paths) = get_paths_from_event_result(&e, &git_dir_path) {
                    let mut paths_without_dirs: Vec<String> = paths
                        .into_iter()
                        .collect();

                    let first_path = paths_without_dirs.first();

                    if let Some(path) = first_path {
                        notifier.detected_change(path.clone().into());
                    }


                    last_update = SystemTime::now()
                        .duration_since(UNIX_EPOCH)
                        .expect("We need a TARDIS to fix this")
                        .as_millis();

                    println!("Event: {e:?}");
                }
            }
            Err(e) => {
                if e != RecvTimeoutError::Timeout {
                    println!("Watch error: {:?}", e);
                }
            }
        }
    }

    watcher
        .unwatch(Path::new(path.as_str()))
        .expect("Unwatch failed");

    // Ok(())
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
                    if path.is_dir() {
                        println!("Ignoring directory {path:#?}");
                        None
                    } else {
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
                    }
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
    // fn should_keep_looping(&self) -> bool;
    fn detected_change(&self, path: FileChanged);
}

const ACCEPTED_SSH_TYPES: &str = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,ssh-rsa,rsa-sha2-512,rsa-sha2-256,ssh-dss";

#[jni_class]
pub struct Session {
    pub session: RwLock<libssh_rs::Session>,
}

#[jni_struct_impl]
impl Session {
    pub fn new() -> Session {
        let session = libssh_rs::Session::new().unwrap();

        Session {
            session: RwLock::new(session)
        }
    }

    pub fn setup(&self, host: String, user: String, port: Option<i32>) {
        let session = self.session.write().unwrap();
        session.set_option(SshOption::Hostname(host)).unwrap();

        if !user.is_empty() {
            session.set_option(SshOption::User(Some(user))).unwrap();
        }

        if let Some(port) = port {
            session.set_option(SshOption::Port(port as u16)).unwrap();
        }

        session.set_option(SshOption::PublicKeyAcceptedTypes(ACCEPTED_SSH_TYPES.to_string())).unwrap();
        session.options_parse_config(None).unwrap();
        session.connect().unwrap();
    }
    //
    pub fn public_key_auth(&self, password: String) -> i32 { //AuthStatus {
        println!("Public key auth");

        let session = self.session.write().unwrap();

        let status = session.userauth_public_key_auto(None, Some(&password)).unwrap();

        println!("Status is {status:?}");

        to_int(status) // TODO remove this cast
    }
    //
    pub fn password_auth(&self, password: String) -> i32 { //AuthStatus {
        let session = self.session.write().unwrap();
        let status = session.userauth_password(None, Some(&password)).unwrap();
        to_int(status) // TODO remove this cast
    }

    pub fn disconnect(&self) {
        let session = self.session.write().unwrap();
        session.disconnect()
    }
}

#[jni_class]
pub struct Channel {
    channel: RwLock<libssh_rs::Channel>,
}

#[jni_struct_impl]
impl Channel {
    pub fn new(session: &mut Session) -> Channel {
        let session = session.session.write().unwrap();
        let channel = session.new_channel().unwrap();

        Channel {
            channel: RwLock::new(channel)
        }
    }

    pub fn open_session(&self) {
        let channel = self.channel.write().unwrap();
        channel.open_session().unwrap();
    }

    pub fn is_open(&self) -> bool {
        let channel = self.channel.write().unwrap();
        channel.is_open()
    }

    pub fn close_channel(&self) {
        let channel = self.channel.write().unwrap();
        channel.close().unwrap();
    }

    pub fn request_exec(&self, command: String) {
        let channel = self.channel.write().unwrap();
        channel.request_exec(&command).unwrap();
    }

    pub fn poll_has_bytes(&self, is_stderr: bool) -> bool {
        let channel = self.channel.write().unwrap();
        let poll_timeout = channel.poll_timeout(is_stderr, None).unwrap();

        match poll_timeout {
            PollStatus::AvailableBytes(count) => count > 0,
            PollStatus::EndOfFile => false
        }
    }

    pub fn read(&self, is_stderr: bool, len: u64) -> ReadResult {
        let ulen = len as usize;

        let channel = self.channel.write().unwrap();

        let mut buffer = vec![0; ulen];
        let read = channel.read_timeout(&mut buffer, is_stderr, None).unwrap();

        ReadResult {
            read_count: read as u64,
            data: buffer,
        }
    }

    pub fn write_byte(&self, byte: i32) {
        println!("Byte is {byte}");

        let channel = self.channel.write().unwrap();
        channel.stdin().write_all(&byte.to_ne_bytes()).unwrap();
    }

    pub fn write_bytes(&self, data: &Vec<u8>) {
        let channel = self.channel.write().unwrap();
        channel.stdin().write_all(data).unwrap();
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

#[jni_data_class]
pub struct ReadResult {
    pub read_count: u64,
    pub data: Vec<u8>,
}