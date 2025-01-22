#include "sql.h"
#include <QJsonArray>
#include <QJsonDocument>
#include <QSqlField>
#include <QSqlError>

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

void sql::bindList(QSqlQuery &query, const QVariantList &values, const char *name) {
    query.bindValue(name, QString::fromUtf8(QJsonDocument(QJsonArray::fromVariantList(values)).toJson()));
}

void sql::exec(QSqlQuery &query, const QString &className, const char *queryName) {
    if (!query.exec()) {
        qCritical().noquote().nospace() << className << "." << queryName << ": " << query.lastError().text();
        qCritical().noquote() << query.executedQuery();
        throw query.lastError().text();
    }
}
