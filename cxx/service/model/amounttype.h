#ifndef AMOUNT_TYPE_H
#define AMOUNT_TYPE_H

#include "service/model/basedomain.h"
#include <QHash>
#include <QObject>
#include <QString>

#define DEBIT_DEPOSIT "DEBIT_DEPOSIT"
#define ASSET_VALUE "ASSET_VALUE"

class AmountType : public EnumValue {
    Q_OBJECT
    AmountType(const char *code, const QString name, bool affectsBalance);
public:
    const bool affectsBalance;

    static const AmountType debitDeposit;
    static const AmountType assetValue;

    static QHash<QString, const AmountType*> values;
};

#endif // AMOUNT_TYPE_H
