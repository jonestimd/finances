#ifndef COMPANYDAO_H
#define COMPANYDAO_H

#include "../model/company.h"
#include <QtSql/QSqlDatabase>

namespace companyDao {
    QList<const Company*> getAll(QSqlDatabase &db);

    QList<const Company*> update(QSqlDatabase &db, QList<Company*> companies, const QString &user);

    QList<const Company*> add(QSqlDatabase &db, QList<Company*> companies, const QString &user);

    void remove(QSqlDatabase &db, QList<const Company*> companies);
}

#endif // COMPANYDAO_H
