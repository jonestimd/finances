#include "companyservice.h"
#include "database/companydao.h"

#include <QSqlError>

CompanyService::CompanyService(ConnectionPool *connectionPool)
    : connectionPool(connectionPool) {}

QList<Company*> CompanyService::getAll() {
    auto conn = Connection(connectionPool);
    return companyDao::getAll(conn.db);
}

QList<Company*> CompanyService::update(QList<Company *> companies, const QString &user) {
    auto conn = Connection(connectionPool);
    try {
        return companyDao::update(conn.db, companies, user);
    } catch(...) {
        conn.db.rollback();
        throw;
    }
};
