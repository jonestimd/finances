#include "company.h"
#include <QSqlField>

Company::Company() {}

Company::Company(const QSqlRecord &record)
    : NamedEntity{record}
    , accounts{record.field("accounts").value().toInt()}
{}

Company::Company(const QString &name) : NamedEntity{name} {}

bool Company::deletable() const {
    return accounts == 0;
}
