#ifndef COMPANYSERVICE_H
#define COMPANYSERVICE_H

#include "database/connectionpool.h"
#include "model/company.h"

class CompanyService
{
    ConnectionPool *connectionPool;
public:
    CompanyService(ConnectionPool *connectionPool);

    QList<const Company*> getAll();

    QList<const Company*> update(QList<Company*> updates, QList<Company*> adds, QList<const Company*> deletes, const QString &user);
};

#endif // COMPANYSERVICE_H
