#include "transactiondetailservice.h"

TransactionDetailService::TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao)
    : EntityService{pool, transactionDetailDao}
{}
