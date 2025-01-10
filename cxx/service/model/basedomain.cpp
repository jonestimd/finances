#include "basedomain.h"
#include <QDate>
#include <QSqlField>

BaseDomain::BaseDomain() {}

BaseDomain::BaseDomain(QSqlRecord record)
    : id{record.field("id").value()}
    , version{record.field("version").value()}
    , changeUser{record.field("change_user").value()}
    , changeDate{record.field("change_date").value()}
{}

NamedEntity::NamedEntity(QSqlRecord record, const char *nameColumn)
    : BaseDomain{record}
    , name{record.field(nameColumn).value().toString()}
{}

NamedEntity::NamedEntity(const QString &name) : BaseDomain{}, name{name} {}

QVariant NamedEntity::getId(const QVariant &value) {
    auto entity = value.value<const NamedEntity*>();
    return entity ? entity->id : QVariant{};
}

QString NamedEntity::getName(const NamedEntity *entity) {
    return entity->name.toString();
}

EnumValue::EnumValue(const char *code, const QString name)
    : code{code}, name{name} {}

bool EnumValue::less(const EnumValue *lhs, const EnumValue *rhs) {
    return lhs->name < rhs->name;
}
