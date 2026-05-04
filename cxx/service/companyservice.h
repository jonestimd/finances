#ifndef COMPANYSERVICE_H
#define COMPANYSERVICE_H

#include "database/connectionpool.h"
#include "entityservice.h"
#include "database/companydao.h"

class CompanyService : public EntityService<Company, CompanyDao> {
public:
    CompanyService(ConnectionPool *connectionPool, CompanyDao &dao);
};

#endif // COMPANYSERVICE_H
