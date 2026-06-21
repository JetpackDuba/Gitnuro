extern crate notify;

use std::collections::HashMap;
use std::fmt::Debug;
use std::io::Write;
use std::path::Path;
use std::sync::mpsc::{Receiver, RecvTimeoutError, channel};
use std::sync::{Arc, LockResult, RwLock, RwLockWriteGuard};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use libssh_rs::{PollStatus, SignAlgorithm, SshKey, SshOption, ssh_sign};

#[allow(unused_imports)]
use libssh_rs::AuthStatus;
use notify::event::{CreateKind, RemoveKind};
use notify::{
    Config, Error, ErrorKind, Event, EventKind, RecommendedWatcher, RecursiveMode, Watcher,
};

uniffi::setup_scaffolding!();

#[derive(uniffi::Object)]
struct FileWatcher {
    keep_watching: RwLock<bool>,
    watcher: RwLock<Option<WatcherHolder>>,
    receiver: RwLock<Option<ReceiverHolder>>,
}

struct WatcherHolder {
    watcher: Box<dyn Watcher>,
}

struct ReceiverHolder {
    receiver: Receiver<notify::Result<Event>>,
}

unsafe impl Send for WatcherHolder {}
unsafe impl Sync for WatcherHolder {}
unsafe impl Send for ReceiverHolder {}
unsafe impl Sync for ReceiverHolder {}

impl Drop for FileWatcher {
    fn drop(&mut self) {
        println!("File watcher dropped!");
    }
}

#[derive(uniffi::Record, Debug, Clone, Eq, PartialEq, Hash)]
pub struct FileChanged {
    path: String,
    file_type: FileType,
}

#[derive(uniffi::Enum, Debug, Clone, Eq, PartialEq, Hash)]
pub enum FileType {
    File,
    Directory,
}

#[uniffi::export]
impl FileWatcher {
    fn init(&self) -> i32 {
        println!("initializing file watcher");

        // Create a channel to receive the events.
        let (sender, receiver) = channel();

        // Create a watcher object, delivering debounced events.
        // The notification back-end is selected based on the platform.
        let config = Config::default();
        config.with_poll_interval(Duration::from_secs(3600));

        let watcher = RecommendedWatcher::new(sender, config);

        match watcher {
            Ok(watcher) => {
                let mut watcher_holder = self.watcher.write().unwrap();
                let mut receiver_holder = self.receiver.write().unwrap();

                *watcher_holder = Some(WatcherHolder {
                    watcher: Box::new(watcher),
                });
                *receiver_holder = Some(ReceiverHolder { receiver });
                0
            }
            Err(e) => {
                // TODO Hardcoded nums should be changed to an enum or sth similar once Kotars supports them
                let code = error_to_code(e.kind);
                code
            }
        }
    }

    fn watch(&self, notifier: Box<dyn WatchDirectoryNotifier>) {
        let receiver = self.receiver.read().unwrap();

        let receiver = match receiver.as_ref() {
            None => {
                println!("Receiver not initialized");
                return;
            }
            Some(receiver) => &receiver.receiver,
        };

        let mut paths_cached = HashMap::<FileChanged, Vec<EventKind>>::new();

        let mut last_update: u128 = 0;

        while notifier.should_keep_looping() {
            match receiver.recv_timeout(Duration::from_millis(WATCH_TIMEOUT)) {
                Ok(e) => {
                    if let Some(paths) = get_paths_from_event_result(&e) {
                        let paths_without_dirs: Vec<FileChangeEvent> = paths.into_iter().collect();

                        for path in paths_without_dirs.into_iter() {
                            let is_dir = is_directory_event(&path.event_kind);
                            let file_type = if is_dir {
                                FileType::Directory
                            } else {
                                FileType::File
                            };

                            let file_changed = FileChanged {
                                path: path.path,
                                file_type,
                            };

                            match paths_cached.get_mut(&file_changed) {
                                Some(v) => v.push(path.event_kind),
                                None => {
                                    paths_cached.insert(file_changed, vec![path.event_kind]);
                                }
                            }
                        }

                        let current_time = current_time_as_millis();

                        if last_update != 0
                            && current_time - last_update > MIN_TIME_IN_MS_BETWEEN_REFRESHES
                        {
                            process_paths_cached(&mut paths_cached, &notifier);
                            last_update = current_time_as_millis();
                        }
                    }
                }
                Err(e) => match e {
                    RecvTimeoutError::Timeout => {
                        process_paths_cached(&mut paths_cached, &notifier);
                        last_update = current_time_as_millis();
                    }
                    RecvTimeoutError::Disconnected => {
                        println!("Watch error: {:?}", e);
                    }
                },
            };
        }

        // // TODO If unwatch fails it's probably because we no longer have access to it. We probably don't care about it but double check in the future
        // let _ = watcher.unwatch(Path::new(path.as_str()));

        println!("Watch finishing...");
    }

