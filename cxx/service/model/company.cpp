#include "company.h"
#include <QSqlField>

Company::Company() : BaseDomain() {}

Company::Company(QSqlRecord record) : BaseDomain::BaseDomain(record) {
    name = record.field("name").value();
    accounts = record.field("accounts").value();
}
