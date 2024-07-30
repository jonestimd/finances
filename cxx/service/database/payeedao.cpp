#include "payeedao.h"

static const auto getPayeesSql = R"(
select p.*, count(tx.id) transactions
from payee p
left join tx on p.id = tx.payee_id
group by p.id
order by p.name)";

static const auto updatePayeeSql = R"(
update payee
set name = :name, change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertPayeeSql = R"(
insert into payee (name, version, change_user, change_date)
values (:name, 0, :user, current_timestamp))";

static const auto deletePayeeSql = "delete from payee where id = :id";

PayeeDao::PayeeDao()
    : EntityDao<Payee>{getPayeesSql, updatePayeeSql, insertPayeeSql, deletePayeeSql, "PayeeDao",
                       tr("Payees have been modified.  Please reload and try again.")}
{}

#include "payeedao.moc"
