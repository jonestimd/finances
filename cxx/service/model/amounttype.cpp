#include "amounttype.h"

QHash<QString, const AmountType*> AmountType::values;

AmountType::AmountType(const char *code, const QString name, bool affectsBalance)
    : EnumValue(code, name), affectsBalance{affectsBalance}
{
    values[code] = this;
}

const AmountType AmountType::debitDeposit = AmountType("DEBIT_DEPOSIT", tr("Debit/despoit"), true);
const AmountType AmountType::assetValue = AmountType("ASSET_VALUE", tr("Asset value"), false);

#include "amounttype.moc"
