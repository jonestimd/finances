#include "accountservice.h"
#include "database/accountdao.h"

AccountService::AccountService(ConnectionPool *connectionPool)
    : connectionPool(connectionPool) {}

QList<const Account*> AccountService::getAll() {
    auto conn = Connection(connectionPool);
    return accountDao::getAll(conn.db);
}

QList<const Account *> AccountService::update(BulkUpdate<Account> &changes, const QString &user) {
    auto conn = Connection(connectionPool);
    try {
        QList<const Account*> result;
        if (!changes.updates.empty()) result += accountDao::update(conn.db, changes.updates, user);
        // if (!changes.adds.empty()) result += accountDao::add(conn.db, changes.adds, user);
        // if (!changes.deletes.empty()) accountDao::remove(conn.db, changes.deletes);
        return result;
    } catch(...) {
        conn.db.rollback();
        changes.onError();
        throw;
    }
};
