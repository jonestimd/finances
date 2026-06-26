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

static const auto uniqueIndexQuery = "create unique index unique_category on tx_category (coalesce(parent_id, -1), code)";

#define GET_ALL_QUERY(jsonArrayAgg) \
    "with summary as (\n" \
    "  select td.tx_category_id, count(*) details\n" \
    "  from tx_detail td\n" \
    "  group by td.tx_category_id\n" \
    "), children as (\n" \
    "  select parent_id, " jsonArrayAgg "(id) child_ids\n" \
    "  from tx_category\n" \
    "  where parent_id is not null\n" \
    "  group by parent_id\n" \
    ")\n" \
    "select c.*, coalesce(s.details, 0) details, ch.child_ids\n" \
    "from tx_category c\n" \
    "left join summary s on c.id = s.tx_category_id\n" \
    "left join children ch on c.id = ch.parent_id"

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

static const auto setParentSql = R"(
update tx_category
set parent_id = :parentId, change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto setParentsSql = R"(
update tx_category
set parent_id = :parentId, change_user = :user, change_date = current_timestamp, version = version + 1
where parent_id = :oldParentId)";

#define DAO_QUERIES(idtype, uniqueNulls, jsonArrayAgg) \
    .createTableSql = CREATE_TABLE_QUERY(idtype, uniqueNulls),\
    .getAllSql = GET_ALL_QUERY(jsonArrayAgg),\
    .updateSql = updateCategorySql,\
    .insertSql = insertCategorySql,\
    .deleteSql = deleteCategorySql,

static const DaoQueries pgQueries{
    DAO_QUERIES(PG_ID_TYPE, "nulls not distinct ", DEFAULT_JSON_ARRAY_AGG)
};
static const DaoQueries mysqlQueries{
    DAO_QUERIES(MYSQL_ID_TYPE, "", DEFAULT_JSON_ARRAY_AGG)
};
static const DaoQueries sqliteQueries{
    DAO_QUERIES(SQLITE_ID_TYPE, "", SQLITE_JSON_ARRAY_AGG)
};

CategoryDao::CategoryDao(const QString &dbType)
    : NamedEntityDao<Category>{DB_TYPE_QUERY(dbType, Queries), "CategoryDao",
                               QObject::tr("Categories have been modified.  Please reload and try again.")}
{}

void CategoryDao::createTable(const QSqlDatabase &db) const {
    NamedEntityDao::createTable(db);
    if (IS_SQLITE(db)) sql::exec(db, uniqueIndexQuery, className, "addUniqueIndex");
}

QHash<domain_id, const Category*> CategoryDao::setParent(QSqlDatabase &db, const Category *category, const optional_id &parentId, const QString user) {
    QSqlQuery query(db);
    QList<domain_id> ids{category->id.value()};
    if (category->parentId.has_value()) ids.append(category->parentId.value());
    if (parentId.has_value()) ids.append(parentId.value());
    query.prepare(setParentSql);
    sql::bindValue(query, ":user", user);
    sql::bindValue(query, ":id", category->id);
    sql::bindValue(query, ":version", category->version);
    sql::bindValue(query, ":parentId", parentId);
    sql::exec(query, className, "setParent");
    if (query.numRowsAffected() < 1) throw staleDataMessage;
    return get(db, ids);
}

void CategoryDao::moveChildren(QSqlDatabase &db, const Category *category, const domain_id destinationId, const QString user) const {
    if (!category->childIds.isEmpty()) {
        QSqlQuery query(db);
        query.prepare(setParentsSql);
        sql::bindValue(query, ":user", user);
        sql::bindValue(query, ":parentId", destinationId);
        sql::bindValue(query, ":oldParentId", category->id);
        sql::exec(query, className, "setParents");
        if (query.numRowsAffected() != category->childIds.length()) throw staleDataMessage;
    }
}

void CategoryDao::bindUpdateValues(QSqlQuery &query, Category *category) {
    NamedEntityDao::bindUpdateValues(query, category);
    sql::bindValue(query, ":parentId", category->parentId);
    sql::bindValue(query, ":description", category->description);
    sql::bindValue(query, ":amountType", category->amountType);
    sql::bindValue(query, ":income", mapping::toYesNo(category->income));
    sql::bindValue(query, ":security", mapping::toYesNo(category->security));
}

void CategoryDao::bindInsertValues(QSqlQuery &query, Category *category) {
    NamedEntityDao::bindInsertValues(query, category);
    sql::bindValue(query, ":parentId", category->parentId);
    sql::bindValue(query, ":description", category->description);
    sql::bindValue(query, ":amountType", category->amountType);
    sql::bindValue(query, ":income", mapping::toYesNo(category->income));
    sql::bindValue(query, ":security", mapping::toYesNo(category->security));
}
