#include "securityservice.h"

SecurityService::SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao, StockSplitDao &stockSplitDao)
    : NamedEntityService{connectionPool, securityDao}
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

QHash<const AccountSecurityId, const AccountSecurity *> SecurityService::getAccountSecurities(
    const QList<domain_id> accountIds, const QList<domain_id> securityIds) const
{
    auto conn = Connection(connectionPool);
    return dao.getAccountSecurities(conn.db, accountIds, securityIds);
}
