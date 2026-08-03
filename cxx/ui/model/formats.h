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

Q_GLOBAL_STATIC(const QString, dateDisplayFormat, QObject::tr("yyyy-MM-dd", "date display"));
Q_GLOBAL_STATIC(const QString, dateEditFormat, QObject::tr("yyyy/MM/dd", "date edit"));

#endif // FORMATS_H
