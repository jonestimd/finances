#include "transactiondetaildao.h"
#include "dbdialect.h"
#include "sql.h"
#include <QSqlQuery>
#include <QSqlDriver>
#include <QSqlField>

static const auto createTableQuery = R"(
create table tx_detail (
    id %1,
    change_date timestamp not null default current_timestamp,
    change_user varchar(50) not null,
    version bigint not null,
    amount numeric(19,2) not null,
    asset_quantity numeric(19,6) default null,
    memo varchar(2000) default null,
    tx_category_id bigint,
    exchange_asset_id bigint,
    tx_group_id bigint,
    related_detail_id bigint,
    tx_id bigint not null,
    date_acquired date,
    constraint tx_related_key unique (related_detail_id),
    constraint tx_asset_fk foreign key (exchange_asset_id) references asset (id),
    constraint tx_detail_group_fk foreign key (tx_group_id) references tx_group (id),
    constraint tx_detail_transfer_fk foreign key (related_detail_id) references tx_detail (id) on delete set null,
    constraint tx_detail_tx_fk foreign key (tx_id) references tx (id),
    constraint tx_detail_tx_type_fk foreign key (tx_category_id) references tx_category (id)
))";

static const auto getAllQuery = R"(
select td.*, rx.account_id transfer_account_id
from tx_detail td
left join tx_detail rd on rd.id = td.related_detail_id
left join tx rx on rx.id = rd.tx_id)";

static const auto getByAccountQuery = R"(
select td.*, rx.account_id transfer_account_id
from tx_detail td
join tx on td.tx_id = tx.id
left join tx_detail rd on rd.id = td.related_detail_id
left join tx rx on rx.id = rd.tx_id
where :accountId in (tx.account_id, rx.account_id))";

static const auto getRelatedIdsQuery = R"(
select td.id, td.related_detail_id, tx.account_id
from tx_detail td
join tx on td.tx_id = tx.id
where %0)";

static const auto insertQuery = R"(
insert into tx_detail (tx_id, amount, asset_quantity, memo, tx_category_id, tx_group_id, related_detail_id, version, change_user, change_date)
values (:txId, :amount, :assetQuantity, :memo, :categoryId, :groupId, :relatedDetailId, 0, :user, current_timestamp))";

static const auto setRelatedDetailQuery = "update tx_detail set related_detail_id = :relatedId where id = :id";

static const auto updateQuery = R"(
update tx_detail
set amount = :amount, asset_quantity = :assetQuantity, memo = :memo, tx_category_id = :categoryId,
    tx_group_id = :groupId, related_detail_id = :relatedDetailId,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto updateTransferAmountQuery = R"(
update tx_detail
set amount = (select -rd.amount from tx_detail rd where rd.related_detail_id = tx_detail.id),
    change_user = :user, change_date = current_timestamp, version = version + 1
where %0)";

static const auto mysqlUpdateTransferAmountQuery = R"(
update tx_detail td
join tx_detail rd on rd.id = td.related_detail_id
set td.amount = -rd.amount,
    td.change_user = :user, td.change_date = current_timestamp, td.version = td.version + 1
where td.id member of (:ids))";

static const auto deleteQuery = "delete from tx_detail where id = :id";
static const auto deleteByIdsQuery = "delete from tx_detail where %0";
static const auto deleteByTransactionQuery = R"(
delete from tx_detail
where %0
   or id in (select related_detail_id from tx_detail where %0))";

static const auto mysqlDeleteByTransactionQuery = R"(
delete td, rd
from tx_detail td
left join tx_detail rd on td.related_detail_id = rd.id
where td.tx_id member of (:txIds))";

static const auto setCategorySql = R"(
update tx_detail
set tx_category_id = :categoryId, change_user = :user, change_date = current_timestamp, version = version + 1
where tx_category_id = :oldCategoryId)";

TransactionDetailDao::TransactionDetailDao()
    : EntityDao<TransactionDetail>{getAllQuery, updateQuery, insertQuery, deleteQuery, "TransactionDetailDao",
                                   QObject::tr("Transaction details have been modified.  Please reload and try again."), "td.id"}
{}

void TransactionDetailDao::createTable(const QSqlDatabase &db) {
    sql::exec(db, dbDialect::createTableSql(db, createTableQuery), className, "createTable");
}

QHash<qlonglong, const TransactionDetail*> TransactionDetailDao::getAll(const QSqlDatabase &db, const QVariant &accountId) {
    QSqlQuery query(db);
    query.prepare(getByAccountQuery);
    SQL_BIND_VALUE(query, ":accountId", accountId);
    sql::exec(query, className, "getByAccount");
    return load(query);
}

void TransactionDetailDao::removeByTransaction(QSqlDatabase &db, const QList<const Transaction*> transactions) {
    QSqlQuery query(db);
    QString sql;
    if (db.driverName() == "QMYSQL") sql = QString{mysqlDeleteByTransactionQuery};
    else sql =QString{deleteByTransactionQuery}.arg(dbDialect::inList(db, "tx_id", ":txIds"));
    query.prepare(sql);
    qCInfo(sqlLogger, "%s", sql.toLocal8Bit().constData());
    SQL_BIND_LIST(query, ":txIds", getEntityIds(transactions));
    SQL_EXEC(query, "deleteByTransaction");
}

