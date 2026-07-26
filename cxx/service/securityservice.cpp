#include "securityservice.h"

SecurityService::SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao, StockSplitDao &stockSplitDao)
    : EntityService{connectionPool, securityDao}
    , stockSplitDao{stockSplitDao}
{}

QHash<domain_id, const StockSplit *> SecurityService::getSplits() {
    auto conn = Connection(connectionPool);
    return stockSplitDao.getAll(conn.db);
}

QHash<const AccountSecurityId, const AccountSecurity*> SecurityService::getAccountSecurities() const {
    auto conn = Connection(connectionPool);
    return dao.getAccountSecurities(conn.db);
}
