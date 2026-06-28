#include "payee.h"
#include <QSqlField>

Payee::Payee() : NamedEntity() {}

Payee::Payee(const QSqlRecord &record)
    : NamedEntity(record)
    , transactions{record.field("transactions").value().toInt()}
{}

Payee::Payee(const QString &name) : NamedEntity{name} {}

bool Payee::deletable() const {
    return transactions == 0;
}
