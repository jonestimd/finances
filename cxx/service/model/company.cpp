#include "company.h"
#include <QSqlField>

Company::Company() : accounts{0} {}

Company::Company(QSqlRecord record) : NamedEntity{record} {
    name = record.field("name").value().toString();
    accounts = record.field("accounts").value();
}

Company::Company(const QString &name) : name{name} {}

QString Company::displayName() const {
    return name.toString();
}

bool Company::deletable() const {
    return accounts.toInt() == 0;
}
