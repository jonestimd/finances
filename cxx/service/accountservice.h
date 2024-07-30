#ifndef ACCOUNTSERVICE_H
#define ACCOUNTSERVICE_H

#include "database/connectionpool.h"
#include "database/accountdao.h"
#include "entityservice.h"
#include "model/company.h"

class AccountService : public EntityService<Account, AccountDao>
{
public:
    AccountService(ConnectionPool *connectionPool);

    QList<const Account*> update(BulkUpdate<Account> &changes, const QString &user, QList<const Company*> *companies = nullptr);
};

#endif // ACCOUNTSERVICE_H
