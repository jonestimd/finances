#include "companydao.h"
#include "dbdialect.h"

static const auto createTableQuery = R"(
create table company (
    id %1,
    name varchar(100) not null,
    change_date timestamp not null default current_timestamp,
    change_user varchar(50) not null,
    version bigint not null,
    constraint company_ak unique (name)
))";

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

CompanyDao::CompanyDao()
    : NamedEntityDao<Company>{getCompaniesSql, updateCompanySql, insertCompanySql, deleteCompanySql, "CompanyDao",
                              QObject::tr("Companies have been modified.  Please reload and try again.")}
{}

void CompanyDao::createTable(QSqlDatabase &db) {
    sql::exec(db, dbDialect::createTableSql(db, createTableQuery), className, "createTable");
}
