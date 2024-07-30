#ifndef COMPANY_DAO_H
#define COMPANY_DAO_H

#include "entitydao.h"
#include "../model/company.h"
#include <QtSql/QSqlDatabase>

class CompanyDao : public QObject, public EntityDao<Company> {
    Q_OBJECT
public:
    CompanyDao();
};

Q_GLOBAL_STATIC(CompanyDao, companyDao)

#endif // COMPANY_DAO_H
