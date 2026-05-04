#include <QSqlError>
#include <QThread>
#include <QtDebug>
#include <QSqlDriver>
#include <sqlite3.h>
#include "connectionpool.h"

Q_DECLARE_OPAQUE_POINTER(sqlite3*);

QString ConnectionSettings::makeName() const {
    return QString("%1:%2:%3").arg(dbType, host, schema);
}

QString ConnectionSettings::displayName() const {
    return QString("%1:%2").arg(host, schema);
}

bool ConnectionSettings::openDatabase(QSqlDatabase &db) const {
    db.setHostName(host);
    db.setPort(port);
    db.setDatabaseName(schema);
    return db.open(user, password);
}

ConnectionPool::ConnectionPool(const ConnectionSettings &settings)
    : settings{settings}
    , name{settings.makeName()}
    , displayName{settings.displayName()}
{}

const QString &ConnectionPool::dbType() const {
    return settings.dbType;
}

QSqlDatabase ConnectionPool::acquire() {
    QString dbName = nameStore.localData();
    if (dbName.isNull()) {
        QMutexLocker locker(&poolMutex);
        dbName = QString("%1(%2)").arg(name).arg(openConnections++);
        auto db = QSqlDatabase::addDatabase(settings.dbType, dbName);
        if (!db.isOpen()) {
            db.setNumericalPrecisionPolicy(QSql::HighPrecision);
            if (!settings.openDatabase(db)) {
                qCritical() << "ConnectionPool:" << dbName << db.lastError().text();
                throw QObject::tr("Failed to connect to the database.");
            }
        }
        if (settings.dbType == "QSQLITE") {
            QVariant qhandle = db.driver()->handle();
            if (qhandle.isValid()) {
                sqlite3 *handle = qhandle.value<sqlite3*>();
                sqlite3_db_config(handle, SQLITE_DBCONFIG_ENABLE_LOAD_EXTENSION, 1, nullptr);
                char *msg;
                sqlite3_load_extension(handle, "./decimal", "sqlite3_decimal_init", &msg);
                if (msg) {
                    qCritical() << qgetenv("LD_LIBRARY_PATH");
                    qCritical() << msg;
                    sqlite3_free(msg);
                }
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
