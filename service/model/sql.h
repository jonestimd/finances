#ifndef SQL_H
#define SQL_H

#include <QSqlRecord>
#include <QVariant>

namespace sql {
    QVariant getValue(QSqlRecord record, const char *name);
}

#endif // SQL_H
