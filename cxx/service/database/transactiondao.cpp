#include "transactiondao.h"
#include "mapping.h"
#include "dbdialect.h"
#include "sql.h"
#include <QSqlQuery>

static const char* const createTableQueries[]{R"(
create table tx (
    id %1,
    change_date timestamp not null default current_timestamp,
    change_user varchar(50) not null,
    version bigint not null,
    cleared character(1) not null,
    date date not null,
    memo varchar(2000) default null,
    reference_number varchar(30) default null,
    account_id bigint not null,
    payee_id bigint,
    security_id bigint,
    constraint tx_account_fk foreign key (account_id) references account (id),
    constraint tx_payee_fk foreign key (payee_id) references payee (id),
    constraint tx_security_fk foreign key (security_id) references asset (id)
))",
"create index tx_payee on tx (payee_id)",
};

static const auto getByAccountQuery = R"(
with detail_summary as (
    select tx_id, json_arrayagg(id) detail_ids
    from tx_detail
    group by tx_id
), tx_data as (
    select distinct tx.*
    from tx
    join tx_detail td on td.tx_id = tx.id
    left join tx_detail rd on rd.id = td.related_detail_id
    left join tx rx on rx.id = rd.tx_id
    where :accountId in (tx.account_id, rx.account_id)
)
select tx.*, ds.detail_ids
from tx_data tx
join detail_summary ds on ds.tx_id = tx.id)";

static const auto getAllQuery = R"(
select *
from tx)";

static const auto insertQuery = R"(
insert into tx (account_id, date, reference_number, memo, payee_id, security_id, cleared, version, change_user, change_date)
values (:accountId, :date, :referenceNo, :memo, :payeeId, :securityId, :cleared, 0, :user, current_timestamp))";

static const auto insertRelatedQuery = R"(
insert into tx (account_id, date, memo, payee_id, security_id, cleared, version, change_user, change_date)
select :accountId, rx.date, rx.memo, rx.payee_id, rx.security_id, 'N', 0, :user, current_timestamp
from tx_detail rd
join tx rx on rd.tx_id = rx.id
where rd.id = :detailId
returning *)";

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
    sql::exec(db, dbDialect::createTableSql(db, createTableQueries[0]), className, "createTable");
    sql::exec(db, createTableQueries[1], className, "createPayeeIndex");
}

QHash<qlonglong, const Transaction*> TransactionDao::getAll(const QSqlDatabase &db, qlonglong accountId) {
    QSqlQuery query(db);
    query.prepare(dbDialect::replaceJsonArrayAgg(db, getByAccountQuery));
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
    if (!query.next()) throw QString("failed to insert transaction");
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