    fn add_watch(&self, path: String, is_recursive: bool) -> i32 {
        println!("Adding watch: {path}");
        let mut watcher_holder = self.watcher.write().unwrap();
        let watcher = match watcher_holder.as_mut() {
            None => {
                println!("Watcher not initialized");
                return 1; // TODO Provide better error
            }
            Some(watcher) => &mut watcher.watcher,
        };

        // Add a path to be watched. All files and directories at that path and
        // below will be monitored for changes.

        let recursive_mode = if is_recursive {
            RecursiveMode::Recursive
        } else {
            RecursiveMode::NonRecursive
        };

        let res = watcher.watch(Path::new(path.as_str()), recursive_mode);

        if let Err(e) = res {
            // TODO Hardcoded nums should be changed to an enum or sth similar once Kotars supports them
            error_to_code(e.kind)
        } else {
            0
        }
    }

    fn remove_watch(&self, path: String) -> i32 {
        println!("Removing watch: {path}");
        let mut watcher_holder = self.watcher.write().unwrap();
        let watcher = match watcher_holder.as_mut() {
            None => {
                println!("Watcher not initialized");
                return 1; // TODO Provide better error
            }
            Some(watcher) => &mut watcher.watcher,
        };

        // Add a path to be watched. All files and directories at that path and
        // below will be monitored for changes.
        let res = watcher.unwatch(Path::new(path.as_str()));

        if let Err(e) = res {
            // TODO Hardcoded nums should be changed to an enum or sth similar once Kotars supports them
            error_to_code(e.kind)
        } else {
            0
        }
    }
    #[uniffi::constructor]
    fn new() -> FileWatcher {
        FileWatcher {
            keep_watching: RwLock::from(true),
            watcher: RwLock::from(None),
            receiver: RwLock::from(None),
        }
    }

    fn stop_watching(&self) {
        println!("Keep watching set to false");
        *self.keep_watching.write().unwrap() = false
    }
}

fn remove_temporary_files(changes: &mut HashMap<FileChanged, Vec<EventKind>>) -> Vec<FileChanged> {
    let paths: Vec<FileChanged> = changes
        .iter()
        .filter_map(|(key, value)| {
            let index_created = value
                .iter()
                .position(|v| matches!(v, EventKind::Create(_)));

            let index_removed = value
                .iter()
                .position(|v| matches!(v, EventKind::Remove(_)));

            // If a file has been created and removed before passing it to kotlin,  filter it out,
            // we don't care about it as it's a temporary file.
            // If a file has been first removed and then created, then it shouldn't be marked as
            // temporary file.
            if let (Some(index_created), Some(index_removed)) = (index_created, index_removed)
                && index_created < index_removed
            {
                println!(
                    "Removing entry {} as it looks like a temporary file.",
                    key.path
                );
                None
            } else {
                Some(key.clone())
            }
        })
        .collect();

    paths
}

fn is_directory_event(kind: &EventKind) -> bool {
    match kind {
        EventKind::Create(CreateKind::Folder) | EventKind::Remove(RemoveKind::Folder) => true,
        _ => false,
    }
}

