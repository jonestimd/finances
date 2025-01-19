#include "securitydao.h"

static const auto getAllQuery = R"(
with summary as (
    select security_id
         , sum(use_count) transactions
         , sum(shares) shares
         , min(first_acquired) first_acquired
         , sum(cost_basis) cost_basis
         , sum(dividends) dividends
    from account_security
    group by security_id
)
select a.*, s.type security_type, coalesce(sum.transactions, 0) transactions
     , coalesce(sum.shares, 0) shares, sum.first_acquired
     , coalesce(sum.cost_basis) cost_basis
     , coalesce(sum.dividends) dividends
from asset a
join security s on a.id = s.asset_id
left join summary sum on a.id = sum.security_id)";

static const auto updateQuery = R"(
update asset a
join security s on a.id = s.asset_id
set a.name = :name, a.symbol = :symbol, s.type = :securityType,
    a.change_user = :user, a.change_date = current_timestamp, a.version = version + 1
where a.id = :id and a.version = :version)";

static const auto insertQuery = R"(
insert into asset (type, name, symbol, scale, version, change_user, change_date)
values (:type, :name, :symbol, :scale, 0, :user, current_timestamp))";

static const auto insertSecurityQuery = "insert into security (asset_id, type) values (:id, :type)";

static const auto deleteQuery = "delete from security where asset_id = :id";

SecurityDao::SecurityDao()
    : NamedEntityDao<Security>{getAllQuery, updateQuery , insertQuery, deleteQuery, "SecurityDao",
                               QObject::tr("Securities have been modified.  Please reload and try again")} {}

QList<const Security *> SecurityDao::add(QSqlDatabase &db, QList<Security*> securities, const QString &user) {
    auto result = EntityDao::add(db, securities, user);
    QSqlQuery query(db);
    query.prepare(insertSecurityQuery);
    for (auto security : result) {
        query.bindValue(":id", security->id);
        query.bindValue(":type", security->securityType);
        exec(query, "insert security");
    }
    return result;
}

void SecurityDao::remove(QSqlDatabase &db, QList<const Security*> securities) {
    EntityDao::remove(db, securities);
    QSqlQuery query(db);
    query.prepare("delete from asset where id = :id");
    for (auto security : securities) {
        query.bindValue(":id", security->id);
        exec(query, "remove asset");
    }
}

void SecurityDao::bindUpdateValues(QSqlQuery &query, Security *security) {
    NamedEntityDao::bindUpdateValues(query, security);
    query.bindValue(":symbol", security->symbol);
    query.bindValue(":securityType", security->securityType);
}

 void SecurityDao::bindInsertValues(QSqlQuery &query, Security *security) {
    NamedEntityDao::bindInsertValues(query, security);
    query.bindValue(":type", security->type);
    query.bindValue(":symbol", security->symbol);
    query.bindValue(":scale", security->scale);
}
