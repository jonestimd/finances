#include "companyservice.h"
#include "database/companydao.h"

CompanyService::CompanyService(ConnectionPool *connectionPool)
    : connectionPool(connectionPool) {}

QList<Company*> CompanyService::getAll() {
    auto conn = Connection(connectionPool);
    return companyDao::getAll(conn.db);
};
