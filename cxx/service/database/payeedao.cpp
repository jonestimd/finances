#include "payeedao.h"
#include "dbdialect.h"

#define CREATE_TABLE_QUERY(idtype) \
    "create table payee (\n" \
    "    id " idtype ",\n" \
    "    name varchar(200) not null,\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    constraint payee_ak unique (name)\n" \
    ")"

static const auto pgCreateTableSql = CREATE_TABLE_QUERY(PG_ID_TYPE);
static const auto mysqlCreateTableSql = CREATE_TABLE_QUERY(MYSQL_ID_TYPE);
static const auto sqliteCreateTableSql = CREATE_TABLE_QUERY(SQLITE_ID_TYPE);

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

void PayeeDao::createTable(const QSqlDatabase &db) {
    sql::exec(db, SELECT_QUERY(db, CreateTableSql), className, "createTable");
}

