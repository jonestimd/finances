#ifndef ASSET_H
#define ASSET_H

#include "basedomain.h"
#include "assettype.h"

class Asset : public NamedEntity {
public:
    QVariant type;
    QVariant scale;
    QVariant symbol{};
    QVariant transactions{0};

    Asset(const AssetType &type);
    Asset(const QSqlRecord &record);

    bool deletable() const;
};

#endif // ASSET_H
