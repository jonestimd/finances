#include "amounttype.h"

QHash<const QString, const AmountType*> AmountType::values;

AmountType::AmountType(const char *code, const QString name, bool affectsBalance, bool sharesRequired)
    : EnumValue(code, name), affectsBalance{affectsBalance}, sharesRequied{sharesRequired}
{
    values.insert(code, this);
}

const AmountType *AmountType::valueOf(const QVariant &value) {
    return values.value(value.toString());
}

const AmountType AmountType::debitDeposit(DEBIT_DEPOSIT, tr("Debit/despoit"), true, false);
const AmountType AmountType::assetValue(ASSET_VALUE, tr("Asset value"), false, true);
const AmountType AmountType::assetExchange(ASSET_EXCHANGE, tr("Asset Exchange"), true, true);
