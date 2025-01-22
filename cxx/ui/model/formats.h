#ifndef FORMATS_H
#define FORMATS_H

#include <QVariant>

QString moneyFormat(const QVariant &amount);
QString dollarFormat(const QVariant &amount);

QString securityShares(const QVariant &amount);
QString dateFormat(const QVariant &value);

#endif // FORMATS_H