fn process_paths_cached(
    paths_cached: &mut HashMap<FileChanged, Vec<EventKind>>,
    notifier: &Box<dyn WatchDirectoryNotifier>,
) {
    let paths_to_send: Vec<FileChanged> = remove_temporary_files(paths_cached);
    paths_cached.clear();

    if !paths_to_send.is_empty() {
        println!(
            "Sending a total of {} paths cached to Kotlin side",
            paths_to_send.len()
        );
        notifier.detected_change(paths_to_send);
    }
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

pub fn get_paths_from_event_result(
    event_result: &Result<Event, Error>,
) -> Option<Vec<FileChangeEvent>> {
    match event_result {
        Ok(event) => match event.kind {
            EventKind::Create(_) | EventKind::Modify(_) | EventKind::Remove(_) => {
                let events: Vec<FileChangeEvent> = get_event_paths(event);

                if events.is_empty() {
                    None
                } else {
                    Some(events)
                }
            }
            _ => None,
        },
        Err(err) => {
            println!("{:?}", err);
            None
        }
    }
}

fn get_event_paths(event: &Event) -> Vec<FileChangeEvent> {
    event
        .paths
        .clone()
        .into_iter()
        .filter_map(|path| {
            let path_str = path.into_os_string().into_string().ok()?;

            let file_change_event = FileChangeEvent {
                path: path_str,
                event_kind: event.kind,
            };

            Some(file_change_event)
        })
        .collect()
}

pub struct FileChangeEvent {
    pub path: String,
    pub event_kind: EventKind,
}

#[uniffi::export(callback_interface)]
pub trait WatchDirectoryNotifier: Send + Sync + Debug {
    fn should_keep_looping(&self) -> bool;
    fn detected_change(&self, paths: Vec<FileChanged>);
    fn on_error(&self, code: i32);
}

const ACCEPTED_SSH_TYPES: &str = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,ssh-rsa,rsa-sha2-512,rsa-sha2-256,ssh-dss";

#[derive(uniffi::Object)]
pub struct Session {
    session_holder: Option<SessionHolder>,
}

pub struct SessionHolder {
    pub session: RwLock<libssh_rs::Session>,
}

#[uniffi::export]
impl Session {
    #[uniffi::constructor]
    pub fn new() -> Session {
        let session = libssh_rs::Session::new().unwrap();

        let session_holder = SessionHolder {
            session: RwLock::new(session),
        };

        Session {
            session_holder: Some(session_holder),
        }
    }

    pub fn setup(&self, host: String, user: String, port: Option<i32>) -> String {
        let session_holder = self.session_holder.as_ref().unwrap();
        let session = match session_holder.session.write() {
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

        if let Err(e) = session.set_option(SshOption::PublicKeyAcceptedTypes(
            ACCEPTED_SSH_TYPES.to_string(),
        )) {
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

    pub fn public_key_auth(&self, password: String) -> i32 {
        //AuthStatus {
        println!("Public key auth");
        let session_holder = self.session_holder.as_ref().unwrap();
        let session = match session_holder.session.write() {
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

    pub fn password_auth(&self, password: String) -> i32 {
        //AuthStatus {
        let session_holder = self.session_holder.as_ref().unwrap();
        let session = match session_holder.session.write() {
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
        let session_holder = self.session_holder.as_ref().unwrap();
        match session_holder.session.write() {
            Ok(session) => session.disconnect(),
            Err(e) => println!("Session disconnection failed due to: {e:#?}"),
        };
    }
}

pub struct ChannelHolder {
    channel: RwLock<libssh_rs::Channel>,
}

unsafe impl Send for ChannelHolder {}
unsafe impl Sync for ChannelHolder {}

#[derive(uniffi::Object)]
pub struct Channel {
    channel: Option<ChannelHolder>,
}

#[uniffi::export]
impl Channel {
    #[uniffi::constructor]
    pub fn new(session: Arc<Session>) -> Channel {
        let session_holder = session.as_ref().session_holder.as_ref().unwrap();
        let session = session_holder.session.read().unwrap();
        let channel = session.new_channel().unwrap();

        let channel_holder = ChannelHolder {
            channel: RwLock::new(channel),
        };

        Channel {
            channel: Some(channel_holder),
        }
    }

    pub fn open_session(&self) -> String {
        let channel_holder = self.channel.as_ref().unwrap();
        let channel = match channel_holder.channel.write() {
            Ok(c) => c,
            Err(e) => return format!("{e:#}"),
        };

        if let Err(e) = channel.open_session() {
            let message = libssh_error_to_message(&e);
            return format!("Channel open session failed: {message}");
        };

        String::new()
    }

    pub fn is_open(&self) -> bool {
        let channel_holder = self.channel.as_ref().unwrap();
        let channel = match channel_holder.channel.write() {
            Ok(s) => s,
            Err(e) => {
                println!("Something failed obtaining write channel: {e:?}");
                return false;
            }
        };

        channel.is_open()
    }

    pub fn close_channel(&self) -> String {
        let channel_holder = self.channel.as_ref().unwrap();
        let channel = match channel_holder.channel.write() {
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
        let channel_holder = self.channel.as_ref().unwrap();
        let channel = match channel_holder.channel.write() {
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
        let channel_holder = self.channel.as_ref().unwrap();
        let channel = match channel_holder.channel.write() {
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
            PollStatus::EndOfFile => false,
        }
    }

    pub fn read(&self, is_stderr: bool, len: u64) -> Option<ReadResult> {
        let ulen = len as usize;

        let channel = match self.channel.as_ref()?.channel.write() {
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

        Some(ReadResult {
            read_count: read as u64,
            data: buffer,
        })
    }

    pub fn write_byte(&self, byte: i32) -> String {
        let channel = match self.get_channel() {
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
        let channel = match self.get_channel() {
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

impl Channel {
    fn get_channel(&'_ self) -> LockResult<RwLockWriteGuard<'_, libssh_rs::Channel>> {
        self.channel.as_ref().unwrap().channel.write()
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

#[derive(uniffi::Record)]
pub struct ReadResult {
    pub read_count: u64,
    pub data: Vec<u8>,
}

#[derive(uniffi::Object)]
pub struct Signing;

#[uniffi::export]
impl Signing {
    #[uniffi::constructor]
    fn new() -> Signing {
        Signing {}
    }

    fn sign_data(&self, data: &Vec<u8>, key: String, password: String) -> String {
        let key =
            SshKey::from_privkey_file(&key, Some(&password)).expect("Unable to load private key");
        ssh_sign(&data, key, SignAlgorithm::SHA512, None, "git".to_string())
            .expect("Unable to sign data")
    }
}
