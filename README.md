# Finances
An application for tracking personal finances using a SQL database.

## Database Support

* [MySql](https://dev.mysql.com/downloads/mysql/)
* [PostgreSQL](https://www.postgresql.org/download/)
* SQLite

When using `MySql` or `PostgreSQL`, the database runs as a separate application
and must be installed separately.

When using `SQLite`, the database runs as part of the application
and does not require a separate installation.

### Compiling MySql QT plugin

* Install [Qt](https://doc.qt.io/qt-6/get-and-install-qt.html)
* Configure the build (`QT_DIR` is set to the QT version install directory, e.g. `/opt/Qt/<version>`)
```sh
cd $QT_DIR/Src/qtbase
./configure -sql-mysql
```
* Compile QT
```sh
cmake . --build . --parallel
```
* Install the plugin
```sh
cd $QT_DIR/gcc_64/plugins/sqldrivers
ln -s $QT_DIR/Src/qtbase/plugins/sqldrivers/libqsqlmysql.so .
```

## Compiling the Application

The application can be built using [Qt Creator](https://www.qt.io/product/development-tools)
or `CMake`.

### Compiling with CMake
Prerequisites for building:
* Install [Qt](https://doc.qt.io/qt-6/get-and-install-qt.html)
* Install `CMake`
* Set the `CMAKE_PREFIX_PATH` env variable to the location of the Qt `cmake` executable
```sh
export CMAKE_PREFIX_PATH=/opt/Qt/6.11.0/gcc_64/lib/cmake
```

Run the following commands in the project root directory.
```sh
cmake -S . -B out
cmake --build out -v
```

The compiled executable will be at `out/finances`.

### Running tests with CTest

```sh
cd build/Desktop_Qt_6.11.0-Debug/cxx
ctest
```

#### Test environment variables

**NOTE**: For MySQL using IPv4, use `127.0.0.1` instead of `localhost` for *host*.

<div class="highlight highlight-source-shell notranslate position-relative overflow-auto" dir="auto">
<pre>
TEST_PSQL_CONNECTION=<em>host</em>|<em>port</em>|<em>schema</em>|<em>user</em>|<em>password</em>
TEST_MYSQL_CONNECTION=<em>host</em>|<em>port</em>|<em>schema</em>|<em>user</em>|<em>password</em>
QT_LOGGING_RULES=sql.debug=false;sql.info=false
QT_DEBUG_PLUGINS=1
QT_PLUGIN_PATH=/usr/lib/x86_64-linux-gnu/qt6/plugins
</pre>
</div>