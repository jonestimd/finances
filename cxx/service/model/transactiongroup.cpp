#include "transactiongroup.h"
#include "service/model/sql.h"
#include <QSqlField>

TransactionGroup::TransactionGroup() : NamedEntity() {}

TransactionGroup::TransactionGroup(QSqlRecord record) : NamedEntity(record) {
    name = record.field("name").value();
    description = sql::getValue(record, "description");
    transactions = record.field("transactions").value();
    details = sql::getValue(record, "details");
}

TransactionGroup::TransactionGroup(const QString &name) : NamedEntity{name} {}

bool TransactionGroup::deletable() const {
    return transactions.toInt() == 0;
}
