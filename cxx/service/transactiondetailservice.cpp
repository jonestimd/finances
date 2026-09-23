#include "transactiondetailservice.h"
#include "service/database/securitylotdao.h"

TransactionDetailService::TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao, SecurityLotDao& securityLotDao)
    : EntityService{pool, transactionDetailDao}
    , securityLotDao{securityLotDao}
{}

QList<const SearchTransactionDetail*> TransactionDetailService::findTransactionDetails(const DetailSearchCriteria& criteria) {
    Connection conn(connectionPool);
    return dao.find(conn.db, criteria);
}

SecuritySaleData TransactionDetailService::findAvailableLots(const TransactionDetail* sale) {
    Connection conn(connectionPool);
    auto lots = securityLotDao.findBySale(conn.db, sale);
    auto purchases = dao.findPreviousPurchases(conn.db, sale->transactionId);
    return {lots, purchases};
}