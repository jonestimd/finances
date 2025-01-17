#ifndef SECURITYSTORE_H
#define SECURITYSTORE_H

#include "entitystore.h"
#include "service/securityservice.h"

class SecurityStore : public EntityStore<Security, SecurityService> {
public:
    SecurityStore(SecurityService *service);
};

#endif // SECURITYSTORE_H
