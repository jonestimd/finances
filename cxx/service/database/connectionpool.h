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

    const QString name;
    const ConnectionSettings settings;
    QThreadStorage<QString> nameStore{};
    QMutex poolMutex{};
    int openConnections{0};

    QSqlDatabase acquire();

public:
    const QString displayName;

    ConnectionPool(const ConnectionSettings &settings);
};

class Connection {
    ConnectionPool *pool;

public:
    QSqlDatabase db;

    Connection(ConnectionPool *pool);
    ~Connection();
};

#endif // CONNECTIONPOOL_H
