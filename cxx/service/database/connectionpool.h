#ifndef CONNECTIONPOOL_H
#define CONNECTIONPOOL_H

#include <QtSql/QSqlDatabase>
#include <QList>
#include <QMutex>
#include <QWaitCondition>

class Connection;

class ConnectionPool {
    friend Connection;

    const QString name;
    const char *const dbType;
    const char *const host;
    const char *const schema;
    const char *const user;
    const char *const password;
    QMutex poolMutex;
    QWaitCondition poolCondition;
    QList<QString> availableNames;
    QList<QString> idleConnections;
    int minConnections;
    int maxConnections;

    int openCount() const;
    QSqlDatabase acquire();
    void release(QSqlDatabase db);

public:
    ConnectionPool(
        const char *const dbType,
        const char *const host,
        const char *const schema,
        const char *const user,
        const char *const password,
        int maxConnections = 5
    );

    ~ConnectionPool();
};

class Connection {
    ConnectionPool *pool;

public:
    QSqlDatabase db;

    Connection(ConnectionPool *pool);
    ~Connection();
};

#endif // CONNECTIONPOOL_H
