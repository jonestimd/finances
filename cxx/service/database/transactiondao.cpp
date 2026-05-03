#include "transactiondao.h"
#include "mapping.h"
#include "dbdialect.h"
#include "sql.h"
#include <QSqlQuery>

#define CREATE_TABLE_QUERY(idtype) \
    "create table tx (\n" \
    "    id " idtype ",\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    cleared character(1) not null,\n" \
    "    date date not null,\n" \
    "    memo varchar(2000) default null,\n" \
    "    reference_number varchar(30) default null,\n" \
    "    account_id bigint not null,\n" \
    "    payee_id bigint,\n" \
    "    security_id bigint,\n" \
    "    constraint tx_account_fk foreign key (account_id) references account (id),\n" \
    "    constraint tx_payee_fk foreign key (payee_id) references payee (id),\n" \
    "    constraint tx_security_fk foreign key (security_id) references asset (id)\n" \
    ")"

static const auto pgCreateTableSql = CREATE_TABLE_QUERY(PG_ID_TYPE);
static const auto mysqlCreateTableSql = CREATE_TABLE_QUERY(MYSQL_ID_TYPE);
static const auto sqliteCreateTableSql = CREATE_TABLE_QUERY(SQLITE_ID_TYPE);

static const auto createPayeeIndexSql = "create index tx_payee on tx (payee_id)";

#define GET_BY_ACCOUNT_QUERY(jsonArrayAgg) \
    "with detail_summary as (\n" \
    "    select tx_id, " jsonArrayAgg "(id) detail_ids\n" \
    "    from tx_detail\n" \
    "    group by tx_id\n" \
    "), tx_data as (\n" \
    "    select distinct tx.*\n" \
    "    from tx\n" \
    "    join tx_detail td on td.tx_id = tx.id\n" \
    "    left join tx_detail rd on rd.id = td.related_detail_id\n" \
    "    left join tx rx on rx.id = rd.tx_id\n" \
    "    where :accountId in (tx.account_id, rx.account_id)\n" \
    ")\n" \
    "select tx.*, ds.detail_ids\n" \
    "from tx_data tx\n" \
    "join detail_summary ds on ds.tx_id = tx.id"

static const auto pgGetByAccountSql = GET_BY_ACCOUNT_QUERY(DEFAULT_JSON_ARRAY_AGG);
static const auto mysqlGetByAccountSql = pgGetByAccountSql;
static const auto sqliteGetByAccountSql = GET_BY_ACCOUNT_QUERY(SQLITE_JSON_ARRAY_AGG);

static const auto getAllQuery = "select * from tx";

static const auto getOneQuery = "select * from tx where id = :id";

static const auto insertQuery = R"(
insert into tx (account_id, date, reference_number, memo, payee_id, security_id, cleared, version, change_user, change_date)
values (:accountId, :date, :referenceNo, :memo, :payeeId, :securityId, :cleared, 0, :user, current_timestamp))";

static const auto insertRelatedQuery = R"(
insert into tx (account_id, date, memo, payee_id, security_id, cleared, version, change_user, change_date)
select :accountId, rx.date, rx.memo, rx.payee_id, rx.security_id, 'N', 0, :user, current_timestamp
from tx_detail rd
join tx rx on rd.tx_id = rx.id
where rd.id = :detailId)";

static const auto updateQuery = R"(
update tx
set account_id = :accountId, date = :date, reference_number = :referenceNo, memo = :memo,
    payee_id = :payeeId, security_id = :securityId, cleared = :cleared,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto deleteQuery = "delete from tx where id = :id";

static const auto deleteEmptyQuery = R"(
delete from tx
where not exists (select 1 from tx_detail where tx_id = tx.id))";

static const auto setPayeeQuery = R"(
update tx
set payee_id = :payeeId, change_user = :user, change_date = current_timestamp, version = version + 1
where payee_id = :oldPayeeId)";

