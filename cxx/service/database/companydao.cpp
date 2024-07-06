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

static const auto deleteCompanySql = "delete from company where id = :id";

namespace companyDao {
    QList<const Company*> getAll(QSqlDatabase db) {
        QSqlQuery query(db);
        if (!query.exec(getCompaniesSql)) {
            qCritical() << "companyDao.getAll:" << query.lastError().text();
            throw query.lastError().text();
        }
        QList<const Company*> companies;
        if (query.size() > 0) companies.reserve(query.size());
        while (query.next()) {
            companies.append(new Company(query.record()));
        }
        return companies;
    }

    QList<const Company*> update(QSqlDatabase &db, QList<Company*> companies, const QString &user) {
        QSqlQuery query(db);
        QList<const Company*> result;
        result.reserve(companies.length());
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
            result.append(company);

        }
        return result;
    }

    QList<const Company*> add(QSqlDatabase &db, QList<Company*> companies, const QString &user) {
        QSqlQuery query(db);
        QList<const Company*> result;
        result.reserve(companies.length());
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
            result.append(company);
        }
        return result;
    }

    void remove(QSqlDatabase &db, QList<const Company *> companies) {
        QSqlQuery query(db);
        query.prepare(deleteCompanySql);
        for (auto company : companies) {
            query.bindValue(":id", company->id.toInt());
            if (!query.exec()) {
                qCritical() << "companyDao.remove:" << query.lastError();
                throw query.lastError().text();
            }
        }
    }
}
