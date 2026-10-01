Java/Maven runtime repair — 1 October 2026

The installed system Java could not load `/usr/lib/jvm/java-21-openjdk/lib/libjli.so`. Its header contained zero bytes; package checks reported 193 altered JDK files. The root filesystem had only about 16 MB free. The cause of the file damage was not established.

A Java 21.0.12.1 runtime was extracted from the existing CachyOS package archive into a private temporary directory. The archive passed its zstd integrity check. Its SHA-256 is `483cf3c7bb11b563445c9f4acf8c90866a53d492a1db834cb74edba0232c4e8f`.

Local environment changes:

- `~/.local/share/uitmerch-java/env.sh` selects the working runtime when the configured/default runtime cannot start. It verifies the cached archive hash and recreates the temporary runtime if necessary after reboot. Recovery depends on that cached archive remaining available.
- `~/.local/share/uitmerch-java/java-home` records the selected runtime path.
- `~/.mavenrc` loads the helper for Maven, including already-open terminals.
- `~/.config/zsh/.zshrc` loads the helper in new interactive terminals. This is the active rc path selected by ZDOTDIR; `~/.zshrc` was preserved.
- Original active zsh configuration was backed up under `~/.local/share/uitmerch-java/active-zshrc.before-java-fix`. No existing Maven rc was present before this repair.

Verification: Java and javac report 21.0.12.1; Maven 3.9.16 starts using that runtime. Automatic recovery from a missing temporary runtime was exercised successfully. The actual backend started with the dev profile and a random port; `/v3/api-docs` returned HTTP 200 with 66 documented paths. The verification app was then stopped. No application source changes were needed for this repair.

To start in the current terminal from `backend`:

```sh
source ~/.local/share/uitmerch-java/env.sh
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

The dev profile uses an in-memory H2 database. Starting with the default profile still requires the normal PostgreSQL, mail and storage environment configuration.

Remaining machine maintenance: the root disk is still full and the system JDK remains damaged. Free disk space before packaging/installing artifacts, then reinstall `jdk21-openjdk` with pacman. Administrator repair was not performed because sudo requires the user's password. Once system Java works, the fallback source blocks/helper can be removed; Maven will otherwise continue using the system runtime when it can start and JAVA_HOME is unset.
