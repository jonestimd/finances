#include "transactiongroupdao.h"

static const auto getGroupsSql = R"(
with summary as (
  select td.tx_group_id, count(distinct td.tx_id) transactions, count(*) details
  from tx_detail td
  group by td.tx_group_id
)
select g.*, coalesce(s.transactions, 0) transactions, coalesce(s.details, 0) details
from tx_group g
left join summary s on g.id = s.tx_group_id)";

static const auto updateGroupSql = R"(
update tx_group
set name = :name, description = :description,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertGroupSql = R"(
insert into tx_group (name, description, version, change_user, change_date)
values (:name, :description, 0, :user, current_timestamp))";

static const auto deleteGroupSql = "delete from tx_group where id = :id";

TransactionGroupDao::TransactionGroupDao()
    : EntityDao<TransactionGroup>{getGroupsSql, updateGroupSql, insertGroupSql, deleteGroupSql,
                "TransactionGroupDao", QObject::tr("Groups have been modified.  Please reload and try again.")} {}
