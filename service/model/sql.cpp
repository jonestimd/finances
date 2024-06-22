#include "sql.h"
#include <QSqlField>

QVariant sql::getValue(QSqlRecord record, const char *name) {
    auto field = record.field(name);
    return field.isNull() ? QVariant{} : field.value();
}
