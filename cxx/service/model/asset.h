#ifndef ASSET_H
#define ASSET_H

#include "basedomain.h"
#include "assettype.h"

class Asset : public NamedEntity {
public:
    const AssetType* type{&AssetType::security};
    int scale;
    QString symbol{};

    Asset(const AssetType* type, const QString &name = "");
    Asset(const QSqlRecord &record);
};

#endif // ASSET_H
