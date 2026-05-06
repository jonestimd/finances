#include "stocksplitdao.h"

#define CREATE_TABLE_QUERY(idtype) \
    "create table stock_split(\n" \
    "    id " idtype ",\n" \
    "    change_date timestamp not null,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    date date not null,\n" \
    "    shares_in numeric(19,6) not null,\n" \
    "    shares_out numeric(19,6) not null,\n" \
    "    security_id bigint not null,\n" \
    "    constraint stock_split_ak unique (date, security_id),\n" \
    "    constraint stock_split_security_fk foreign key (security_id) references asset (id)\n" \
    ")"

static const auto insertQuery = R"(
insert into stock_split (security_id, date, shares_in, shares_out, version, change_user, change_date)
values (:securityId, :date, :sharesIn, :sharesOut, 0, :user, current_timestamp))";

static const auto updateQuery = R"(
update stock_split
set date = :date, shares_in = :sharesIn, shares_out = :sharesOut,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

#define DAO_QUERIES(idtype) \
    .createTableSql = CREATE_TABLE_QUERY(idtype),\
    .getAllSql = "select * from stock_split",\
    .updateSql = updateQuery,\
    .insertSql = insertQuery,\
    .deleteSql = "delete from stock_split where id = :id",

const DaoQueries pgQueries{
    DAO_QUERIES(PG_ID_TYPE)
};
const DaoQueries mysqlQueries{
    DAO_QUERIES(MYSQL_ID_TYPE)
};
const DaoQueries sqliteQueries{
    DAO_QUERIES(SQLITE_ID_TYPE)
};

StockSplitDao::StockSplitDao(const QString &dbType)
    : EntityDao<StockSplit>{DB_TYPE_QUERY(dbType, Queries), "StockSplitDao",
                            QObject::tr("Stock splits have been modified.  Please reload and try again")}
{}

void StockSplitDao::bindInsertValues(QSqlQuery &query, StockSplit *split) {
    SQL_BIND_VALUE(query, ":securityId", split->securityId);
    SQL_BIND_VALUE(query, ":date", split->date);
    SQL_BIND_VALUE(query, ":sharesIn", split->sharesIn);
    SQL_BIND_VALUE(query, ":sharesOut", split->sharesOut);
}

void StockSplitDao::bindUpdateValues(QSqlQuery &query, StockSplit *split) {
    EntityDao::bindUpdateValues(query, split);
    SQL_BIND_VALUE(query, ":date", split->date);
    SQL_BIND_VALUE(query, ":sharesIn", split->sharesIn);
    SQL_BIND_VALUE(query, ":sharesOut", split->sharesOut);
}
