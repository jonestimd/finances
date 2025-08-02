#include "transactiongroupdao.h"
#include "dbdialect.h"

static const auto createTableQuery = R"(
create table tx_group (
    id %1,
    change_date timestamp not null default current_timestamp,
    change_user varchar(50) not null,
    version bigint not null,
    description text,
    name varchar(50) not null,
    constraint tx_group_ak unique (name)
))";

static const auto getAllQuery = R"(
with summary as (
  select td.tx_group_id, count(distinct td.tx_id) transactions, count(*) details
  from tx_detail td
  group by td.tx_group_id
)
select g.*, coalesce(s.transactions, 0) transactions, coalesce(s.details, 0) details
from tx_group g
left join summary s on g.id = s.tx_group_id)";

static const auto updateQuery = R"(
update tx_group
set name = :name, description = :description,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertQuery = R"(
insert into tx_group (name, description, version, change_user, change_date)
values (:name, :description, 0, :user, current_timestamp))";

static const auto deleteQuery = "delete from tx_group where id = :id";

TransactionGroupDao::TransactionGroupDao()
    : NamedEntityDao<TransactionGroup>{getAllQuery, updateQuery, insertQuery, deleteQuery, "TransactionGroupDao",
                                       QObject::tr("Groups have been modified.  Please reload and try again.")} {}

void TransactionGroupDao::createTable(const QSqlDatabase &db) {
    sql::exec(db, dbDialect::createTableSql(db, createTableQuery), className, "createTable");
}

void TransactionGroupDao::bindUpdateValues(QSqlQuery &query, TransactionGroup *entity) {
    NamedEntityDao::bindUpdateValues(query, entity);
    query.bindValue(":description", entity->description);
}

void TransactionGroupDao::bindInsertValues(QSqlQuery &query, TransactionGroup *entity) {
    NamedEntityDao::bindInsertValues(query, entity);
    query.bindValue(":description", entity->description);
}
