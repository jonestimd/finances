#ifndef SQL_H
#define SQL_H

#include "QDecNumber.hh"
#include <QSqlField>
#include <QSqlQuery>
#include <QSqlRecord>
#include <QVariant>
#include <QLoggingCategory>
#include <QDate>

Q_DECLARE_LOGGING_CATEGORY(sqlLogger)

namespace sql {
    QVariant getValue(QSqlRecord record, const char *name, QVariant defaultValue = {});

    std::optional<qlonglong> getInt(QSqlRecord record, const char *name);

    QString getString(QSqlRecord record, const char* name);

    std::optional<QDate> getDate(QSqlRecord record, const char *name);

    std::optional<QDecNumber> decimalValue(const QSqlRecord &record, const char* name);

    QVariant yesNoValue(QSqlRecord record, const char *name);

    void bindValue(QSqlQuery &query, const char *name, const std::optional<qlonglong> &value);

    void bindValue(QSqlQuery &query, const char *name, const QVariant &value);

    inline void bindValue(QSqlQuery &query, const char *name, qlonglong value) {
        bindValue(query, name, QVariant{value});
    }

    void bindValue(QSqlQuery &query, const char *name, const QDecNumber& value);

    void bindValue(QSqlQuery &query, const char *name, const std::optional<QDecNumber>& value);

    void bindList(QSqlQuery &query, const char *name, const QList<qlonglong> &values);

    void bindList(QSqlQuery &query, const char *name, const QVariantList &values);

    void exec(const QSqlDatabase &db, const char *query, const char *className, const char *queryName);

    void exec(QSqlQuery &query, const char *className, const char *queryName);

    QList<qlonglong> loadValues(QSqlQuery& query, const char* column);
}

#endif // SQL_H
