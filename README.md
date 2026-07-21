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

## Compile MySql and SQLite3 QT plugins

* Install [Qt](https://doc.qt.io/qt-6/get-and-install-qt.html)
* Install `libsqlite3-dev`, `libpq-dev` and `libmysqlclient-dev`
* Compile and install the drivers:

```sh
# set QT_DIR to the base directory of the QT version, e.g. /opt/Qt/6.11.1
cd $QT_DIR/Src/qtbase/src/plugins/sqldrivers
mkdir build
cd build
cmake -G Ninja .. -DCMAKE_INSTALL_PREFIX=$QT_DIR -DCMAKE_INSTALL_PREFIX=$QT_DIR/gcc_64 -DFEATURE_system_sqlite=ON
cmake --build .
cmake --install .
```

## Compiling the Application

The application can be built using [Qt Creator](https://www.qt.io/product/development-tools)
or `CMake`.

### Compiling with CMake
Prerequisites for building:
* Install [Qt](https://doc.qt.io/qt-6/get-and-install-qt.html)
* Install `CMake`
* Set the `CMAKE_PREFIX_PATH` env variable to the location of the Qt `cmake` extensions
  (e.g. `<QT install dir>/<QT version>/gcc_64/lib/cmake`)
* If using Ninja
  * install Ninja
  * or add Qt's ninja to your `PATH` (e.g. `alias ninja=${QT_DIR}/../Tools/Ninja/ninja`)
```sh
export CMAKE_PREFIX_PATH=/opt/Qt/6.11.0/gcc_64/lib/cmake
```

Run the following commands in the project root directory.
```sh
cmake -S . -B out -G Ninja
cmake --build out -v
```

The compiled executable will be at `out/finances`.

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
