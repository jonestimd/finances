#include "transactiondetaildao.h"
#include "sql.h"
#include <QSqlQuery>

#define DAO_NAME "TransactionDetailDao"

static const auto getByAccountQuery = R"(
select td.*
from tx
join tx_detail td on td.tx_id = tx.id
left join tx_detail rd on rd.id = td.related_detail_id
left join tx rx on rx.id = rd.tx_id
where :account_id in (tx.account_id, rx.account_id))";

static const auto setCategorySql = R"(update tx_detail
set tx_category_id = :categoryId, change_user = :user, change_date = current_timestamp, version = version + 1
where tx_category_id = :oldCategoryId)";

TransactionDetailDao::TransactionDetailDao()
    : EntityDao<TransactionDetail>{"", "", "", "", "TransactionDetailDao", QObject::tr("Transactions have been modified.  Please reload and try again.")}
{}

QHash<qlonglong, const TransactionDetail*> TransactionDetailDao::getAll(const QSqlDatabase &db, const QVariant &accountId) {
    QSqlQuery query(db);
    query.prepare(getByAccountQuery);
    query.bindValue(":account_id", accountId);
    sql::exec(query, DAO_NAME, "getByAccount");
    return load(query);
}

void TransactionDetailDao::replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setCategorySql);
    query.bindValue(":user", user);
    query.bindValue(":categoryId", newCategoryId);
    query.bindValue(":oldCategoryId", category->id);
    sql::exec(query, DAO_NAME, "setCategory");
    if (query.numRowsAffected() != category->details.toInt()) throw staleDataMessage;
}

void TransactionDetailDao::bindInsertValues(QSqlQuery &query, TransactionDetail *entity) {
}
