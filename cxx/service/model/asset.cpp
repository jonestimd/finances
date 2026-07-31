#include "asset.h"
#include "service/database/sql.h"
#include <QSqlField>

Asset::Asset(const AssetType* type, const QString &name)
    : NamedEntity(name)
    , type{type}
    , scale{type->defaultScale}
{}

Asset::Asset(const QSqlRecord &record)
    : NamedEntity(record)
    , type{sql::enumValue(record, "type", AssetType::values)}
    , scale{record.value("scale").toInt()}
    , symbol{sql::getString(record, "symbol")}
{}
