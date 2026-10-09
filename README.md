# Finances
[![CTest](https://github.com/jonestimd/finances/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/jonestimd/finances/actions/workflows/tests.yml)

An application for tracking personal finances using a SQL database.

## Database Support

* [MySql](https://dev.mysql.com/downloads/mysql/)
* [PostgreSQL](https://www.postgresql.org/download/)
* SQLite

When using `MySql` or `PostgreSQL`, the database runs as a separate application
and must be installed separately.

When using `SQLite`, the database runs as part of the application
and does not require a separate installation.

# Installing

Download and install the appropriate file for your OS from the [releases](https://github.com/jonestimd/finances/releases).

### Ubuntu/Debian
  * For Ubuntu 26.04 or later, you can use <code>finances-qt-<em>&lt;version></em>-Linux.deb</code>
  which includes dependencies on Qt packages (>= 6.10)
  * Otherwise, you can use <code>finances-qt-all-<em>&lt;version></em>-Linux.deb</code>
  which includes Qt libraries and plugins

### Other Linux
* Use <code>finances-qt-<em>&lt;version></em>-Linux.AppImage</code> which includes Qt libraries and plugins

### Windows
* Use <code>Finances-<em>&lt;version></em>-win64.exe</code> to install the program

# Building

## Linux Prerequisites

* Install [Qt](https://doc.qt.io/qt-6/get-and-install-qt.html) **or** the following packages:
  * `qt6-base-dev`
  * `qt6-tools-dev`
  * `qt6-5compat-dev`
  * `libqt6sql6`
  * `libqt6sql6-psql`
  * `libqt6sql6-mysql`
  * `libqt6widgets6`
* Install `CMake`
* Set the `CMAKE_PREFIX_PATH` env variable to the location of the Qt `cmake` extensions, for example:
```sh
# to use Qt installed at /opt/Qt
export CMAKE_PREFIX_PATH=/opt/Qt/6.11.0/gcc_64
# or to use system Qt development packages
export CMAKE_PREFIX_PATH=/usr
```
* If using Ninja
  * install Ninja
  * or add Qt's ninja to your `PATH` (e.g. `alias ninja=${QT_DIR}/../Tools/Ninja/ninja`)

* If building with Qt install instead of system libs then compile MySql and SQLite3 QT plugins
  * Install `libsqlite3-dev`, `libpq-dev` and `libmysqlclient-dev`
  * Compile and install the drivers:

```sh
# set QT_DIR to the base directory of the QT version, e.g. /opt/Qt/6.11.1
cd $QT_DIR/Src/qtbase/src/plugins/sqldrivers
mkdir build
cd build
cmake -G Ninja -S .. -DCMAKE_INSTALL_PREFIX=$QT_DIR/gcc_64 -DFEATURE_system_sqlite=ON
cmake --build .
cmake --install .
```

## Compiling the Application (on Ubuntu)

The application can be built using [Qt Creator](https://www.qt.io/product/development-tools)
or `CMake`.

### Compiling with CMake

Run the following commands in the project root directory.
```sh
# configure CMake
cmake -S . -B out -G Ninja -DCMAKE_EXPORT_COMPILE_COMMANDS=1
# compile the application
cmake --build out -v
# or to just build external projects
cmake --build . --target qdecimal
```

The compiled executable will be at `out/finances-qt`.

Run the following commands in the project root to generate package files.
When building with system libs, CPack will generate a `.deb` file with dependencies on the Qt packages.
When building with a Qt install, CPack will generate a `.deb` file and an AppImage containing the Qt
libraries.

```sh
cd out
cpack
```

### Running tests with CTest

```sh
cd out
ctest
```

#### Test environment variables

The tests use the SQLite driver to interact with the database.  By default, an
in memory database is used.  To use a file for the test database, set the
`TEST_SQLITE_FILE` environment variable to the file name.

The service tests will also verify compatability with Postgresql and MySQL if connection
settings are provided by the `TEST_PSQL_CONNECTION` and `TEST_MYSQL_CONNECTION`
environment variables.  The format for the values is:

> *host*|*port*|*schema*|*user*|*password*

**NOTE**: For MySQL using IPv4, use `127.0.0.1` instead of `localhost` for *host*.

The following environment variables also affect the tests.

```sh
# Don't show SQL queries and bindings:
QT_LOGGING_RULES=sql.debug=false;sql.info=false

# The following values can start with ~ to indicate your HOME directory
# Directory containing the Postgres unix socket file (defaults to /var/run/postgresql)
TEST_PG_SOCKET_PATH=
# Directory containing the MySQL unix socket file (not used by default)
TEST_MYSQL_SOCKET_PATH=
```

## Cross-compiling for Windows

* Clone [MXE](https://github.com/mxe/mxe).
The following instructions use `/opt/mxe` for the build environment.
```sh
cd /opt
git clone https://github.com/mxe/mxe.git
```
* Install [MXE requirements](https://mxe.cc/#requirements-debian)
```sh
cd /opt/mxe
make check-requirements
# install missing requirements
# e.g. sudo apt install ...
```
* To build the required MXE packages, run the following in project root.
```sh
./package/windows/setup-mxe.sh /opt/mxe
```

* Compile the project.
```sh
mkdir -p build/windows
cd build/windows
export PATH=$PATH:/opt/mxe/usr/bin
alias mxe-cmake=/opt/mxe/usr/bin/x86_64-w64-mingw32.shared-cmake
mxe-cmake -G Ninja -S ../..
mxe-cmake --build .

# generate the Windows installer: Finances-<version>-win64.exe
alias mxe-cpack=/opt/mxe/usr/bin/x86_64-w64-mingw32.shared-cpack
mxe-cpack
```
