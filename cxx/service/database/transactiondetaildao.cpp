#include "transactiondetaildao.h"
#include "dbdialect.h"
#include "sql.h"
#include <QSqlQuery>
#include <QSqlDriver>
#include <QSqlField>

#define CREATE_TABLE_QUERY(idtype) \
    "create table tx_detail (\n" \
    "    id " idtype ",\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    amount numeric(19,2) not null,\n" \
    "    asset_quantity numeric(19,6) default null,\n" \
    "    memo varchar(2000) default null,\n" \
    "    tx_category_id bigint,\n" \
    "    exchange_asset_id bigint,\n" \
    "    tx_group_id bigint,\n" \
    "    related_detail_id bigint,\n" \
    "    tx_id bigint not null,\n" \
    "    date_acquired date,\n" \
    "    constraint tx_related_key unique (related_detail_id),\n" \
    "    constraint tx_asset_fk foreign key (exchange_asset_id) references asset (id),\n" \
    "    constraint tx_detail_group_fk foreign key (tx_group_id) references tx_group (id),\n" \
    "    constraint tx_detail_transfer_fk foreign key (related_detail_id) references tx_detail (id) on delete set null,\n" \
    "    constraint tx_detail_tx_fk foreign key (tx_id) references tx (id),\n" \
    "    constraint tx_detail_tx_type_fk foreign key (tx_category_id) references tx_category (id)\n" \
    ")"

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

#define GET_RELATED_IDS_QUERY(inList) \
    "select td.id, td.related_detail_id, tx.account_id, rx.id related_tx_id, rx.account_id transfer_account_id\n" \
    "from tx_detail td\n" \
    "join tx on td.tx_id = tx.id\n" \
    "left join tx_detail rd on td.related_detail_id = rd.id\n" \
    "left join tx rx on rd.tx_id = rx.id\n" \
    "where " inList(td.id, :ids)

static const auto pgGetRelatedIdsSql = GET_RELATED_IDS_QUERY(PG_IN_LIST);
static const auto sqliteGetRelatedIdsSql = GET_RELATED_IDS_QUERY(SQLITE_IN_LIST);
static const auto mysqlGetRelatedIdsSql = GET_RELATED_IDS_QUERY(MYSQL_IN_LIST);

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

#define UPDATE_TRANSFER_AMOUNT_QUERY(inList) \
    "update tx_detail\n" \
    "set amount = (select -rd.amount from tx_detail rd where rd.related_detail_id = tx_detail.id),\n" \
    "    change_user = :user, change_date = current_timestamp, version = version + 1\n" \
    "where " inList(id, :ids)

static const auto pgUpdateTransferAmountSql = UPDATE_TRANSFER_AMOUNT_QUERY(PG_IN_LIST);
static const auto sqliteUpdateTransferAmountSql = UPDATE_TRANSFER_AMOUNT_QUERY(SQLITE_IN_LIST);
static const auto mysqlUpdateTransferAmountSql = R"(
update tx_detail td
join tx_detail rd on rd.id = td.related_detail_id
set td.amount = -rd.amount,
    td.change_user = :user, td.change_date = current_timestamp, td.version = td.version + 1
where td.id member of (:ids))";

#define DELETE_BY_IDS_QUERY(inList) "delete from tx_detail where " inList(id, :ids)

static const auto pgDeleteByIdsSql = DELETE_BY_IDS_QUERY(PG_IN_LIST);
static const auto sqliteDeleteByIdsSql = DELETE_BY_IDS_QUERY(SQLITE_IN_LIST);
static const auto mysqlDeleteByIdsSql = DELETE_BY_IDS_QUERY(MYSQL_IN_LIST);

#define DELETE_IDS_BY_TRANSACTION_QUERY(inList) \
    "select td.id, td.related_detail_id, rd.tx_id related_tx_id\n" \
    "from tx_detail td\n" \
    "left join tx_detail rd on td.related_detail_id = rd.id\n" \
    "where " inList(td.tx_id, :txIds)

