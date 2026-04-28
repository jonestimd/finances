#ifndef DBDIALECT_H
#define DBDIALECT_H

#include <QSqlDatabase>
#include <QString>

namespace dbDialect {
    QString createTableSql(const QSqlDatabase &db, QString sqlTemplate);

    QString inList(const QSqlDatabase &db, const char *column, const char *placeholder);

    QString replaceJsonArrayAgg(const QSqlDatabase &db, QString sql);

    QSqlQuery prepareGetByIds(const QSqlDatabase &db, const char *getAllSql, QVariantList ids, const char *idColumn);
};

#endif // DBDIALECT_H
