#include "companyservice.h"
#include "database/companydao.h"

#include <QSqlError>

CompanyService::CompanyService(ConnectionPool *connectionPool)
    : connectionPool(connectionPool) {}

QList<Company*> CompanyService::getAll() {
    auto conn = Connection(connectionPool);
    return companyDao::getAll(conn.db);
}

QList<Company*> CompanyService::update(QList<Company *> updates, QList<Company*> adds, const QString &user) {
    auto conn = Connection(connectionPool);
    try {
        QList<Company*> result;
        if (!updates.isEmpty()) result += companyDao::update(conn.db, updates, user);
        if (!adds.isEmpty()) result += companyDao::add(conn.db, adds, user);
        return result;
    } catch(...) {
        conn.db.rollback();
        throw;
    }
};
