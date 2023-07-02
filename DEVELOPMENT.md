# Developing Gitnuro

## Installing libssh

Gitnuro depends on libssh being present as an external, native library (
using [JNA](https://github.com/java-native-access/jna)).
While the release GitHub workflow packages it automatically, you'll need to install it manually when developing locally,
such that it's available on the `$PATH`. See [here](https://www.libssh.org/get-it/) for one-liner installation
instructions with your OS's package manager, or manually download a binary or compile it from source and place it in the
main project directory (next to `LICENSE`) or elsewhere on your `$PATH`.

## Setting up an IDE

If you don't have another preference, the recommendation is to download and install
[IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/)
(possibly through the JetBrains Toolbox, if you have it already) as well as the
[Compose Multiplatform IDE Support](https://plugins.jetbrains.com/plugin/16541-compose-multiplatform-ide-support)
plugin.

By default, the JDK used by "IntelliJ IDEA Community Edition (2023.1.3)" is "JetBrains Runtime version 21" which is not currently supported by the project.

## Alternative: Setting up JDK for use on CLI

You don't need this if you only use the JDK installed by IntelliJ IDEA.

Check which Java version this project currently uses (`cat build.gradle.kts | grep JavaLanguageVersion`) and install it.
For instance on Debian-based systems, you'd run:

```bash
sudo apt-get install openjdk-17-jre openjdk-17-jdk libssh-dev
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

Once it works (e.g. `./gradlew build`), you may want to add that latter line to your `/etc/environment`.

## Running the app / unit tests

From the "Gradle" window in the IDE, under "Tasks" select "compose desktop > run" or "verification > test"
for the main app or unit tests, respectively, and run it.
Next time, it will already be in the "Run Configurations" at the top right of the IDE, so you
won't have to open the "Gradle" window again.
You can also run these in debug mode and set break points in the code.

Alternatively on CLI: `./gradlew run` or `./gradlew test`.
