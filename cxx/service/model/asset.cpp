#include "asset.h"
#include <QSqlField>

Asset::Asset(const AssetType &type)
    : NamedEntity()
    , type{type.code}
    , scale{type.defaultScale}
{}

Asset::Asset(const QSqlRecord &record)
    : NamedEntity(record)
    , type{record.field("type").value()}
    , scale{record.field("scale").value()}
    , symbol{record.field("symbol").value()}
    , transactions{record.field("transactions").value()}
{}

bool Asset::deletable() const {
    return transactions.toInt() == 0;
}
