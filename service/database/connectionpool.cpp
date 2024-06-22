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
    const char *const schema,
    const char *const user,
    const char *const password,
    int maxConnections)
    : dbType{dbType}, host{host}, schema{schema}, user{user}, password{password}
    , name{makeName(dbType, host, schema)}, minConnections{1}, maxConnections{maxConnections}, poolCondition{}
{
    for (int i = 0; i < maxConnections; ++i) {
        availableNames.append(QString("%1[%2]").arg(name).arg(i));
    }
}

ConnectionPool::~ConnectionPool() {
    QMutexLocker locker(&poolMutex);
    while (openCount() > 0) {
        qWarning("ConnectionPool: waiting for %d connections", openCount());
        poolCondition.wait(locker.mutex(), 5000);
    }
    for (QString &name : idleConnections) {
        qDebug() << "ConnectionPool: closing" << name;
        QSqlDatabase::removeDatabase(name);
    }
};

int ConnectionPool::openCount() const {
    return maxConnections - availableNames.length() - idleConnections.length() - 1;
}

QSqlDatabase ConnectionPool::acquire() {
    QMutexLocker locker(&poolMutex);
    if (idleConnections.isEmpty() && availableNames.isEmpty()) {
        // wait for a connection
        poolCondition.wait(locker.mutex(), 5000);
    }
    if (!idleConnections.isEmpty()) {
        auto dbName = idleConnections.takeLast();
        // TODO ignore not same thread warning?
        return QSqlDatabase::database(dbName, true);
    }
    if (!availableNames.isEmpty()) {
        auto dbName = availableNames.takeLast();
        auto db = QSqlDatabase::addDatabase(dbType, dbName);
        db.setHostName(host);
        db.setDatabaseName(schema);
        db.setNumericalPrecisionPolicy(QSql::HighPrecision);
        if (!db.open(user, password)) {
            // QMessageBox::critical(nullptr, QObject::tr("Cannot open database"),
            //                       QObject::tr("Unable to establish a database connection."),
            //                       QMessageBox::Cancel);
            qCritical() << "ConnectionPool:" << db.lastError().text().data();
            throw "failed to connect to db";
        }
        return db;
    }
    throw "no more connections";
}

void ConnectionPool::release(QSqlDatabase db) {
    QMutexLocker locker(&poolMutex);
    idleConnections.append(db.connectionName());
    poolCondition.wakeOne();
}

Connection::Connection(ConnectionPool *pool) : pool{pool}, db{pool->acquire()} {
    if (!db.transaction()) {
        qWarning("ConnectionPool: begin transaction failed");
    }
};

Connection::~Connection() {
    db.commit();
    pool->release(db);
}
