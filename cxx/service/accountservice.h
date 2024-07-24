#ifndef ACCOUNTSERVICE_H
#define ACCOUNTSERVICE_H

#include "database/connectionpool.h"
#include "model/account.h"
#include <service/model/bulkupdate.h>

class AccountService
{
    ConnectionPool *connectionPool;
public:
    AccountService(ConnectionPool *connectionPool);

    QList<const Account*> getAll();

    QList<const Account*> update(BulkUpdate<Account> &changes, const QString &user);
};

#endif // ACCOUNTSERVICE_H
