#include "payeedao.h"

static const auto getAllQuery = R"(
with summary as (
    select payee_id, count(*) transactions
    from tx
    group by payee_id
)
select p.*, coalesce(s.transactions, 0) transactions
from payee p
left join summary s on p.id = s.payee_id)";

static const auto updateQuery = R"(
update payee
set name = :name, change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertQuery = R"(
insert into payee (name, version, change_user, change_date)
values (:name, 0, :user, current_timestamp))";

static const auto deleteQuery = "delete from payee where id = :id";

PayeeDao::PayeeDao()
    : NamedEntityDao<Payee>{getAllQuery, updateQuery, insertQuery, deleteQuery, "PayeeDao",
                            QObject::tr("Payees have been modified.  Please reload and try again.")}
{}

