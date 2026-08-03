#ifndef SECURITYSERVICE_H
#define SECURITYSERVICE_H

#include "entityservice.h"
#include "service/database/securitydao.h"

class SecurityService : public NamedEntityService<Security, SecurityDao> {
public:
    SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao);

    QHash<domain_id, const Security*> getSecurities(const QList<domain_id> ids);
    
    QHash<const AccountSecurityId, const AccountSecurity*> getAccountSecurities() const;
    QHash<const AccountSecurityId, const AccountSecurity*> getAccountSecurities(
        const QList<domain_id> accountIds, const QList<domain_id> securityIds) const;
};

#endif // SECURITYSERVICE_H
