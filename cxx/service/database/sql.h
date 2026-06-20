#ifndef SQL_H
#define SQL_H

#include <QSqlQuery>
#include <QSqlRecord>
#include <QVariant>
#include <QLoggingCategory>

Q_DECLARE_LOGGING_CATEGORY(sqlLogger)

#define SQL_BIND_VALUE(query, name, value) \
    query.bindValue((name), (value)); \
    qCDebug(sqlLogger) << "-" << (name) << "=" << (value);

#define SQL_BIND_LIST(query, name, values) \
    sql::bindList(query, name, values); \
    qCDebug(sqlLogger) << "-" << (name) << "=" << (values);

namespace sql {
    QVariant getValue(QSqlRecord record, const char *name, QVariant defaultValue = {});

    QVariant yesNoValue(QSqlRecord record, const char *name);

    void bindList(QSqlQuery &query, const char *name, const QVariantList &values);

    void exec(const QSqlDatabase &db, const char *query, const char *className, const char *queryName);

    void exec(QSqlQuery &query, const char *className, const char *queryName);

    QList<QVariant> loadValues(QSqlQuery& query, const char* column);
}

#endif // SQL_H
