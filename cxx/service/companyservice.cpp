#include "companyservice.h"

CompanyService::CompanyService(ConnectionPool *connectionPool, CompanyDao &dao)
    : EntityService(connectionPool, dao) {}
