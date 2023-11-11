extern crate notify;

use std::fmt::Debug;
use std::path::{Path, PathBuf};
use std::sync::mpsc::{channel, RecvTimeoutError};
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use notify::{Config, Error, ErrorKind, Event, RecommendedWatcher, RecursiveMode, Watcher};

const MIN_TIME_IN_MS_BETWEEN_REFRESHES: u128 = 500;
const WATCH_TIMEOUT: u64 = 500;


pub fn watch_directory(
    path: String,
    git_dir_path: String,
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

    let mut paths_cached: Vec<String> = Vec::new();

    let mut last_update: u128 = 0;

    while notifier.should_keep_looping() {
        let current_time = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("We need a TARDIS to fix this")
            .as_millis();

        // Updates are batched to prevent excessive communication between Kotlin and Rust, as the
        // bridge has overhead
        if last_update != 0 && current_time > (last_update + MIN_TIME_IN_MS_BETWEEN_REFRESHES) {
            last_update = 0;

            if paths_cached.len() == 1 {
                let first_path = paths_cached.first().unwrap();
                let is_dir = PathBuf::from(first_path).is_dir();

                if !is_dir {
                    notifier.detected_change(paths_cached.to_vec());
                }
            } else if !paths_cached.is_empty() {
                println!("Sending batched events to Kotlin side");
                notifier.detected_change(paths_cached.to_vec());
            }

            paths_cached.clear();
        }

        match rx.recv_timeout(Duration::from_millis(WATCH_TIMEOUT)) {
            Ok(e) => {
                if let Some(paths) = get_paths_from_event_result(&e, &git_dir_path) {
                    let mut paths_without_dirs: Vec<String> = paths
                        .into_iter()
                        .collect();

                    paths_cached.append(&mut paths_without_dirs);

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
        .map_err(|err| err.kind.into_watcher_init_error())?;

    Ok(())
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
