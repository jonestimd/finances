#ifndef SECURITYSERVICE_H
#define SECURITYSERVICE_H

#include "entityservice.h"
#include "service/database/securitydao.h"

class SecurityService : public EntityService<Security, SecurityDao> {
public:
    SecurityService(ConnectionPool *connectionPool);
};

#endif // SECURITYSERVICE_H
