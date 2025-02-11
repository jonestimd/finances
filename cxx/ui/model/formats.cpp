#include "formats.h"
#include <QTime>
#include <QDecNumber.hh>
#include <QRegularExpression>

QString moneyFormat(const QVariant &amount) {
    return moneyFormat(amount.value<QDecNumber>());
}

QString moneyFormat(const QDecNumber &value) {
    auto exponent = value.data()->exponent;
    QString result = value.toString();
    if (exponent == 0) result += ".00";
    else if (exponent == -1) result += "0";
    return result;
}

QString dollarFormat(const QVariant &amount) {
    return moneyFormat(amount).prepend("$");
}

QString dollarFormat(const QDecNumber &amount) {
    return moneyFormat(amount).prepend("$");
}

QString securityShares(const QVariant &amount) {
    if (amount.isNull()) return "";
    QDecNumber value = amount.value<QDecNumber>();
    return value.trim().toString();
}

QString dateFormat(const QVariant &date) {
    return date.value<QDate>().toString(Qt::ISODate);
}
