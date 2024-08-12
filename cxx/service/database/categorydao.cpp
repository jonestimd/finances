#include "categorydao.h"
#include "mapping.h"

#include <QtSql>

static const auto getCategoriesSql = R"(
with summary as (
  select td.tx_category_id, count(distinct td.tx_id) transactions
  from tx_detail td
  group by td.tx_category_id
), children as (
  select parent_id id, json_arrayagg(id) child_ids
  from tx_category
  where parent_id is not null
  group by parent_id
)
select c.*, coalesce(s.transactions, 0) transactions, ch.child_ids
from tx_category c
left join summary s on c.id = s.tx_category_id
left join children ch on c.id = ch.id
order by c.code)";

static const auto updateCategorySql = R"(
update tx_category
set code = :name, parent_id = :parentId, description = :description,
    amount_type = :amountType, income = :income, security = :security,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertCategorySql = R"(
insert into tx_category (code, parent_id, description, amount_type, income, security, version, change_user, change_date)
values (:name, :parentId, :description, :amountType, :income, :security, 0, :user, current_timestamp))";

static const auto deleteCategorySql = "delete from category where id = :id";

CategoryDao::CategoryDao()
    : EntityDao<Category>{getCategoriesSql, updateCategorySql, insertCategorySql, deleteCategorySql, "CategoryDao",
                         tr("Categories have been modified.  Please reload and try again.")} {}

QList<const Category *> CategoryDao::getAll(QSqlDatabase &db) {
    auto result = EntityDao::getAll(db);
    Category::categories.clear();
    for (auto category : result) {
        Category::categories.insert(category->id.toLongLong(), category);
    }
    return result;
}

void CategoryDao::bindUpdateValues(QSqlQuery &query, Category *category) {
    EntityDao::bindUpdateValues(query, category);
    query.bindValue(":parentId", category->parentId);
    query.bindValue(":description", category->description);
    query.bindValue(":amountType", category->amountType);
    query.bindValue(":income", mapping::toYesNo(category->income));
    query.bindValue(":security", mapping::toYesNo(category->security));
}

void CategoryDao::bindInsertValues(QSqlQuery &query, Category *category) {
    EntityDao::bindInsertValues(query, category);
    query.bindValue(":parentId", category->parentId);
    query.bindValue(":description", category->description);
    query.bindValue(":amountType", category->amountType);
    query.bindValue(":income", mapping::toYesNo(category->income));
    query.bindValue(":security", mapping::toYesNo(category->security));
}
