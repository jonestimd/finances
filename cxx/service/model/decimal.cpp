#include "decimal.h"
#include <QDecNumber.hh>
#include <QSqlField>

QVariant decimalValue(const QSqlRecord &record, const char* name) {
    return QVariant::fromValue(QDecNumber(record.field(name).value().toByteArray().constData()));
}
