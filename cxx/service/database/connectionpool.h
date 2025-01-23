#ifndef CONNECTIONPOOL_H
#define CONNECTIONPOOL_H

#include <QtSql/QSqlDatabase>
#include <QList>
#include <QMutex>
#include <QThreadStorage>
#include <QWaitCondition>

class Connection;

class ConnectionPool {
    friend Connection;

    const QString name;
    const char *const dbType;
    const char *const host;
    const int port;
    const char *const schema;
    const char *const user;
    const char *const password;
    QThreadStorage<QString> nameStore{};
    QMutex poolMutex{};
    int openConnections{0};

    QSqlDatabase acquire();

public:
    const QString displayName;

    ConnectionPool(
        const char *const dbType,
        const char *const host,
        const int port,
        const char *const schema,
        const char *const user,
        const char *const password
    );
};

class Connection {
    ConnectionPool *pool;

public:
    QSqlDatabase db;

    Connection(ConnectionPool *pool);
    ~Connection();
};

#endif // CONNECTIONPOOL_H
