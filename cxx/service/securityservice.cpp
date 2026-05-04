#include "securityservice.h"

SecurityService::SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao)
    : EntityService{connectionPool, securityDao}
{}
