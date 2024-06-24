#include "basedomain.h"

#include <QSqlField>

BaseDomain::BaseDomain() {}

BaseDomain::BaseDomain(QSqlRecord record) {
    id = record.field("id").value();
    version = record.field("version").value();
    changeUser = record.field("change_user").value();
    changeDate = record.field("change_date").value();
}
