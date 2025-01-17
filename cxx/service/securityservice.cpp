#include "securityservice.h"

SecurityService::SecurityService(ConnectionPool *connectionPool)
    : EntityService{connectionPool, securityDao}
{}
