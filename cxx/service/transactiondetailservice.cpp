#include "transactiondetailservice.h"

TransactionDetailService::TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao)
    : EntityService{pool, transactionDetailDao}
{}

QHash<qlonglong, const TransactionDetail *> TransactionDetailService::getAll(const QVariant &accountId) {
    Connection conn(connectionPool);
    return dao.getAll(conn.db, accountId);
}
