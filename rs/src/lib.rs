#[allow(unused_imports)] // Needed to map it to the enum in the UDL file
use libssh_rs::AuthStatus;

use ssh::{*};
use watch_directory::{*};

mod ssh;
mod watch_directory;

uniffi::include_scaffolding!("gitnuro");
