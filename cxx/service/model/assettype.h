#ifndef ASSETTYPE_H
#define ASSETTYPE_H

#include "basedomain.h"

#define CURRENCY_ASSET "Currency"
#define SECURITY_ASSET "Security"

class AssetType : public EnumValue {
    AssetType(const char *code, const QString name, qint16 defaultScale);

public:
    const qint16 defaultScale;

    static const AssetType currency;
    static const AssetType security;

    static QHash<QString, const AssetType*> values;
};

#endif // ASSETTYPE_H
