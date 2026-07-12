#ifndef CONNECTIONPOOL_H
#define CONNECTIONPOOL_H

#include <QtSql/QSqlDatabase>
#include <QList>
#include <QMutex>
#include <QSettings>
#include <QThreadStorage>
#include <QWaitCondition>

class Connection;

struct ConnectionSettings {
    QString dbType;
    QString host;
    int port;
    QString schema;
    QString user;
    QString password;

    QString makeName() const;
    QString configName() const;
    QString displayName() const;

    bool isComplete() const;
    bool openDatabase(QSqlDatabase &db) const;
    QSqlDatabase connect() const;
private:
    friend class ConnectionPool;
    QSqlDatabase connect(int* activeCount) const;

public:
    void save(QSettings* settings) const;
    static ConnectionSettings fromConfig(const QString& name, QSettings* settings);
    static QStringList parseConfigName(const QString& name);
    static QString lastError(const QSqlDatabase &db);
    ConnectionSettings admin(const QString& user, const QString& password, const QString& socket) const;
    ConnectionSettings forUser(const QString& user, const QString& password, const QString& socket) const;

private:
    static int openConnections;
};

class ConnectionPool {
    friend Connection;

    const QString name;

    QMutex poolMutex{};
    QWaitCondition released;
    QList<QSqlDatabase> idle;
    int activeCount{0};
    bool isShutdown{false};

    QSqlDatabase acquire();
    void release(QSqlDatabase db);

public:
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
