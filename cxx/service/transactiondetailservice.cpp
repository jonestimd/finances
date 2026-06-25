#include "transactiondetailservice.h"

TransactionDetailService::TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao)
    : EntityService{pool, transactionDetailDao}
{}

QHash<qlonglong, const TransactionDetail *> TransactionDetailService::getAll(qlonglong accountId) {
    Connection conn(connectionPool);
    return dao.getAll(conn.db, accountId);
}
