#include "company.h"
#include <QSqlField>

Company::Company() {}

Company::Company(QSqlRecord record) : NamedEntity{record} {
    accounts = record.field("accounts").value();
}

Company::Company(const QString &name) : NamedEntity{name} {}

bool Company::deletable() const {
    return accounts.toInt() == 0;
}
