#ifndef SECURITYSERVICE_H
#define SECURITYSERVICE_H

#include "entityservice.h"
#include "service/database/securitydao.h"

class SecurityService : public EntityService<Security, SecurityDao> {
public:
    SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao);
};

#endif // SECURITYSERVICE_H
