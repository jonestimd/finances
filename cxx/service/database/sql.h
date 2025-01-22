#ifndef SQL_H
#define SQL_H

#include <QSqlQuery>
#include <QSqlRecord>
#include <QVariant>

namespace sql {
    QVariant getValue(QSqlRecord record, const char *name, QVariant defaultValue = {});

    QVariant yesNoValue(QSqlRecord record, const char *name);

    void bindList(QSqlQuery &query, const QVariantList &values, const char *name);

    void exec(QSqlQuery &query, const QString &className, const char *queryName);
}

#endif // SQL_H
