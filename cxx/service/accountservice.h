#ifndef ACCOUNTSERVICE_H
#define ACCOUNTSERVICE_H

#include "database/connectionpool.h"
#include "database/companydao.h"
#include "database/accountdao.h"
#include "entityservice.h"
#include "model/company.h"

class AccountService : public EntityService<Account, AccountDao> {
    CompanyDao &companyDao;

public:
    AccountService(ConnectionPool *connectionPool, AccountDao &accountDao, CompanyDao &companyDao);

    QList<const Account*> update(BulkUpdate<Account> &changes, const QString &user, QHash<domain_id, const Company*> &companies);
};

#endif // ACCOUNTSERVICE_H
