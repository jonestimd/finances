#include "companyservice.h"
#include "database/companydao.h"

#include <QSqlError>

CompanyService::CompanyService(ConnectionPool *connectionPool)
    : connectionPool(connectionPool) {}

QList<const Company*> CompanyService::getAll() {
    auto conn = Connection(connectionPool);
    return companyDao::getAll(conn.db);
}

QList<const Company*> CompanyService::update(QList<Company *> updates, QList<Company*> adds, QList<const Company*> deletes, const QString &user) {
    auto conn = Connection(connectionPool);
    try {
        QList<const Company*> result;
        if (!updates.empty()) result += companyDao::update(conn.db, updates, user);
        if (!adds.empty()) result += companyDao::add(conn.db, adds, user);
        if (!deletes.empty()) companyDao::remove(conn.db, deletes);
        return result;
    } catch(...) {
        conn.db.rollback();
        throw;
    }
};
