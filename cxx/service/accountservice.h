#ifndef ACCOUNTSERVICE_H
#define ACCOUNTSERVICE_H

#include "database/connectionpool.h"
#include "model/account.h"
#include "model/company.h"
#include <service/model/bulkupdate.h>

class AccountService
{
    ConnectionPool *connectionPool;
public:
    AccountService(ConnectionPool *connectionPool);

    QList<const Account*> getAll();

    QList<const Account*> update(BulkUpdate<Account> &changes, const QString &user, QList<const Company*> *companies = nullptr);
};

#endif // ACCOUNTSERVICE_H
