#include <QLoggingCategory>
#include <QSqlError>
#include <QThread>
#include <QtDebug>
#include <QSqlDriver>
#include <QSqlQuery>
#include <sqlite3.h>
#include <QUrl>
#include <QFileInfo>
#include <QtEnvironmentVariables>
#include "connectionpool.h"
#include "dbdialect.h"

#define MAX_ACTIVE 5
#define CONFIG_SEP '|'
#define CONNECT_TIMEOUT "5"

Q_LOGGING_CATEGORY(connectionPoolLogger, "connectionPool");

Q_DECLARE_OPAQUE_POINTER(sqlite3*);

static int openConnections{0};

static const QHash<const QString, const char*>timeoutOptions{
    {MYSQL_DRIVER, "MYSQL_OPT_CONNECT_TIMEOUT="},
    {PG_DRIVER, "connect_timeout="},
};

static void load_sqlite_extension(sqlite3 *handle, const char *filename, const char *initFunction) {
    char *msg;
    sqlite3_load_extension(handle, filename, initFunction, &msg);
    if (msg) {
        qCCritical(connectionPoolLogger) << qgetenv("LD_LIBRARY_PATH");
        qCCritical(connectionPoolLogger) << msg;
        sqlite3_free(msg);
    }
}

int ConnectionSettings::openConnections{0};

QString ConnectionSettings::makeName() const {
    return QString("%1:%2:%3").arg(dbType, host, schema);
}

QString ConnectionSettings::configName() const {
    auto schema = this->schema;
    if (dbType == SQLITE_DRIVER) schema = QFileInfo{schema}.absoluteFilePath();
    schema = QUrl::toPercentEncoding(schema);
    if (dbType == SQLITE_DRIVER) return QStringList{dbType, schema}.join(CONFIG_SEP);
    else return QStringList{dbType, host, QString::number(port), schema}.join(CONFIG_SEP);
}

QString ConnectionSettings::displayName() const {
    return QString("%1:%2").arg(host, schema);
}

bool ConnectionSettings::isComplete() const {
    if (dbType == SQLITE_DRIVER) {
        return !schema.isEmpty() && QFile::exists(schema);
    } else {
        return !(host.isEmpty() || port < 0 || schema.isEmpty() || user.isEmpty() || password.isEmpty());
    }
}

QString timeoutOption(const QString& driver) {
    QString option = timeoutOptions.value(driver, "");    static int openConnections;

    if (!option.isEmpty()) option += qEnvironmentVariable("FINANCES_CONNECT_TIMEOUT", CONNECT_TIMEOUT);
    return option;
}

bool ConnectionSettings::openDatabase(QSqlDatabase &db) const {
    db.setHostName(host);
    db.setPort(port);
    db.setDatabaseName(schema);
    db.setUserName(user);
    db.setPassword(password);
    db.setConnectOptions(timeoutOption(db.driverName()));
    return db.open();
}

QSqlDatabase ConnectionSettings::connect() const {
    return connect(nullptr);
}

QSqlDatabase ConnectionSettings::connect(int* activeCount) const {
    auto dbName = QString("Connection (%2)").arg(openConnections++);
    auto db = QSqlDatabase::addDatabase(dbType, dbName);
    if (!db.isOpen()) {
        db.setNumericalPrecisionPolicy(QSql::HighPrecision);
        if (!openDatabase(db)) {
            auto error = ConnectionSettings::lastError(db);
            qCCritical(connectionPoolLogger) << dbName << error;
            throw QObject::tr("Connection error: %1").arg(error);
        }
    }
    if (dbType == "QSQLITE") {
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
    if (activeCount) (*activeCount)++;
    return db;
}

void ConnectionSettings::save(QSettings* settings) const {
    auto name = configName();
    if (!user.isEmpty()) {
        settings->setValue(name + "/user", user);
        settings->setValue(name + "/password", password);
    }
}

ConnectionSettings ConnectionSettings::fromConfig(const QString &name, QSettings* settings) {
    auto parts = parseConfigName(name);
    return ConnectionSettings{
        parts[0], parts[1], parts[2].toInt(), parts[3],
        settings->value(name + "/user").toString(),
        settings->value(name + "/password").toString(),
    };
}

QStringList ConnectionSettings::parseConfigName(const QString &name) {
    auto parts = name.split(CONFIG_SEP);
    if (parts[0] == SQLITE_DRIVER) {
        parts.insert(1, "");
        parts.insert(2, "0");
    }
    Q_ASSERT(parts.size() >= 4);
    parts[3] = QUrl::fromPercentEncoding(parts[3].toLocal8Bit());
    return parts;
}

QString ConnectionSettings::lastError(const QSqlDatabase &db) {
    return db.lastError().isValid() ? db.lastError().text() : QObject::tr("timed out");
}

ConnectionSettings ConnectionSettings::admin(const QString &user, const QString &password, const QString& socket) const {
    auto settings = forUser(user, password, socket);
    if (dbType == MYSQL_DRIVER) settings.schema = MYSQL_ROOT_SCHEMA;
    else if (dbType == PG_DRIVER) settings.schema = PG_ROOT_SCHEMA;
    return settings;
}

ConnectionSettings ConnectionSettings::forUser(const QString &user, const QString &password, const QString &socket) const {
    auto settings = *this;
    settings.user = user;
    settings.password = password;
    if (!socket.isEmpty()) settings.host = socket;
    return settings;
}

ConnectionPool::ConnectionPool(const ConnectionSettings &settings)
    : settings{settings}
    , name{settings.makeName()}
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
        qCDebug(connectionPoolLogger, "shutting down");
        isShutdown = true;
        while (activeCount) released.wait(&poolMutex);
        closeConnections(idle);
    } else qCWarning(connectionPoolLogger, "already shut(ting) down");
}

const QString &ConnectionPool::dbType() const {
    return settings.dbType;
}

QSqlDatabase ConnectionPool::acquire() {
    Q_ASSERT(!isShutdown);
    QMutexLocker locker(&poolMutex);
    if (idle.isEmpty()) {
        if (activeCount >= MAX_ACTIVE) qCWarning(connectionPoolLogger, "active connections: %d", activeCount);
        return settings.connect(&activeCount);
    }
    activeCount++;
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
        qCCritical(connectionPoolLogger) << db.connectionName() << "begin transaction failed";
    }
};

Connection::~Connection() {
    db.commit();
    pool->release(db);
}
