#include "asset.h"
#include "service/database/sql.h"
#include <QSqlField>

Asset::Asset(const AssetType* type)
    : NamedEntity()
    , type{type}
    , scale{type->defaultScale}
{}

Asset::Asset(const QSqlRecord &record)
    : NamedEntity(record)
    , type{sql::enumValue(record, "type", AssetType::values)}
    , scale{record.field("scale").value()}
    , symbol{record.field("symbol").value()}
    , transactions{record.field("transactions").value().toInt()}
{}

bool Asset::deletable() const {
    return transactions == 0;
}
