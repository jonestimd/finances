#include "transactionservice.h"

TransactionService::TransactionService(ConnectionPool *pool)
    : EntityService{pool, transactionDao} {}

QHash<qlonglong, const Transaction *> TransactionService::getAll(qlonglong accountId) {
    Connection conn(connectionPool);
    return dao.getAll(conn.db, accountId);
}
