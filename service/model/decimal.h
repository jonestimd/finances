#ifndef DECIMAL_H
#define DECIMAL_H

#include <QSqlRecord>
#include <QVariant>

QVariant decimalValue(QSqlRecord record, const char* name);

#endif // DECIMAL_H
