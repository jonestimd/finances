#include "sql.h"
#include <QJsonArray>
#include <QJsonDocument>
#include <QSqlDriver>
#include <QSqlField>
#include <QSqlError>

Q_LOGGING_CATEGORY(sqlLogger, "sql")

QVariant sql::getValue(QSqlRecord record, const char *name, QVariant defaultValue) {
    auto field = record.field(name);
    if (field.isNull()) return defaultValue;
    // fix value comparison for table/tree cell edits on VARCHAR column
    auto value = field.value();
    if (value.typeId() == QMetaType::QByteArray) return QVariant{value.toString()};
    return value;
}

QVariant sql::yesNoValue(QSqlRecord record, const char *name) {
    return getValue(record, name).toString() == "Y";
}

void sql::bindList(QSqlQuery &query, const char *name, const QVariantList &values) {
    query.bindValue(name, QString::fromUtf8(QJsonDocument(QJsonArray::fromVariantList(values)).toJson()));
}

static void logAndThrowError(const QSqlQuery &query, const QString &className, const char *queryName) {
    auto connectionName = query.driver()->connectionName();
    auto driver = connectionName.split(':').first();
    qCritical().noquote().nospace() << driver << " " << className << "." << queryName << ": " << query.lastError().text();
    qCritical().noquote() << query.lastQuery();
    throw query.lastError().text();
}

void sql::exec(const QSqlDatabase &db, const char *sql, const char *className, const char *queryName) {
    qCInfo(sqlLogger, "%s.%s:\n%s", className, queryName, sql);
    QSqlQuery query(sql, db);
    if (query.lastError() != QSqlError()) logAndThrowError(query, className, queryName);
}

void sql::exec(QSqlQuery &query, const char *className, const char *queryName) {
    if (sqlLogger().isInfoEnabled()) qCInfo(sqlLogger, "%s.%s:\n%s", className, queryName, query.lastQuery().toLocal8Bit().data());
    if (!query.exec()) logAndThrowError(query, className, queryName);
}

QList<QVariant> sql::loadValues(QSqlQuery &query, const char *column) {
    QList<QVariant> result;
    while (query.next()) result.append(query.value(column));
    return result;
}
