# Developing Gitnuro

## Requirements

- **JDK 17 or higher**: You don't need this if you only use the JDK installed by IntelliJ IDEA. If you want to build
  using the CLI, check this [section](#alternative-setting-up-jdk-for-use-on-cli).
- **Rust:** Gitnuro is mainly written in Kotlin (JVM) but also uses Rust for some specific tasks. To set up your Rust
  environment,
  please read [its documentation](https://www.rust-lang.org/). `cargo` and `rustc` must be available in the path in
  order to build Gitnuro properly.
- **cargo-kotars:** CLI tool for integrating Rust and Kotlin. Available in source form [here](https://github.com/JetpackDuba/kotars),
  it needs to be compiled and placed somewhere in the path where Gradle will find it.
- **Perl:** Perl is required to build openssl (which is required for LibSSH to work).
- **Packages for Linux ARM64/aarch64**: You need to install the `aarch64-linux-gnu-gcc` package to cross compile the
  Rust components to ARM from x86_64. You will also need to use `rustup` to add a new
  target: `rustup target add aarch64-unknown-linux-gnu`

## Setting up an IDE

If you don't have another preference, the recommendation is to download and install
[IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/)
(possibly through the JetBrains Toolbox, if you have it already). The recommended plugins to improve the DX are:

- [Compose Multiplatform IDE Support](https://plugins.jetbrains.com/plugin/16541-compose-multiplatform-ide-support)
- [Rust Plugin](https://plugins.jetbrains.com/plugin/8182-rust) (deprecated due
  to [RustRover IDE](https://blog.jetbrains.com/rust/2023/09/13/introducing-rustrover-a-standalone-rust-ide-by-jetbrains/)
  but still works).

By default, the JDK used by "IntelliJ IDEA Community Edition (2023.1.3)" is "JetBrains Runtime version 21" which is not
currently supported by the project.

## Alternative: Setting up JDK for use on CLI

You don't need this if you only use the JDK installed by IntelliJ IDEA and build using the IDE.

Check which Java version this project currently uses (`cat build.gradle.kts | grep JavaLanguageVersion`) and install it.
For instance on Debian-based systems, you'd run:

```bash
sudo apt install openjdk-17-jre openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

Once it works (e.g. `./gradlew build`). On Linux, you may want to add that latter line to your `/etc/environment` or
user-specific files such as `.profile` or `.bashrc`.

## Running the app / unit tests

From the "Gradle" window in the IDE, under "Tasks" select "compose desktop > run" or "verification > test"
for the main app or unit tests, respectively, and run it.
Next time, it will already be in the "Run Configurations" at the top right of the IDE, so you
won't have to open the "Gradle" window again.
You can also run these in debug mode and set break points in the code.

Alternatively on CLI: `./gradlew run` or `./gradlew test`.
