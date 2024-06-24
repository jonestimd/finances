#include "formats.h"
#include <QDecNumber.hh>

QString accountBalance(const Account *row, QVariant balance) {
    QDecNumber value = balance.value<QDecNumber>();
    return QString("%1%2").arg(row->currency.toString()).arg(value.toDouble(), 0, 'f', 2);
}
