#ifndef AMOUNT_TYPE_H
#define AMOUNT_TYPE_H

#include "basedomain.h"
#include <QHash>
#include <QObject>
#include <QString>

#define DEBIT_DEPOSIT "DEBIT_DEPOSIT"
#define ASSET_VALUE "ASSET_VALUE"
#define ASSET_EXCHANGE "ASSET_EXCHANGE"

class AmountType : public EnumValue {
    AmountType(const char *code, const QString name, bool affectsBalance, bool sharesRequired);

public:
    const bool affectsBalance;
    const bool sharesRequied;

    static const AmountType debitDeposit;
    static const AmountType assetValue;
    static const AmountType assetExchange;

    static QHash<const QString, const AmountType*> values;
    static const AmountType* valueOf(const QVariant &value);
};

#endif // AMOUNT_TYPE_H
