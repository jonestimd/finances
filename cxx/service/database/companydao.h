#ifndef COMPANY_DAO_H
#define COMPANY_DAO_H

#include "entitydao.h"
#include "../model/company.h"
#include <QtSql/QSqlDatabase>

class CompanyDao : public NamedEntityDao<Company> {
    const char *createTableSql;

public:
    CompanyDao(const QString &dbType);

    void createTable(QSqlDatabase &db) const;
};

#endif // COMPANY_DAO_H
