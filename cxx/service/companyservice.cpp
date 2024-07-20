#include "companyservice.h"
#include "database/companydao.h"

#include <QSqlError>

CompanyService::CompanyService(ConnectionPool *connectionPool)
    : connectionPool(connectionPool) {}

QList<const Company*> CompanyService::getAll() {
    auto conn = Connection(connectionPool);
    return companyDao::getAll(conn.db);
}

QList<const Company*> CompanyService::update(BulkUpdate<Company> &changes, const QString &user) {
    auto conn = Connection(connectionPool);
    try {
        QList<const Company*> result;
        if (!changes.updates.empty()) result += companyDao::update(conn.db, changes.updates, user);
        if (!changes.adds.empty()) result += companyDao::add(conn.db, changes.adds, user);
        if (!changes.deletes.empty()) companyDao::remove(conn.db, changes.deletes);
        return result;
    } catch(...) {
        conn.db.rollback();
        changes.onError();
        throw;
    }
}

const Company *CompanyService::add(const QString &name, const QString &user) {
    auto company = new Company(name);
    auto conn = Connection(connectionPool);
    try {
        companyDao::add(conn.db, QList{company}, user);
        return company;
    } catch(...) {
        conn.db.rollback();
        delete company;
        throw;
    }
};
