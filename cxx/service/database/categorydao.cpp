#include "categorydao.h"
#include "mapping.h"
#include "dbdialect.h"
#include "sql.h"

#define CREATE_TABLE_QUERY(idtype, uniqueNulls) \
    "create table tx_category (\n" \
    "    id " idtype ",\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    amount_type varchar(255) not null,\n" \
    "    description text,\n" \
    "    income character(1) not null,\n" \
    "    code varchar(50) not null,\n" \
    "    security character(1) not null,\n" \
    "    parent_id bigint,\n" \
    "    constraint tx_category_ak unique " uniqueNulls "(parent_id, code),\n" \
    "    constraint tx_type_parent_fk foreign key (parent_id) references tx_category (id)\n" \
    ")"

static const auto pgCreateTableQuery = CREATE_TABLE_QUERY(PG_ID_TYPE, "nulls not distinct ");
static const auto mysqlCreateTableQuery = CREATE_TABLE_QUERY(MYSQL_ID_TYPE, "");
static const auto sqliteCreateTableQuery = CREATE_TABLE_QUERY(SQLITE_ID_TYPE, "");

static const auto uniqueIndexQuery = "create unique index unique_category on tx_category (coalesce(parent_id, -1), code)";

#define GET_CATEGORIES_QUERY(jsonArrayAgg) \
    "with summary as (\n" \
    "  select td.tx_category_id, count(distinct td.tx_id) transactions, count(*) details\n" \
    "  from tx_detail td\n" \
    "  group by td.tx_category_id\n" \
    "), children as (\n" \
    "  select parent_id, " jsonArrayAgg "(id) child_ids\n" \
    "  from tx_category\n" \
    "  where parent_id is not null\n" \
    "  group by parent_id\n" \
    ")\n" \
    "select c.*, coalesce(s.transactions, 0) transactions, coalesce(s.details, 0) details, ch.child_ids\n" \
    "from tx_category c\n" \
    "left join summary s on c.id = s.tx_category_id\n" \
    "left join children ch on c.id = ch.parent_id"

static const auto getCategoriesSql = GET_CATEGORIES_QUERY(DEFAULT_JSON_ARRAY_AGG);
static const auto sqliteGetCategoriesSql = GET_CATEGORIES_QUERY(SQLITE_JSON_ARRAY_AGG);

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
where parent_id = :oldParentId)";

static const char *uniqueConstraint(const QSqlDatabase &db) {
    if (db.driverName() == "QPSQL") return "nulls not distinct (parent_id, code)";
    return "(parent_id, code)";
}

CategoryDao::CategoryDao()
    : NamedEntityDao<Category>{getCategoriesSql, updateCategorySql, insertCategorySql, deleteCategorySql, "CategoryDao",
                               QObject::tr("Categories have been modified.  Please reload and try again.")} {}

void CategoryDao::createTable(const QSqlDatabase &db) {
    sql::exec(db, SELECT_QUERY(db, CreateTableQuery), className, "createTable");
    if (IS_SQLITE(db)) sql::exec(db, uniqueIndexQuery, className, "addUniqueIndex");
}

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
    sql::exec(query, className, "setParent");
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
        sql::exec(query, className, "setParents");
        if (query.numRowsAffected() != category->childIds.length()) throw staleDataMessage;
    }
}

const char *CategoryDao::getLoadAllQuery(QSqlDatabase &db) const {
    if (IS_SQLITE(db)) return sqliteGetCategoriesSql;
    return NamedEntityDao::getLoadAllQuery(db);
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
