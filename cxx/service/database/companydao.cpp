#include "companydao.h"

#include <QtSql>

static const auto getCompaniesSql = R"(
select c.*, count(a.id) accounts
from company c
left join account a on c.id = a.company_id
group by c.id
order by c.name)";

static const auto updateCompanySql = R"(
update company
set name = :name, change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertCompanySql = R"(
insert into company (name, version, change_user, change_date)
values (:name, 0, :user, current_timestamp))";

namespace companyDao {
    QList<Company*> getAll(QSqlDatabase db) {
        QSqlQuery query(db);
        if (!query.exec(getCompaniesSql)) {
            qCritical() << "companyDao.getAll:" << query.lastError().text();
            throw query.lastError().text();
        }
        QList<Company*> companies{};
        while (query.next()) {
            companies.append(new Company(query.record()));
        }
        return companies;
    }

    QList<Company*> update(QSqlDatabase &db, QList<Company*> companies, const QString &user) {
        QSqlQuery query(db);
        query.prepare(updateCompanySql);
        query.bindValue(":user", user);
        for (auto company : companies) {
            query.bindValue(":id", company->id.toInt());
            query.bindValue(":name", company->name.toString().trimmed());
            query.bindValue(":version", company->version.toInt());
            if (!query.exec()) {
                qCritical() << "companyDao.update:" << query.lastError();
                throw query.lastError().text();
            }
            if (query.numRowsAffected() < 1) throw "stale data";
            company->version = company->version.toInt() + 1;
            company->changeUser = user;
        }
        return companies;
    }

    QList<Company*> add(QSqlDatabase &db, QList<Company*> companies, const QString &user) {
        QSqlQuery query(db);
        query.prepare(insertCompanySql);
        query.bindValue(":user", user);
        for (auto company : companies) {
            query.bindValue(":name", company->name.toString().trimmed());
            if (!query.exec()) {
                qCritical() << "companyDao.insert:" << query.lastError();
                throw query.lastError().text();
            }
            company->id = query.lastInsertId();
            company->changeUser = user;
        }
        return companies;
    }
}
