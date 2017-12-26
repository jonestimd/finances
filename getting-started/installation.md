---
title: Installation
sections:
- Linux
- Windows
---
# Installation

The following files are available for download.  The corresponding installation
instructions are in the following sections.  The application requires a Java 8
or newer runtime environment (JRE).  A JRE is included with the Windows `.msi`
file.  A JRE must be installed separately for the other download types.

* Linux: [finances_1.3-1_all.deb](https://github.com/jonestimd/finances/releases/download/v1.3/finances_1.3-1_all.deb)
  (requires an installed JRE)
* Windows: [finances-1.3.msi](https://github.com/jonestimd/finances/releases/download/v1.3/finances-1.3.msi)
  (includes Oracle JRE)
* Manual install: [finances-1.3.zip](https://github.com/jonestimd/finances/releases/download/v1.3/finances-1.3.zip)
  (requires an installed JRE)

## Linux
The application requires a Java 8 compatible JRE to be installed.
To install the Open JDK JRE you can use the following command.
```sh
sudo apt-get install openjdk-8-jre
```

The following command can be used to install the `.deb` file.  After installation,
the application will be available in the start menu in the `Office` category.
```sh
sudo dpkg -i finances_1.3-1_all.deb
```

For distributions that don't support `.deb` files, the following commands
will install the application in `/opt` and add an entry to the start menu.
```sh
sudo unzip finances-1.3.zip -d /opt
cd /usr/local/share/applications
sudo ln -s /opt/finances/Finances.desktop .
```

## Windows
To install using the `.msi` file, double click on it in *File Manager* and
complete the setup wizard.  The `.msi` file includes a private JRE.
If you want to use a shared JRE you can install the application using
the `.zip` file.

Use the following steps to install using the `.zip` files.
* Extract the file to the desired location (e.g. `C:\Program Files`)
* Add a shortcut to the *Start* menu using the included `finances.ico`
  file as the icon and the following format for the target
  <pre class="highlight">
  java -jar "<em>&lt;install dir&gt;</em>\finances\finances-1.3.jar"
  </pre>