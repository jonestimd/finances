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

    QList<Company*> update(QList<Company*> companies, const QString &user);
};

#endif // COMPANYSERVICE_H
