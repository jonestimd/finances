#include "formats.h"
#include <QTime>
#include <QDecNumber.hh>

QString accountBalance(const Account *row, const QVariant &balance) {
    QDecNumber value = balance.value<QDecNumber>();
    return QString("%1%2").arg(row->currency.toString()).arg(value.toDouble(), 0, 'f', 2);
}

QString securityDollarAmount(const Security *row, const QVariant &amount) {
    QDecNumber value = amount.value<QDecNumber>();
    return QString("$%1").arg(value.toDouble(), 0, 'f', 2); // TODO currency
}

QString securityShares(const Security *row, const QVariant &amount) {
    QDecNumber value = amount.value<QDecNumber>();
    return QString("%1").arg(value.toDouble(), 0, 'f', row->scale.toInt());
}

QString formatDate(const QVariant &date) {
    return date.value<QDate>().toString(Qt::ISODate);
}
