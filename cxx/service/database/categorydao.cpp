#include "categorydao.h"
#include "mapping.h"
#include "sql.h"

#include <QtSql>

static const auto getCategoriesSql = R"(
with summary as (
  select td.tx_category_id, count(distinct td.tx_id) transactions, count(*) details
  from tx_detail td
  group by td.tx_category_id
), children as (
  select parent_id, json_arrayagg(id) child_ids
  from tx_category
  where parent_id is not null
  group by parent_id
)
select c.*, coalesce(s.transactions, 0) transactions, coalesce(s.details, 0) details, ch.child_ids
from tx_category c
left join summary s on c.id = s.tx_category_id
left join children ch on c.id = ch.parent_id)";

static const auto updateCategorySql = R"(
update tx_category
set code = :name, parent_id = :parentId, description = :description,
    amount_type = :amountType, income = :income, security = :security,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertCategorySql = R"(
insert into tx_category (code, parent_id, description, amount_type, income, security, version, change_user, change_date)
values (:name, :parentId, :description, :amountType, :income, :security, 0, :user, current_timestamp))";

static const auto deleteCategorySql = "delete from tx_category where id = :id";

static const auto setParentSql = R"(update tx_category
set parent_id = :parentId, change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto setParentsSql = R"(update tx_category
set parent_id = :parentId, change_user = :user, change_date = current_timestamp, version = version + 1
where parent_id = :oldParentId and id member of (:ids))";

CategoryDao::CategoryDao()
    : NamedEntityDao<Category>{getCategoriesSql, updateCategorySql, insertCategorySql, deleteCategorySql, "CategoryDao",
                               QObject::tr("Categories have been modified.  Please reload and try again.")} {}

QHash<qlonglong, const Category*> CategoryDao::setParent(QSqlDatabase &db, const Category *category, const QVariant parentId, const QString user) {
    QSqlQuery query(db);
    QVariantList ids{category->id};
    if (!category->parentId.isNull()) ids.append(category->parentId.toLongLong());
    if (!parentId.isNull()) ids.append(parentId);
    query.prepare(setParentSql);
    query.bindValue(":user", user);
    query.bindValue(":id", category->id);
    query.bindValue(":version", category->version);
    query.bindValue(":parentId", parentId);
    exec(query, "setParent");
    if (query.numRowsAffected() < 1) throw staleDataMessage;
    return get(db, ids);
}

void CategoryDao::moveChildren(QSqlDatabase &db, const Category *category, const QVariant destinationId, const QString user) {
    if (!category->childIds.isEmpty()) {
        QSqlQuery query(db);
        query.prepare(setParentsSql);
        query.bindValue(":user", user);
        query.bindValue(":parentId", destinationId);
        query.bindValue(":oldParentId", category->id);
        sql::bindList(query, category->childIds, ":ids");
        exec(query, "setParents");
        if (query.numRowsAffected() != category->childIds.length()) throw staleDataMessage;
    }
}

void CategoryDao::bindUpdateValues(QSqlQuery &query, Category *category) {
    NamedEntityDao::bindUpdateValues(query, category);
    query.bindValue(":parentId", category->parentId);
    query.bindValue(":description", category->description);
    query.bindValue(":amountType", category->amountType);
    query.bindValue(":income", mapping::toYesNo(category->income));
    query.bindValue(":security", mapping::toYesNo(category->security));
}

void CategoryDao::bindInsertValues(QSqlQuery &query, Category *category) {
    NamedEntityDao::bindInsertValues(query, category);
    query.bindValue(":parentId", category->parentId);
    query.bindValue(":description", category->description);
    query.bindValue(":amountType", category->amountType);
    query.bindValue(":income", mapping::toYesNo(category->income));
    query.bindValue(":security", mapping::toYesNo(category->security));
}
