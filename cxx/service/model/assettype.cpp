#include "assettype.h"

AssetType::AssetType(const char *code, const QString name, qint16 defaultScale)
    : EnumValue(code, name), defaultScale{defaultScale}
{
    values[code] = this;
}

QHash<QString, const AssetType*> AssetType::values;

const AssetType AssetType::currency(CURRENCY_ASSET, tr("Currency"), 2);
const AssetType AssetType::security(SECURITY_ASSET, tr("Security"), 6);
