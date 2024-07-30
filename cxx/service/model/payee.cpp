#include "payee.h"
#include <QSqlField>

Payee::Payee() : BaseDomain() {}

Payee::Payee(QSqlRecord record) : BaseDomain(record) {
    name = record.field("name").value();
    transactions = record.field("transactions").value();
}

bool Payee::deletable() const {
    return transactions.toInt() == 0;
}
