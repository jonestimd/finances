#ifndef COMPANY_DAO_H
#define COMPANY_DAO_H

#include "entitydao.h"
#include "../model/company.h"
#include <QtSql/QSqlDatabase>

class CompanyDao : public NamedEntityDao<Company> {
public:
    CompanyDao(const QString &dbType);
};

#endif // COMPANY_DAO_H
