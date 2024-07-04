#include "accountservice.h"
#include "database/accountdao.h"

AccountService::AccountService(ConnectionPool *connectionPool)
    : connectionPool(connectionPool) {}

QList<const Account*> AccountService::getAll() {
    auto conn = Connection(connectionPool);
    return accountDao::getAll(conn.db);
};
