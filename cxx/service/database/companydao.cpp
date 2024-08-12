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

CompanyDao::CompanyDao()
    : EntityDao<Company>{getCompaniesSql, updateCompanySql, insertCompanySql, deleteCompanySql, "CompanyDao",
                         tr("Companies have been modified.  Please reload and try again.")}
{}
