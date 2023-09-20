extern crate notify;

use std::fmt::Debug;
use std::io::{Write};
use std::path::Path;
use std::sync::mpsc::{channel, RecvTimeoutError};
use std::sync::{Arc, RwLock};
use std::time::Duration;

use libssh_rs::{PollStatus, SshOption};
use notify::{Config, Error, ErrorKind, Event, RecommendedWatcher, RecursiveMode, Watcher};

uniffi::include_scaffolding!("gitnuro");

const ACCEPTED_SSH_TYPES: &str = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,ssh-rsa,rsa-sha2-512,rsa-sha2-256,ssh-dss";

fn watch_directory(
    path: String,
    notifier: Box<dyn WatchDirectoryNotifier>,
) -> Result<(), WatcherInitError> {
    // Create a channel to receive the events.
    let (tx, rx) = channel();

    // Create a watcher object, delivering debounced events.
    // The notification back-end is selected based on the platform.
    let config = Config::default();
    config.with_poll_interval(Duration::from_secs(3600));

    let mut watcher =
        RecommendedWatcher::new(tx, config).map_err(|err| err.kind.into_watcher_init_error())?;

    // Add a path to be watched. All files and directories at that path and
    // below will be monitored for changes.
    watcher
        .watch(Path::new(path.as_str()), RecursiveMode::Recursive)
        .map_err(|err| err.kind.into_watcher_init_error())?;

    while notifier.should_keep_looping() {
        match rx.recv_timeout(Duration::from_secs(1)) {
            Ok(e) => {
                if let Some(paths) = get_paths_from_event_result(&e) {
                    notifier.detected_change(paths)
                }
            }
            Err(e) => {
                if e != RecvTimeoutError::Timeout {
                    println!("Watch error: {:?}", e)
                }
            }
        }
    }

    watcher
        .unwatch(Path::new(path.as_str()))
        .map_err(|err| err.kind.into_watcher_init_error())?;

    Ok(())
}

fn get_paths_from_event_result(event_result: &Result<Event, Error>) -> Option<Vec<String>> {
    match event_result {
        Ok(event) => {
            let events: Vec<String> = event
                .paths
                .clone()
                .into_iter()
                .filter_map(|path| path.into_os_string().into_string().ok())
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

pub trait WatchDirectoryNotifier: Send + Sync + Debug {
    fn should_keep_looping(&self) -> bool;
    fn detected_change(&self, paths: Vec<String>);
}

#[derive(Debug, thiserror::Error)]
pub enum WatcherInitError {
    #[error("{error}")]
    Generic { error: String },
    #[error("IO Error")]
    Io { error: String },
    #[error("Path not found")]
    PathNotFound,
    #[error("Can not remove watch, it has not been found")]
    WatchNotFound,
    #[error("Invalid configuration")]
    InvalidConfig,
    #[error("Max files reached. Check the inotify limit")]
    MaxFilesWatch,
}

trait WatcherInitErrorConverter {
    fn into_watcher_init_error(self) -> WatcherInitError;
}

impl WatcherInitErrorConverter for ErrorKind {
    fn into_watcher_init_error(self) -> WatcherInitError {
        match self {
            ErrorKind::Generic(err) => WatcherInitError::Generic { error: err },
            ErrorKind::Io(err) => WatcherInitError::Generic {
                error: err.to_string(),
            },
            ErrorKind::PathNotFound => WatcherInitError::PathNotFound,
            ErrorKind::WatchNotFound => WatcherInitError::WatchNotFound,
            ErrorKind::InvalidConfig(_) => WatcherInitError::InvalidConfig,
            ErrorKind::MaxFilesWatch => WatcherInitError::MaxFilesWatch,
        }
    }
}

struct Session {
    pub session: RwLock<libssh_rs::Session>,
}

impl Session {
    fn new() -> Self {
        let session = libssh_rs::Session::new().unwrap();

        Session {
            session: RwLock::new(session)
        }
    }

    fn setup(&self, host: String, user: Option<String>, port: i32) {
        let session = self.session.write().unwrap();
        session.set_option(SshOption::Hostname(host)).unwrap();

        if let Some(user) = user {
            session.set_option(SshOption::User(Some(user))).unwrap();
        }

        if let Ok(port) = port.try_into() {
            session.set_option(SshOption::Port(port)).unwrap();
        }

        session.set_option(SshOption::PublicKeyAcceptedTypes(ACCEPTED_SSH_TYPES.to_string())).unwrap();
        session.options_parse_config(None).unwrap();
        session.connect().unwrap();
    }

    fn public_key_auth(&self) {
        let session = self.session.write().unwrap();
        session.userauth_public_key_auto(None, Some("")).unwrap();
    }

    fn password_auth(&self, password: String) {
        let session = self.session.write().unwrap();
        session.userauth_password(None, Some(&password)).unwrap();
    }

    fn disconnect(&self) {
        let session = self.session.write().unwrap();
        session.disconnect()
    }
}


struct Channel {
    channel: RwLock<libssh_rs::Channel>,
}

unsafe impl Send for Channel {}
unsafe impl Sync for Channel {}

impl Channel {
    fn new(session: Arc<Session>) -> Self {
        let session = session.session.write().unwrap();
        let channel = session.new_channel().unwrap();

        Channel {
            channel: RwLock::new(channel)
        }
    }
    fn open_session(&self) {
        let channel = self.channel.write().unwrap();
        channel.open_session().unwrap();
    }
    fn is_open(&self) -> bool {
        let channel = self.channel.write().unwrap();
        channel.is_open()
    }

    fn close(&self) {
        let channel = self.channel.write().unwrap();
        channel.close().unwrap();
    }

    fn request_exec(&self, command: String) {
        let channel = self.channel.write().unwrap();
        channel.request_exec(&command).unwrap();
    }

    fn poll_has_bytes(&self, is_stderr: bool) -> bool {
        let channel = self.channel.write().unwrap();
        let poll_timeout = channel.poll_timeout(is_stderr, None).unwrap();

        match poll_timeout {
            PollStatus::AvailableBytes(count) => count > 0,
            PollStatus::EndOfFile => false
        }
    }

    fn read(&self, is_stderr: bool, len: u64) ->  ReadResult {
        let ulen = len as usize;

        let channel = self.channel.write().unwrap();

        let mut buffer = vec![0; ulen];
        let read = channel.read_timeout(&mut buffer, is_stderr, None).unwrap();

        ReadResult {
            read_count: read as u64,
            data: buffer,
        }
    }

    fn write_byte(&self, byte: i32) {
        let channel = self.channel.write().unwrap();
        channel.stdin().write_all(&byte.to_ne_bytes()).unwrap();
    }

    fn write_bytes(&self, data: Vec<u8>) {
        let channel = self.channel.write().unwrap();
        channel.stdin().write_all(&data).unwrap();
    }
}

pub struct ReadResult {
    read_count: u64,
    data: Vec<u8>,
}