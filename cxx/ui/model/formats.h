#ifndef FORMATS_H
#define FORMATS_H

#include <QDecNumber.hh>
#include <QVariant>

QString moneyFormat(const QVariant &amount);
QString moneyFormat(const QDecNumber &amount);
QString dollarFormat(const QVariant &amount);
QString dollarFormat(const QDecNumber &amount);

QString securityShares(const QVariant &amount);
QString dateFormat(const QVariant &value);

#endif // FORMATS_H
