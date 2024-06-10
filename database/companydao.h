#ifndef COMPANYDAO_H
#define COMPANYDAO_H

#include "model/company.h"
#include <QtSql/QSqlDatabase>

class CompanyDao {
    QSqlDatabase db;

public:
    CompanyDao(QSqlDatabase db);
    QList<Company*> getAll();
};

#endif // COMPANYDAO_H
