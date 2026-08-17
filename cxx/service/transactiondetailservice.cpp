#include "transactiondetailservice.h"

TransactionDetailService::TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao)
    : EntityService{pool, transactionDetailDao}
{}

QList<const SearchTransactionDetail*> TransactionDetailService::findTransactionDetails(const QString& text) {
    Connection conn(connectionPool);
    return dao.findByString(conn.db, text);
}