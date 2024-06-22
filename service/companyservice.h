#ifndef COMPANYSERVICE_H
#define COMPANYSERVICE_H

#include "database/connectionpool.h"
#include "model/company.h"

class CompanyService
{
    ConnectionPool *connectionPool;
public:
    CompanyService(ConnectionPool *connectionPool);

    QList<Company*> getAll();
};

#endif // COMPANYSERVICE_H
