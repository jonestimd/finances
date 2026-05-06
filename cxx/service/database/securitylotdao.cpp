#include "securitylotdao.h"

#define CREATE_TABLE_QUERY(idtype) \
    "create table security_lot (\n" \
    "    id " idtype ",\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    purchase_shares numeric(19,6) not null,\n" \
    "    adjusted_shares numeric(19,6) not null,\n" \
    "    purchase_tx_detail_id bigint,\n" \
    "    related_tx_detail_id bigint,\n" \
    "    constraint security_lot_purchase_tx_fk foreign key (purchase_tx_detail_id) references tx_detail (id),\n" \
    "    constraint security_lot_sale_tx_fk foreign key (related_tx_detail_id) references tx_detail (id)\n" \
    ")"

static const auto insertQuery = R"(
insert into security_lot (purchase_tx_detail_id, related_detail_id, purchase_shares, adjusted_shares, version, change_user, change_date)
values (:purchaseDetailId, :relatedDetailId, :purchaseShares, :adjustedShares, 0, :user, current_timestamp))";

static const auto updateQuery = R"(
update security_lot
set purchase_tx_detail_id = :purchaseDetailId, related_detail_id = :relatedDetailId,
    purchase_shares = :purchaseShares, adjusted_shares = :adjustedShares,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

#define DAO_QUERIES(idtype) \
    .createTableSql = CREATE_TABLE_QUERY(idtype),\
    .getAllSql = "select * from security_lot",\
    .updateSql = updateQuery,\
    .insertSql = insertQuery,\
    .deleteSql = "delete from security_lot where id = :id",

const DaoQueries pgQueries{
    DAO_QUERIES(PG_ID_TYPE)
};
const DaoQueries mysqlQueries{
    DAO_QUERIES(MYSQL_ID_TYPE)
};
const DaoQueries sqliteQueries{
    DAO_QUERIES(SQLITE_ID_TYPE)
};

SecurityLotDao::SecurityLotDao(const QString &dbType)
    : EntityDao<SecurityLot>{DB_TYPE_QUERY(dbType, Queries), "SecurityLotDao",
                             QObject::tr("Security lots have been modified.  Please reload and try again")}
{}

void SecurityLotDao::bindInsertValues(QSqlQuery &query, SecurityLot *lot) {
    SQL_BIND_VALUE(query, ":purchaseDetailId", lot->purchaseDetailId);
    SQL_BIND_VALUE(query, ":relatedDetailId", lot->relatedDetailId);
    SQL_BIND_VALUE(query, ":purchaseShares", lot->purchaseShares);
    SQL_BIND_VALUE(query, ":adjustedShares", lot->adjustedShares);
}

void SecurityLotDao::bindUpdateValues(QSqlQuery &query, SecurityLot *lot) {
    EntityDao::bindUpdateValues(query, lot);
    SQL_BIND_VALUE(query, ":purchaseDetailId", lot->purchaseDetailId);
    SQL_BIND_VALUE(query, ":relatedDetailId", lot->relatedDetailId);
    SQL_BIND_VALUE(query, ":purchaseShares", lot->purchaseShares);
    SQL_BIND_VALUE(query, ":adjustedShares", lot->adjustedShares);
}
