#include "transactiondetaildao.h"
#include "sql.h"
#include <QSqlQuery>

static const auto setCategorySql = R"(update tx_detail
set tx_category_id = :categoryId, change_user = :user, change_date = current_timestamp, version = version + 1
where tx_category_id = :oldCategoryId)";

static QString staleDataMessage(QObject::tr("Transactions have been modified.  Please reload and try again."));

TransactionDetailDao::TransactionDetailDao() {}

void TransactionDetailDao::replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setCategorySql);
    query.bindValue(":user", user);
    query.bindValue(":categoryId", newCategoryId);
    query.bindValue(":oldCategoryId", category->id);
    sql::exec(query, "TransactionDetailDao", "setCategory");
    if (query.numRowsAffected() != category->details.toInt()) throw staleDataMessage;
}
