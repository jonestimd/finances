#include "basedomain.h"
#include "category.h"
#include <QDate>
#include <QSqlField>

namespace domain {
    QString toString(const optional_id& id) {
        return id.has_value() ? QString::number(id.value()) : "";
    }
}

BaseDomain::BaseDomain() {}

BaseDomain::BaseDomain(const QSqlRecord &record)
    : id{record.value("id").toLongLong()}
    , version{record.value("version").toLongLong()}
    , changeUser{record.value("change_user").toString()}
    , changeDate{record.value("change_date").toDateTime()}
{}

NamedEntity::NamedEntity(const QSqlRecord &record, const char *nameColumn)
    : BaseDomain{record}
    , name{record.value(nameColumn).toString()}
{}

NamedEntity::NamedEntity(const QString &name) : BaseDomain{}, name{name} {}

QString NamedEntity::getName(const NamedEntity *entity) {
    return entity->name;
}

TransactionType::TransactionType(bool transfer) : NamedEntity{}, transfer{transfer} {}

TransactionType::TransactionType(bool transfer, const QSqlRecord &record, const char *nameColumn)
    : NamedEntity{record, nameColumn}
    , transfer{transfer}
{}

const TransactionType *TransactionType::get(const QVariant &value) {
    return value.isValid() ? static_cast<const TransactionType*>(value.value<const NamedEntity*>()) : nullptr;
}

const Category *TransactionType::getCategory(const QVariant &value) {
    auto entity = get(value);
    return entity && !entity->transfer ? static_cast<const Category*>(entity) : nullptr;
}

TransactionTypeId::TransactionTypeId(bool transfer, const optional_id& id)
    : transfer{transfer}
    , id{id}
{}

TransactionTypeId::TransactionTypeId(const TransactionType &tt) : transfer{tt.transfer}, id{tt.id} {}

TransactionTypeId::TransactionTypeId(const TransactionType *tt) : transfer{tt->transfer}, id{tt->id} {}

EnumValue::EnumValue(const char *code, const QString name)
    : code{code}, name{name} {}

bool EnumValue::less(const EnumValue *lhs, const EnumValue *rhs) {
    return lhs->name < rhs->name;
}
