#include "transactiongroup.h"
#include "service/database/sql.h"
#include <QSqlField>

TransactionGroup::TransactionGroup() : NamedEntity() {}

TransactionGroup::TransactionGroup(const QSqlRecord &record)
    : NamedEntity(record)
    , description{sql::getValue(record, "description")}
    , details{sql::getValue(record, "details")}
{}

TransactionGroup::TransactionGroup(const QString &name) : NamedEntity{name} {}

bool TransactionGroup::deletable() const {
    return details.toInt() == 0;
}
