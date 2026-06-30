#ifndef ASSET_H
#define ASSET_H

#include "basedomain.h"
#include "assettype.h"

class Asset : public NamedEntity {
public:
    const AssetType* type{&AssetType::security};
    QVariant scale;
    QVariant symbol{};
    mutable int transactions{0};

    Asset(const AssetType* type);
    Asset(const QSqlRecord &record);

    bool deletable() const;
};

#endif // ASSET_H
