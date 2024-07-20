#ifndef COMPANYSERVICE_H
#define COMPANYSERVICE_H

#include "database/connectionpool.h"
#include "model/company.h"
#include "model/bulkupdate.h"

class CompanyService
{
    ConnectionPool *connectionPool;
public:
    CompanyService(ConnectionPool *connectionPool);

    QList<const Company*> getAll();

    QList<const Company*> update(BulkUpdate<Company> &changes, const QString &user);

    const Company *add(const QString &name, const QString &user);
};

#endif // COMPANYSERVICE_H
