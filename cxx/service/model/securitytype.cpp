#include "securitytype.h"

SecurityType::SecurityType(const char *code, const QString name)
    : EnumValue{code, name}
{
    values.insert(code, this);
}

QHash<QString, const SecurityType*> SecurityType::values;

const SecurityType SecurityType::bond("Bond", tr("Bond"));
const SecurityType SecurityType::moneyMarket("Money Market", tr("Money Market"));
const SecurityType SecurityType::mutualFund("Mutual Fund", tr("Mutual Fund"));
const SecurityType SecurityType::stock("Stock", tr("Stock"));
