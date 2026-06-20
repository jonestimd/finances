#include "decimal.h"
#include <QDecNumber.hh>
#include <QSqlField>

QVariant decimalValue(const QSqlRecord &record, const char* name) {
    if (record.contains(name) && !record.isNull(name)) {
        return QVariant::fromValue(QDecNumber(record.field(name).value().toByteArray().constData()));
    }
    return QVariant{};
}
