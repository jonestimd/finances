#include "accountservice.h"
#include "database/accountdao.h"
#include "database/companydao.h"

AccountService::AccountService(ConnectionPool *connectionPool, AccountDao &accountDao, CompanyDao &companyDao)
    : EntityService(connectionPool, accountDao)
    , companyDao{companyDao} {}

QList<const Account*> AccountService::update(BulkUpdate<Account> &changes, const QString &user, QHash<domain_id, const Company*> &companies) {
    auto result = EntityService::update(changes, user);
    auto conn = Connection(connectionPool);
    companies = companyDao.getAll(conn.db);
    return result;
};
