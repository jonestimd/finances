#ifndef DECIMAL_H
#define DECIMAL_H

#include <QDecNumber.hh>
#include <QSqlRecord>
#include <QVariant>

QVariant decimalValue(const QSqlRecord &record, const char* name);

Q_GLOBAL_STATIC(QDecNumber, QDEC_ZERO, QDecNumber{0})

#endif // DECIMAL_H
