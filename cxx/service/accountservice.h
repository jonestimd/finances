#ifndef ACCOUNTSERVICE_H
#define ACCOUNTSERVICE_H

#include "database/connectionpool.h"
#include "model/account.h"

class AccountService
{
    ConnectionPool *connectionPool;
public:
    AccountService(ConnectionPool *connectionPool);

    QList<const Account*> getAll();
};

#endif // ACCOUNTSERVICE_H
