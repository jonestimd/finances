#include "basedomain.h"

#include <QDate>
#include <QSqlField>

BaseDomain::BaseDomain() : version{0} {}

BaseDomain::BaseDomain(QSqlRecord record) {
    id = record.field("id").value();
    version = record.field("version").value();
    changeUser = record.field("change_user").value();
    changeDate = record.field("change_date").value();
}

NamedEntity::NamedEntity(QSqlRecord record) : BaseDomain{record} {}
