#ifndef SQL_H
#define SQL_H

#include <QSqlQuery>
#include <QSqlRecord>
#include <QVariant>

namespace sql {
    QVariant getValue(QSqlRecord record, const char *name);

    QVariant yesNoValue(QSqlRecord record, const char *name);

    void bindArray(QSqlQuery &query, QJsonArray &value, const char *name);
}

#endif // SQL_H
