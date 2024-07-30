#include "companyservice.h"

CompanyService::CompanyService(ConnectionPool *connectionPool) : EntityService(connectionPool, companyDao) {}