void TransactionDetailDao::replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setCategorySql);
    SQL_BIND_VALUE(query, ":user", user);
    SQL_BIND_VALUE(query, ":categoryId", newCategoryId);
    SQL_BIND_VALUE(query, ":oldCategoryId", category->id);
    sql::exec(query, className, "setCategory");
    if (query.numRowsAffected() != category->details.toInt()) throw staleDataMessage;
}

QList<const TransactionDetail *> TransactionDetailDao::update(QSqlDatabase &db, const QList<TransactionDetail *> entities, const QString &user) {
    auto updates = EntityDao<TransactionDetail>::update(db, entities, user);
    QVariantList relatedIds{};
    for (auto detail : entities) {
        if (!detail->relatedDetailId.isNull()) relatedIds.append(detail->relatedDetailId);
    }
    if (!relatedIds.isEmpty()) {
        QSqlQuery query(db);
        if (db.driverName() == "QMYSQL") query.prepare(mysqlUpdateTransferAmountQuery);
        else query.prepare(QString{updateTransferAmountQuery}.arg(dbDialect::inList(db, "id", ":ids")));
        SQL_BIND_LIST(query, ":ids", relatedIds);
        SQL_BIND_VALUE(query, ":user", user);
        sql::exec(query, className, "updateTransferAmount");
        updates.append(get(db, relatedIds).values()); // TODO assumes related details are not in entities
    }
    return updates;
}

void TransactionDetailDao::setRelatedDetailIds(QSqlDatabase &db, const QHash<TransactionDetail*, TransactionDetail*> transfers) {
    QSqlQuery query(db);
    query.prepare(setRelatedDetailQuery);
    for (auto [detail, relatedDetail] : transfers.asKeyValueRange()) {
        detail->relatedDetailId = relatedDetail->id;
        SQL_BIND_VALUE(query, ":id", detail->id);
        SQL_BIND_VALUE(query, ":relatedId", detail->relatedDetailId);
        sql::exec(query, className, "setRelatedDetailId");
    }
}

QHash<qlonglong, RelatedDetailIds> TransactionDetailDao::getRelatedDetailIds(QSqlDatabase &db, const QList<TransactionDetail*> updates) {
    QSqlQuery query(db);
    auto ids = getEntityIds(updates);
    query.prepare(QString{getRelatedIdsQuery}.arg(dbDialect::inList(db, "td.id", ":ids")));
    SQL_BIND_LIST(query, ":ids", ids);
    sql::exec(query, className, "getRelatedDetailIds");
    QHash<qlonglong, RelatedDetailIds> relatedIds{};
    while (query.next()) {
        auto record = query.record();
        relatedIds.insert(record.value("id").toLongLong(), {record.value("account_id"), sql::getValue(record, "related_detail_id")});
    }
    return relatedIds;
}

void TransactionDetailDao::remove(QSqlDatabase &db, const QList<const TransactionDetail*> details) {
    QSqlQuery query(db);
    auto ids = getEntityIds(details);
    for (auto detail : details) if (!detail->relatedDetailId.isNull()) ids.append(detail->relatedDetailId);
    query.prepare(QString{deleteByIdsQuery}.arg(dbDialect::inList(db, "id", ":ids")));
    SQL_BIND_LIST(query, ":ids", ids);
    sql::exec(query, className, "deleteByIds");
}

void TransactionDetailDao::bindInsertValues(QSqlQuery &query, TransactionDetail *detail) {
    SQL_BIND_VALUE(query, ":txId", detail->transactionId);
    SQL_BIND_VALUE(query, ":amount", detail->amount);
    SQL_BIND_VALUE(query, ":assetQuantity", detail->assetQuantity);
    SQL_BIND_VALUE(query, ":memo", detail->memo);
    SQL_BIND_VALUE(query, ":categoryId", detail->categoryId);
    SQL_BIND_VALUE(query, ":groupId", detail->groupId);
    SQL_BIND_VALUE(query, ":relatedDetailId", detail->relatedDetailId);
}

void TransactionDetailDao::bindUpdateValues(QSqlQuery &query, TransactionDetail *detail) {
    EntityDao::bindUpdateValues(query, detail);
    SQL_BIND_VALUE(query, ":txId", detail->transactionId);
    SQL_BIND_VALUE(query, ":amount", detail->amount);
    SQL_BIND_VALUE(query, ":assetQuantity", detail->assetQuantity);
    SQL_BIND_VALUE(query, ":memo", detail->memo);
    SQL_BIND_VALUE(query, ":categoryId", detail->categoryId);
    SQL_BIND_VALUE(query, ":groupId", detail->groupId);
    SQL_BIND_VALUE(query, ":relatedDetailId", detail->relatedDetailId);
}
