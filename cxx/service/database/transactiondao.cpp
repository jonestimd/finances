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

static const auto createPayeeIndexSql = "create index tx_payee on tx (payee_id)";

#define GET_ALL_QUERY(jsonArrayAgg) \
    "with detail_summary as (\n" \
    "    select tx_id, " jsonArrayAgg "(id) detail_ids\n" \
    "    from tx_detail\n" \
    "    group by tx_id\n" \
    "), tx_data as (\n" \
    "    select distinct tx.*, rx.account_id related_account_id\n" \
    "    from tx\n" \
    "    join tx_detail td on td.tx_id = tx.id\n" \
    "    left join tx_detail rd on rd.id = td.related_detail_id\n" \
    "    left join tx rx on rx.id = rd.tx_id\n" \
    ")\n" \
    "select tx.*, ds.detail_ids\n" \
    "from tx_data tx\n" \
    "join detail_summary ds on ds.tx_id = tx.id"

#define GET_BY_ACCOUNT_QUERY(jsonArrayAgg) GET_ALL_QUERY(jsonArrayAgg) \
    "\n    where :accountId in (tx.account_id, tx.related_account_id)" \

static const auto pgGetByAccountSql = GET_BY_ACCOUNT_QUERY(DEFAULT_JSON_ARRAY_AGG);
static const auto mysqlGetByAccountSql = pgGetByAccountSql;
static const auto sqliteGetByAccountSql = GET_BY_ACCOUNT_QUERY(SQLITE_JSON_ARRAY_AGG);

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

static const auto findEmptyQuery = R"(
select id
from tx
where not exists (select 1 from tx_detail where tx_id = tx.id))";

static const auto setAccountQuery = R"(
update tx
set account_id = :newAccountId, change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and account_id = :oldAccountId)";

static const auto setPayeeQuery = R"(
update tx
set payee_id = :payeeId, change_user = :user, change_date = current_timestamp, version = version + 1
where payee_id = :oldPayeeId)";

#define DAO_QUERIES(idtype, jsonArrayAgg) \
    .createTableSql = CREATE_TABLE_QUERY(idtype),\
    .getAllSql = GET_ALL_QUERY(jsonArrayAgg),\
    .updateSql = updateQuery,\
    .insertSql = insertQuery,\
    .deleteSql =  "delete from tx where id = :id",

static const DaoQueries pgQueries{
    DAO_QUERIES(PG_ID_TYPE, DEFAULT_JSON_ARRAY_AGG)
};
static const DaoQueries mysqlQueries{
    DAO_QUERIES(MYSQL_ID_TYPE, DEFAULT_JSON_ARRAY_AGG)
};
static const DaoQueries sqliteQueries{
    DAO_QUERIES(SQLITE_ID_TYPE, SQLITE_JSON_ARRAY_AGG)
};

TransactionDao::TransactionDao(const QString &dbType)
    : EntityDao<Transaction>{DB_TYPE_QUERY(dbType, Queries), "TransactionDao",
                             QObject::tr("Transactions have been modified.  Please reload and try again.")}
    , getByAccountSql{DB_TYPE_QUERY(dbType, GetByAccountSql)}
{}

void TransactionDao::createTable(const QSqlDatabase &db) const {
    EntityDao::createTable(db);
    sql::exec(db, createPayeeIndexSql, className, "createPayeeIndex");
}

QHash<qlonglong, const Transaction*> TransactionDao::getAll(const QSqlDatabase &db, qlonglong accountId) {
    QSqlQuery query(db);
    query.prepare(getByAccountSql);
    query.bindValue(":accountId", accountId);
    sql::exec(query, className, "getByAccount");
    return load(query);
}

const QList<PendingTransaction*> TransactionDao::add(QSqlDatabase &db, const QList<PendingTransaction*> adds, const QString &user) {
    QList<Transaction*> txAdds{};
    for (auto tx : adds) txAdds.append(tx);
    EntityDao::add(db, txAdds, user);
    for (auto tx : adds) {
        for (auto detail : std::as_const(tx->details)) detail->transactionId = tx->id.value();
    }
    return adds;
}

void TransactionDao::setAccountId(const QSqlDatabase &db, const QVariant &transactionId, const QVariant &oldAccountId, const QVariant &newAccountId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setAccountQuery);
    SQL_BIND_VALUE(query, ":user", user);
    SQL_BIND_VALUE(query, ":id", transactionId);
    SQL_BIND_VALUE(query, ":oldAccountId", oldAccountId);
    SQL_BIND_VALUE(query, ":newAccountId", newAccountId);
    sql::exec(query, className, "setCategory");
    if (query.numRowsAffected() != 1) throw staleDataMessage;
}

void TransactionDao::replacePayee(const QSqlDatabase &db, const Payee *payee, const QVariant newPayeeId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setPayeeQuery);
    SQL_BIND_VALUE(query, ":user", user);
    SQL_BIND_VALUE(query, ":payeeId", newPayeeId);
    SQL_BIND_VALUE(query, ":oldPayeeId", payee->id.value());
    sql::exec(query, className, "setPayee");
    if (query.numRowsAffected() != payee->transactions.toInt()) throw staleDataMessage;
}

QList<QVariant> TransactionDao::removeEmpty(QSqlDatabase &db) {
    QSqlQuery query(db);
    query.prepare(findEmptyQuery);
    sql::exec(query, className, "findEmpty");
    QList<QVariant> ids = sql::loadValues(query, "id");
    for (const auto& id : std::as_const(ids)) remove(db, id);
    return ids;
}

Transaction *TransactionDao::addRelatedTransaction(QSqlDatabase &db, TransactionDetail *detail, const QString &user) {
    QSqlQuery query(db);
    query.prepare(insertRelatedQuery);
    SQL_BIND_VALUE(query, ":user", user);
    SQL_BIND_VALUE(query, ":detailId", detail->id.value());
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
