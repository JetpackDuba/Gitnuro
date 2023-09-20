extern crate notify;

use std::fmt::Debug;
use std::path::Path;
use std::sync::mpsc::{channel, RecvTimeoutError};
use std::time::Duration;
use notify::{Config, Error, ErrorKind, Event, RecommendedWatcher, RecursiveMode, Watcher};

pub fn watch_directory(
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

pub fn get_paths_from_event_result(event_result: &Result<Event, Error>) -> Option<Vec<String>> {
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
