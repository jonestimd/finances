#ifndef CONNECTIONPOOL_H
#define CONNECTIONPOOL_H

#include <QtSql/QSqlDatabase>
#include <QList>
#include <QMutex>
#include <QThreadStorage>
#include <QWaitCondition>

class Connection;

struct ConnectionSettings {
    const QString dbType;
    const QString host;
    const int port;
    const QString schema;
    const QString user;
    const QString password;

    QString makeName() const;
    QString displayName() const;

    bool openDatabase(QSqlDatabase &db) const;
};

class ConnectionPool {
    friend Connection;

    static int openConnections;
    const QString name;

    QMutex poolMutex{};
    QWaitCondition released;
    QList<QSqlDatabase> idle;
    int activeCount{0};
    bool isShutdown{false};

    QSqlDatabase acquire();
    void release(QSqlDatabase db);

public:
    const QString displayName;
    const ConnectionSettings settings;

    ConnectionPool(const ConnectionSettings &settings);
    ~ConnectionPool();

    void shutdown();

    const QString &dbType() const;
};

class Connection {
    ConnectionPool *pool;

public:
    QSqlDatabase db;

    /** @brief Acquire a connection and begin a transaction. */
    Connection(ConnectionPool *pool);
    /** @brief Commit the transaction and release the connection. */
    ~Connection();
};

#endif // CONNECTIONPOOL_H
