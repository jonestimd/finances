#include "payee.h"
#include <QSqlField>

Payee::Payee() : NamedEntity() {}

Payee::Payee(QSqlRecord record) : NamedEntity(record) {
    name = record.field("name").value();
    transactions = record.field("transactions").value();
}

Payee::Payee(const QString &name) : NamedEntity{name} {}

bool Payee::deletable() const {
    return transactions.toInt() == 0;
}
