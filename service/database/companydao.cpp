#include "companydao.h"

#include <QtSql>

namespace companyDao {
    QList<Company*> getAll(QSqlDatabase db) {
        QSqlQuery query(db);
        if (!query.exec("select * from company order by name")) {} // TODO show error
        QList<Company*> companies{};
        while (query.next()) {
            companies.append(new Company(query.record()));
        }
        return companies;
    }
}
