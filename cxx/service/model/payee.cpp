#include "payee.h"
#include <QSqlField>

Payee::Payee() : NamedEntity() {}

Payee::Payee(const QSqlRecord &record)
    : NamedEntity(record)
    , transactions{record.value("transactions").toInt()}
{}

Payee::Payee(const QString &name) : NamedEntity{name} {}

bool Payee::deletable() const {
    return transactions == 0;
}
