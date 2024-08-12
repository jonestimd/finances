#include "payee.h"
#include <QSqlField>

Payee::Payee() : NamedEntity() {}

Payee::Payee(QSqlRecord record) : NamedEntity(record) {
    name = record.field("name").value();
    transactions = record.field("transactions").value();
}

bool Payee::deletable() const {
    return transactions.toInt() == 0;
}

QString Payee::displayName() const {
    return name.toString();
}
