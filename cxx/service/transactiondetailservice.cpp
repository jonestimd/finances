#include "transactiondetailservice.h"

TransactionDetailService::TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao)
    : EntityService{pool, transactionDetailDao}
{}

QHash<domain_id, const TransactionDetail *> TransactionDetailService::getAll(domain_id accountId) {
    Connection conn(connectionPool);
    return dao.getAll(conn.db, accountId);
}
