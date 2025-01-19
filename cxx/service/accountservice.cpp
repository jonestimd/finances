#include "accountservice.h"
#include "database/accountdao.h"
#include "database/companydao.h"

AccountService::AccountService(ConnectionPool *connectionPool) : EntityService(connectionPool, accountDao) {}

QList<const Account*> AccountService::update(BulkUpdate<Account> &changes, const QString &user, QHash<qlonglong, const Company*> &companies) {
    auto result = EntityService::update(changes, user);
    auto conn = Connection(connectionPool);
    companies = companyDao.getAll(conn.db);
    return result;
};
