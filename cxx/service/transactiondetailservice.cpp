#include "transactiondetailservice.h"

TransactionDetailService::TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao)
    : EntityService{pool, transactionDetailDao}
{}

QList<const SearchTransactionDetail*> TransactionDetailService::findTransactionDetails(const DetailSearchCriteria& criteria) {
    Connection conn(connectionPool);
    return dao.find(conn.db, criteria);
}