#include "daocontext.h"
#include <QSqlError>

namespace daocontext {
    struct ConnectionDeleter {
        QList<QString> names{};
        ~ConnectionDeleter() {
            for (const auto& name : std::as_const(names)) QSqlDatabase::removeDatabase(name);
        }
    };

    struct DbTransaction {
        QSqlDatabase db;

        DbTransaction(const ConnectionSettings& settings, ConnectionDeleter &deleter) : db{settings.connect()} {
            deleter.names.append(db.connectionName());
            if (!db.transaction()) {
                qCritical() << db.connectionName() << "begin transaction failed";
            }
        }
        ~DbTransaction() {
            if (db.lastError().isValid()) db.rollback();
            else db.commit();
            db.close();
        }
    };
}

using namespace daocontext;

DaoContext::DaoContext(const QString &dbType)
    : companyDao{dbType}
    , accountDao{dbType}
    , categoryDao{dbType}
    , transactionGroupDao{dbType}
    , payeeDao{dbType}
    , securityDao{dbType}
    , securityLotDao{dbType}
    , stockSplitDao{dbType}
    , transactionDao{dbType}
    , transactionDetailDao{dbType}
{}

void DaoContext::createDatabase(const ConnectionSettings &settings, const QString &adminUser, const QString &adminPassword, const QString &adminSocket) {
    ConnectionDeleter deleter;
    if (!settings.schema.isEmpty()) {
        auto db = settings.admin(adminUser, adminPassword, adminSocket).connect();
        deleter.names.append(db.connectionName());
        dbDialect::createSchema(db, settings.schema);
    }
    DbTransaction tx{settings.forUser(adminUser, adminPassword, adminSocket), deleter};
    createDatabaseTables(tx.db);
    if (!settings.user.isEmpty()) dbDialect::addUser(tx.db, settings);
}

void DaoContext::createDatabaseTables(const QSqlDatabase &db) {
    securityDao.createTable(db);
    companyDao.createTable(db);
    accountDao.createTable(db);
    payeeDao.createTable(db);
    categoryDao.createTable(db);
    transactionGroupDao.createTable(db);
    transactionDao.createTable(db);
    transactionDetailDao.createTable(db);
    stockSplitDao.createTable(db);
    securityLotDao.createTable(db);

    securityDao.createViews(db);
    securityDao.addCurrency(db);
}
