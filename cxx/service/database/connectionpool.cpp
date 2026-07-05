#include <QLoggingCategory>
#include <QSqlError>
#include <QThread>
#include <QtDebug>
#include <QSqlDriver>
#include <QSqlQuery>
#include <sqlite3.h>
#include "connectionpool.h"

#define MAX_ACTIVE 5

Q_LOGGING_CATEGORY(connectionPoolLogger, "connectionPool");

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

static void closeConnections(QList<QSqlDatabase>& connections) {
    QList<QString> names;
    // remove references before calling removeDatabase() so they won't be reported as "in use"
    while (!connections.isEmpty()) {
        auto db = connections.takeFirst();
        db.close();
        names.append(db.connectionName());
    }
    for (const auto& name : std::as_const(names)) QSqlDatabase::removeDatabase(name);
}

ConnectionPool::~ConnectionPool() {
    if (!isShutdown) {
        qCWarning(connectionPoolLogger, "did not call shutdown()");
        if (activeCount) qCWarning(connectionPoolLogger, "active connections: %d", activeCount);
    }
    closeConnections(idle);
}

void ConnectionPool::shutdown() {
    QMutexLocker locker(&poolMutex);
    if (!isShutdown) {
        qCDebug(connectionPoolLogger, "shutting down connection pool");
        isShutdown = true;
        while (activeCount) released.wait(&poolMutex);
        closeConnections(idle);
    } else qCWarning(connectionPoolLogger, "already shut(ting) down");
}

const QString &ConnectionPool::dbType() const {
    return settings.dbType;
}

static void load_sqlite_extension(sqlite3 *handle, const char *filename, const char *initFunction) {
    char *msg;
    sqlite3_load_extension(handle, filename, initFunction, &msg);
    if (msg) {
        qCritical() << qgetenv("LD_LIBRARY_PATH");
        qCritical() << msg;
        sqlite3_free(msg);
    }
}

int ConnectionPool::openConnections{0};

QSqlDatabase ConnectionPool::acquire() {
    Q_ASSERT(!isShutdown);
    QMutexLocker locker(&poolMutex);
    activeCount++;
    if (idle.isEmpty()) {
        if (activeCount > MAX_ACTIVE) qCWarning(connectionPoolLogger, "active connections: %d", activeCount);
        auto dbName = QString("%1(%2)").arg(name).arg(openConnections++);
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
                load_sqlite_extension(handle, "./decimal", "sqlite3_decimal_init");
                load_sqlite_extension(handle, "./sqlite_finances", "sqlite3_finances_init");
                QSqlQuery query{db};
                query.exec("pragma foreign_keys = on");
            }
        }
        return db;
    }
    auto db = idle.takeFirst();
    return db;
}

void ConnectionPool::release(QSqlDatabase db) {
    QMutexLocker locker(&poolMutex);
    activeCount--;
    idle.append(db);
    released.wakeAll();
}

Connection::Connection(ConnectionPool *pool) : pool{pool}, db{pool->acquire()} {
    if (!db.transaction()) {
        qCritical() << "ConnectionPool:" << db.connectionName() << "begin transaction failed";
    }
};

Connection::~Connection() {
    db.commit();
    pool->release(db);
}
