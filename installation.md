---
layout: docs
title: Installation
---
# Installation

The following files are available for download.  The corresponding installation
instructions are in the following sections.

* Linux: [finances_1.0-1_all.deb](https://github.com/jonestimd/finances/releases/download/v1.0/finances_1.0-1_all.deb)
* Windows: [finances-1.0.msi](https://github.com/jonestimd/finances/releases/download/v1.0/finances-1.0.msi)
* Manual install: [finances-1.0.zip](https://github.com/jonestimd/finances/releases/download/v1.0/finances-1.0.zip)

## Linux
The following command can be used to install the `.deb` file.

```sh
sudo dpkg -i finances_1.0-1_all.deb
```

For distributions that don't support `.deb` files, the following commands
will install the application in `/opt` and add an entry to the applications
menu in the `Office` category.

```sh
sudo unzip finances-1.0.zip -d /opt
cd /usr/local/share/applications
sudo ln -s /opt/finances/Finances.desktop .
```

## Windows
To install using the `.msi` file, double click on it in File Manager and
complete the setup wizard.

Or, use the following steps to install using the `.zip` files.
* Extract the file to the desired location (e.g. `C:\Program Files`)
* Add a shortcut to the `Start` menu with the following target
  <pre class="highlight">
  java -jar "<em>&lt;install dir&gt;</em>\finances\finances-1.0.jar"
  </pre>