static const auto pgDeleteIdsByTransactionSql = DELETE_IDS_BY_TRANSACTION_QUERY(PG_IN_LIST);
static const auto sqliteDeleteIdsByTransactionSql = DELETE_IDS_BY_TRANSACTION_QUERY(SQLITE_IN_LIST);
static const auto mysqlDeleteIdsByTransactionSql = DELETE_IDS_BY_TRANSACTION_QUERY(MYSQL_IN_LIST);

static const auto setCategorySql = R"(
update tx_detail
set tx_category_id = :categoryId, change_user = :user, change_date = current_timestamp, version = version + 1
where tx_category_id = :oldCategoryId)";

#define DAO_QUERIES(idtype) \
    .createTableSql = CREATE_TABLE_QUERY(idtype),\
    .getAllSql = getAllQuery,\
    .updateSql = updateQuery,\
    .insertSql = insertQuery,\
    .deleteSql = "delete from tx_detail where id = :id",

static const DaoQueries pgQueries{
    DAO_QUERIES(PG_ID_TYPE)
};
static const DaoQueries mysqlQueries{
    DAO_QUERIES(MYSQL_ID_TYPE)
};
static const DaoQueries sqliteQueries{
    DAO_QUERIES(SQLITE_ID_TYPE)
};

TransactionDetailDao::TransactionDetailDao(const QString &dbType)
    : EntityDao<TransactionDetail>{DB_TYPE_QUERY(dbType, Queries), "TransactionDetailDao",
                                   QObject::tr("Transaction details have been modified.  Please reload and try again."), "td.id"}
    , deleteIdsByTransactionSql{DB_TYPE_QUERY(dbType, DeleteIdsByTransactionSql)}
    , deleteByIdsSql{DB_TYPE_QUERY(dbType, DeleteByIdsSql)}
    , updateTransferAmountSql{DB_TYPE_QUERY(dbType, UpdateTransferAmountSql)}
    , getRelatedIdsSql{DB_TYPE_QUERY(dbType, GetRelatedIdsSql)}
{}

QHash<qlonglong, const TransactionDetail*> TransactionDetailDao::getAll(const QSqlDatabase &db, qlonglong accountId) {
    QSqlQuery query(db);
    query.prepare(getByAccountQuery);
    sql::bindValue(query, ":accountId", accountId);
    sql::exec(query, className, "getByAccount");
    return load(query);
}

const TransactionDetail *TransactionDetailDao::addRelatedDetail(QSqlDatabase &db, qlonglong txId, const TransactionDetail *detail, const QString &user) {
    QSqlQuery query(db);
    query.prepare(insertQuery);
    TransactionDetail relatedDetail;
    detail->initTransfer(txId, relatedDetail);
    sql::bindValue(query, ":user", user);
    bindInsertValues(query, &relatedDetail);
    sql::exec(query, className, "insert");
    auto id = query.lastInsertId().toLongLong();
    return get(db, QList<qlonglong>{id}).value(id);
}

QList<qlonglong> TransactionDetailDao::removeByTransaction(QSqlDatabase &db, const QList<const Transaction*> transactions, QList<qlonglong>& relatedTransactionIds) {
    QSqlQuery query(db);
    query.prepare(deleteIdsByTransactionSql);
    sql::bindList(query, ":txIds", getEntityIds(transactions));
    sql::exec(query, className, "deleteIdsByTransaction");
    QList<qlonglong> ids{}, relatedDetailIds{};
    while (query.next()) {
        auto record = query.record();
        ids.append(sql::getValue(record, "id").toLongLong());
        auto relatedId = sql::getInt(record, "related_detail_id");
        if (relatedId.has_value()) {
            relatedDetailIds.append(relatedId.value());
            relatedTransactionIds.append(sql::getValue(record, "related_tx_id").toLongLong());
        }
    }
    removeByIds(db, ids + relatedDetailIds);
    return relatedDetailIds;
}

void TransactionDetailDao::replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setCategorySql);
    sql::bindValue(query, ":user", user);
    sql::bindValue(query, ":categoryId", newCategoryId);
    sql::bindValue(query, ":oldCategoryId", category->id);
    sql::exec(query, className, "setCategory");
    if (query.numRowsAffected() != category->details.toInt()) throw staleDataMessage;
}

