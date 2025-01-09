#ifndef COMPANY_DAO_H
#define COMPANY_DAO_H

#include "entitydao.h"
#include "../model/company.h"
#include <QtSql/QSqlDatabase>

class CompanyDao : public EntityDao<Company> {
public:
    CompanyDao();
};

static CompanyDao companyDao;

#endif // COMPANY_DAO_H
