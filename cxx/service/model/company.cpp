#include "company.h"
#include <QSqlField>

Company::Company() : BaseDomain(), accounts{0} {}

Company::Company(QSqlRecord record) : BaseDomain::BaseDomain(record) {
    name = record.field("name").value().toString();
    accounts = record.field("accounts").value();
}