QList<const TransactionDetail *> TransactionDetailDao::update(QSqlDatabase &db, const QList<TransactionDetail *> details, const QString &user) {
    auto updates = EntityDao<TransactionDetail>::update(db, details, user);
    QVariantList relatedIds{};
    for (auto detail : details) {
        if (!detail->relatedDetailId.isNull()) relatedIds.append(detail->relatedDetailId);
    }
    if (!relatedIds.isEmpty()) {
        QSqlQuery query(db);
        query.prepare(updateTransferAmountSql);
        sql::bindList(query, ":ids", relatedIds);
        sql::bindValue(query, ":user", user);
        sql::exec(query, className, "updateTransferAmount");
        updates.append(get(db, relatedIds).values()); // TODO assumes related details are not in entities
    }
    return updates;
}

void TransactionDetailDao::setRelatedDetailIds(QSqlDatabase &db, const QHash<TransactionDetail *, qlonglong> relatedIds) {
    QSqlQuery query(db);
    query.prepare(setRelatedDetailQuery);
    for (auto [detail, relatedId] : relatedIds.asKeyValueRange()) {
        detail->relatedDetailId = relatedId;
        sql::bindValue(query, ":id", detail->id);
        sql::bindValue(query, ":relatedId", detail->relatedDetailId);
        sql::exec(query, className, "setRelatedDetailId");
    }
}

QHash<qlonglong, RelatedDetailIds> TransactionDetailDao::getRelatedDetailIds(QSqlDatabase &db, const QList<TransactionDetail*> updates) {
    QSqlQuery query(db);
    auto ids = getEntityIds(updates);
    query.prepare(getRelatedIdsSql);
    sql::bindList(query, ":ids", ids);
    sql::exec(query, className, "getRelatedDetailIds");
    QHash<qlonglong, RelatedDetailIds> relatedIds{};
    while (query.next()) {
        auto record = query.record();
        relatedIds.insert(record.value("id").toLongLong(), RelatedDetailIds{record});
    }
    return relatedIds;
}

void TransactionDetailDao::remove(QSqlDatabase &db, const QList<const TransactionDetail*> details) {
    QSqlQuery query(db);
    auto ids = getEntityIds(details);
    for (auto detail : details) if (!detail->relatedDetailId.isNull()) ids.append(detail->relatedDetailId.toLongLong());
    removeByIds(db, ids);
}

void TransactionDetailDao::bindInsertValues(QSqlQuery &query, TransactionDetail *detail) {
    sql::bindValue(query, ":txId", detail->transactionId);
    sql::bindValue(query, ":amount", detail->amount);
    sql::bindValue(query, ":assetQuantity", detail->assetQuantity);
    sql::bindValue(query, ":memo", detail->memo);
    sql::bindValue(query, ":categoryId", detail->categoryId);
    sql::bindValue(query, ":groupId", detail->groupId);
    sql::bindValue(query, ":relatedDetailId", detail->relatedDetailId);
}

void TransactionDetailDao::bindUpdateValues(QSqlQuery &query, TransactionDetail *detail) {
    EntityDao::bindUpdateValues(query, detail);
    sql::bindValue(query, ":txId", detail->transactionId);
    sql::bindValue(query, ":amount", detail->amount);
    sql::bindValue(query, ":assetQuantity", detail->assetQuantity);
    sql::bindValue(query, ":memo", detail->memo);
    sql::bindValue(query, ":categoryId", detail->categoryId);
    sql::bindValue(query, ":groupId", detail->groupId);
    sql::bindValue(query, ":relatedDetailId", detail->relatedDetailId);
}

void TransactionDetailDao::removeByIds(QSqlDatabase &db, const QList<qlonglong> ids) {
    QSqlQuery query(db);
    query.prepare(deleteByIdsSql);
    sql::bindList(query, ":ids", ids);
    sql::exec(query, className, "deleteByIds");
}

RelatedDetailIds::RelatedDetailIds(QSqlRecord &record)
    : accountId{record.value("account_id").toLongLong()}
    , relatedDetailId{sql::getInt(record, "related_detail_id")}
    , relatedTransactionId{sql::getInt(record, "related_tx_id")}
    , transferAccountId{sql::getInt(record, "transfer_account_id")}
{}
