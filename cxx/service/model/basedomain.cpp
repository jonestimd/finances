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

bool NamedEntity::less(const NamedEntity *lhs, const NamedEntity *rhs) {
    auto name1 = lhs->displayName(), name2 = rhs->displayName();
    auto lname1 = name1.toLower(), lname2 = name2.toLower();
    return lname1 == lname2 ? name1 < name2 : lname1 < lname2;
}

QVariant NamedEntity::getId(const QVariant &value) {
    auto entity = value.value<const NamedEntity*>();
    return entity ? entity->id : QVariant{};
}

EnumValue::EnumValue(const char *code, const char *name)
    : code{code}, name{QObject::tr(name)} {}

bool EnumValue::less(const EnumValue *lhs, const EnumValue *rhs) {
    return lhs->name < rhs->name;
}
