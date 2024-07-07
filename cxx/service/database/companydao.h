#ifndef COMPANYDAO_H
#define COMPANYDAO_H

#include "../model/company.h"
#include <QtSql/QSqlDatabase>

namespace companyDao {
    QList<Company*> getAll(QSqlDatabase db);

    QList<Company*> update(QSqlDatabase &db, QList<Company*> companies, const QString &user);

    QList<Company*> add(QSqlDatabase &db, QList<Company*> companies, const QString &user);
}

#endif // COMPANYDAO_H
