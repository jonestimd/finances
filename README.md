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

## Compiling the Application
The application can be built using [Qt Creator](https://www.qt.io/product/development-tools)
or `CMake`.

### Compiling with CMake
Prerequisites for building:
* Install [Qt](https://doc.qt.io/qt-6/get-and-install-qt.html)
* Install `CMake`
* Set the `CMAKE_PREFIX_PATH` env variable to the location of the Qt `cmake` executable
```sh
export CMAKE_PREFIX_PATH=/opt/Qt/6.7.2/gcc_64/lib/cmake
```

Run the following commands in the project root directory.
```sh
cmake -S . -B out
cmake --build out -v
```

The compiled executable will be at `out/finances`.
