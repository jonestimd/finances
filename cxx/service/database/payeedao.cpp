#include "payeedao.h"

static const auto getPayeesSql = R"(
with summary as (
    select payee_id, count(*) transactions
    from tx
    group by payee_id
)
select p.*, s.transactions
from payee p
left join summary s on p.id = s.payee_id)";

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
                       QObject::tr("Payees have been modified.  Please reload and try again.")}
{}

