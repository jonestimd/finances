#include "companydao.h"

#include <QtSql>

static const auto getCompaniesSql = R"(
select c.*, count(a.id) accounts
from company c
left join account a on c.id = a.company_id
group by c.id
order by c.name)";

namespace companyDao {
    QList<Company*> getAll(QSqlDatabase db) {
        QSqlQuery query(db);
        if (!query.exec(getCompaniesSql)) {
            qCritical() << "companyDao:" << query.lastError().text();
        }
        QList<Company*> companies{};
        while (query.next()) {
            companies.append(new Company(query.record()));
        }
        return companies;
    }
}
