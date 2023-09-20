use libssh_rs::{AuthStatus, PollStatus, SshOption};
use std::sync::{Arc, RwLock};
use std::io::{Write};

const ACCEPTED_SSH_TYPES: &str = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,ssh-rsa,rsa-sha2-512,rsa-sha2-256,ssh-dss";

pub struct Session {
    pub session: RwLock<libssh_rs::Session>,
}

impl Session {
    pub fn new() -> Self {
        let session = libssh_rs::Session::new().unwrap();

        Session {
            session: RwLock::new(session)
        }
    }

    pub fn setup(&self, host: String, user: Option<String>, port: Option<u16>) {
        let session = self.session.write().unwrap();
        session.set_option(SshOption::Hostname(host)).unwrap();

        if let Some(user) = user {
            session.set_option(SshOption::User(Some(user))).unwrap();
        }

        if let Some(port) = port {
            session.set_option(SshOption::Port(port)).unwrap();
        }

        session.set_option(SshOption::PublicKeyAcceptedTypes(ACCEPTED_SSH_TYPES.to_string())).unwrap();
        session.options_parse_config(None).unwrap();
        session.connect().unwrap();
    }

    pub fn public_key_auth(&self, password: String) -> AuthStatus {
        println!("Public key auth");

        let session = self.session.write().unwrap();

        let status = session.userauth_public_key_auto(None, Some(&password)).unwrap();

        println!("Status is {status:?}");

        status
    }

    pub fn password_auth(&self, password: String) -> AuthStatus {
        let session = self.session.write().unwrap();
        session.userauth_password(None, Some(&password)).unwrap()
    }

    pub fn disconnect(&self) {
        let session = self.session.write().unwrap();
        session.disconnect()
    }
}


pub struct Channel {
    channel: RwLock<libssh_rs::Channel>,
}

unsafe impl Send for Channel {}
unsafe impl Sync for Channel {}

impl Channel {
    pub fn new(session: Arc<Session>) -> Self {
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

    pub fn close(&self) {
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

    pub fn read(&self, is_stderr: bool, len: u64) ->  ReadResult {
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
        let channel = self.channel.write().unwrap();
        channel.stdin().write_all(&byte.to_ne_bytes()).unwrap();
    }

    pub fn write_bytes(&self, data: Vec<u8>) {
        let channel = self.channel.write().unwrap();
        channel.stdin().write_all(&data).unwrap();
    }
}

pub struct ReadResult {
    pub read_count: u64,
    pub data: Vec<u8>,
}