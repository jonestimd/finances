#include "basedomain.h"
#include <QDate>
#include <QSqlField>

BaseDomain::BaseDomain() {}

BaseDomain::BaseDomain(const QSqlRecord &record)
    : id{record.field("id").value()}
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

TransactionTypeId::TransactionTypeId(bool transfer, QVariant id) : transfer{transfer}, id{id} {}

TransactionTypeId::TransactionTypeId(const TransactionType &tt) : transfer{tt.transfer}, id{tt.id} {}

TransactionTypeId::TransactionTypeId(const TransactionType *tt) : transfer{tt->transfer}, id{tt->id} {}

EnumValue::EnumValue(const char *code, const QString name)
    : code{code}, name{name} {}

bool EnumValue::less(const EnumValue *lhs, const EnumValue *rhs) {
    return lhs->name < rhs->name;
}
