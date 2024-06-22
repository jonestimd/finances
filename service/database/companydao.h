#ifndef COMPANYDAO_H
#define COMPANYDAO_H

#include "../model/company.h"
#include <QtSql/QSqlDatabase>

namespace companyDao {
    QList<Company*> getAll(QSqlDatabase db);
}

#endif // COMPANYDAO_H
