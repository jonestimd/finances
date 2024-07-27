#include <QSqlError>
#include <QThread>
#include <QtDebug>

#include "connectionpool.h"
#define DB_NAME "finances_test"

QString makeName(const QString &dbType, const QString &host, const QString &schema) {
    return QString("%1:%2:%3").arg(dbType, host, schema);
}

ConnectionPool::ConnectionPool(
    const char *const dbType,
    const char *const host,
    const int port,
    const char *const schema,
    const char *const user,
    const char *const password)
    : dbType{dbType}, host{host}, port{port}, schema{schema}, user{user}, password{password}
    , name{makeName(dbType, host, schema)}
{}

QSqlDatabase ConnectionPool::acquire() {
    QString dbName = nameStore.localData();
    if (dbName.isNull()) {
        QMutexLocker locker(&poolMutex);
        dbName = QString("%1(%2)").arg(name).arg(openConnections++);
        auto db = QSqlDatabase::addDatabase(dbType, dbName);
        if (!db.isOpen()) {
            db.setHostName(host);
            db.setPort(port);
            db.setDatabaseName(schema);
            db.setNumericalPrecisionPolicy(QSql::HighPrecision);
            if (!db.open(user, password)) {
                qCritical() << "ConnectionPool:" << dbName << db.lastError().text();
                throw QObject::tr("Failed to connect to the database.");
            }
        }
        nameStore.setLocalData(dbName);
        return db;
    }
    return QSqlDatabase::database(dbName);
}

Connection::Connection(ConnectionPool *pool) : pool{pool}, db{pool->acquire()} {
    if (!db.transaction()) {
        qCritical() << "ConnectionPool:" << pool->nameStore.localData() << "begin transaction failed";
    }
};

Connection::~Connection() {
    db.commit();
}
