#include "securityservice.h"

SecurityService::SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao)
    : NamedEntityService{connectionPool, securityDao}
{}

QList<const Security*> SecurityService::getSecurities(const QList<domain_id> ids) {
    auto conn = Connection(connectionPool);
    return dao.get(conn.db, ids);
}

QHash<const AccountSecurityId, const AccountSecurity*> SecurityService::getAccountSecurities() const {
    auto conn = Connection(connectionPool);
    return dao.getAccountSecurities(conn.db);
}

QHash<const AccountSecurityId, const AccountSecurity*> SecurityService::getAccountSecurities(
    const QList<domain_id> accountIds, const QList<domain_id> securityIds) const
{
    auto conn = Connection(connectionPool);
    return dao.getAccountSecurities(conn.db, accountIds, securityIds);
}
