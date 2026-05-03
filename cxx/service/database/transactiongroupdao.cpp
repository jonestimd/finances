#include "transactiongroupdao.h"
#include "dbdialect.h"

#define CREATE_TABLE_QUERY(idtype) \
    "create table tx_group (\n" \
    "    id " idtype ",\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    description text,\n" \
    "    name varchar(50) not null,\n" \
    "    constraint tx_group_ak unique (name)\n" \
    ")"

static const auto pgCreateTableSql = CREATE_TABLE_QUERY(PG_ID_TYPE);
static const auto mysqlCreateTableSql = CREATE_TABLE_QUERY(MYSQL_ID_TYPE);
static const auto sqliteCreateTableSql = CREATE_TABLE_QUERY(SQLITE_ID_TYPE);

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
    sql::exec(db, SELECT_QUERY(db, CreateTableSql), className, "createTable");
}

void TransactionGroupDao::bindUpdateValues(QSqlQuery &query, TransactionGroup *entity) {
    NamedEntityDao::bindUpdateValues(query, entity);
    query.bindValue(":description", entity->description);
}

void TransactionGroupDao::bindInsertValues(QSqlQuery &query, TransactionGroup *entity) {
    NamedEntityDao::bindInsertValues(query, entity);
    query.bindValue(":description", entity->description);
}
