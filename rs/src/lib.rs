mod ssh;
mod watch_directory;

use watch_directory::{ * };
use ssh::{ * };
#[allow(unused_imports)] // Needed to map it to the enum in the UDL file
use libssh_rs::AuthStatus;

uniffi::include_scaffolding!("gitnuro");
