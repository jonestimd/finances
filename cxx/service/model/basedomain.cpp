#include "basedomain.h"
#include "category.h"
#include <QDate>
#include <QSqlField>

BaseDomain::BaseDomain() {}

BaseDomain::BaseDomain(const QSqlRecord &record)
    : id{record.field("id").value().toLongLong()}
    , version{record.field("version").value()}
    , changeUser{record.field("change_user").value()}
    , changeDate{record.field("change_date").value()}
{}

NamedEntity::NamedEntity(const QSqlRecord &record, const char *nameColumn)
    : BaseDomain{record}
    , name{record.field(nameColumn).value().toString()}
{}

NamedEntity::NamedEntity(const QString &name) : BaseDomain{}, name{name} {}

QString NamedEntity::getName(const NamedEntity *entity) {
    return entity->name.toString();
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

TransactionTypeId::TransactionTypeId(bool transfer, const std::optional<qlonglong>& id)
    : transfer{transfer}
    , id{id}
{}

TransactionTypeId::TransactionTypeId(bool transfer, const QVariant& id)
    : transfer{transfer}
    , id{id.isValid() ? std::optional<qlonglong>{id.toLongLong()} : std::optional<qlonglong>{}}
{}

TransactionTypeId::TransactionTypeId(const TransactionType &tt) : transfer{tt.transfer}, id{tt.id} {}

TransactionTypeId::TransactionTypeId(const TransactionType *tt) : transfer{tt->transfer}, id{tt->id} {}

EnumValue::EnumValue(const char *code, const QString name)
    : code{code}, name{name} {}

bool EnumValue::less(const EnumValue *lhs, const EnumValue *rhs) {
    return lhs->name < rhs->name;
}