TransactionDao::TransactionDao()
    : EntityDao<Transaction>{getAllQuery, updateQuery, insertQuery, deleteQuery, "TransactionDao",
                             QObject::tr("Transactions have been modified.  Please reload and try again.")}
{}

void TransactionDao::createTable(const QSqlDatabase &db) {
    sql::exec(db, SELECT_QUERY(db, CreateTableSql), className, "createTable");
    sql::exec(db, createPayeeIndexSql, className, "createPayeeIndex");
}

QHash<qlonglong, const Transaction*> TransactionDao::getAll(const QSqlDatabase &db, qlonglong accountId) {
    QSqlQuery query(db);
    query.prepare(SELECT_QUERY(db, GetByAccountSql));
    query.bindValue(":accountId", accountId);
    sql::exec(query, className, "getByAccount");
    return load(query);
}

void TransactionDao::replacePayee(const QSqlDatabase &db, const Payee *payee, const QVariant newPayeeId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setPayeeQuery);
    qCInfo(sqlLogger, setPayeeQuery);
    SQL_BIND_VALUE(query, ":user", user);
    SQL_BIND_VALUE(query, ":payeeId", newPayeeId);
    SQL_BIND_VALUE(query, ":oldPayeeId", payee->id);
    sql::exec(query, className, "setCategory");
    if (query.numRowsAffected() != payee->transactions.toInt()) throw staleDataMessage;
}

void TransactionDao::removeEmpty(const QSqlDatabase &db) {
    QSqlQuery query(db);
    query.prepare(deleteEmptyQuery);
    sql::exec(query, className, "deleteEmpty");
}

Transaction *TransactionDao::addRelatedTransaction(QSqlDatabase &db, TransactionDetail *detail, const QString &user) {
    QSqlQuery query(db);
    query.prepare(insertRelatedQuery);
    SQL_BIND_VALUE(query, ":user", user);
    SQL_BIND_VALUE(query, ":detailId", detail->id);
    SQL_BIND_VALUE(query, ":accountId", detail->transferAccountId);
    sql::exec(query, className, "addRelatedTransaction");
    if (query.numRowsAffected() != 1) throw QString("failed to insert transaction");
    auto relatedId = query.lastInsertId();
    query.prepare(getOneQuery);
    SQL_BIND_VALUE(query, ":id", relatedId);
    sql::exec(query, className, "getRelatedTransaction");
    if (!query.next()) throw QString("failed to load new transaction");
    return new Transaction(query.record());
}

void TransactionDao::bindInsertValues(QSqlQuery &query, Transaction *transaction) {
    SQL_BIND_VALUE(query, ":accountId", transaction->accountId);
    SQL_BIND_VALUE(query, ":date", transaction->date); // TODO conversion?
    SQL_BIND_VALUE(query, ":referenceNo", transaction->referenceNumber);
    SQL_BIND_VALUE(query, ":memo", transaction->memo);
    SQL_BIND_VALUE(query, ":payeeId", transaction->payeeId);
    SQL_BIND_VALUE(query, ":securityId", transaction->securityId);
    SQL_BIND_VALUE(query, ":cleared", mapping::toYesNo(transaction->cleared));
}

void TransactionDao::bindUpdateValues(QSqlQuery &query, Transaction *transaction) {
    EntityDao::bindUpdateValues(query, transaction);
    SQL_BIND_VALUE(query, ":accountId", transaction->accountId);
    SQL_BIND_VALUE(query, ":date", transaction->date); // TODO conversion?
    SQL_BIND_VALUE(query, ":referenceNo", transaction->referenceNumber);
    SQL_BIND_VALUE(query, ":memo", transaction->memo);
    SQL_BIND_VALUE(query, ":payeeId", transaction->payeeId);
    SQL_BIND_VALUE(query, ":securityId", transaction->securityId);
    SQL_BIND_VALUE(query, ":cleared", mapping::toYesNo(transaction->cleared));
}
