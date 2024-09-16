#include "mapping.h"
#include "QJsonArray"
#include "QJsonDocument"
#include "QJsonValue"

namespace mapping {
    QVariant toYesNo(const QVariant &boolValue) {
        return boolValue.toBool() ? "Y" : "N";
    }

    QList<QVariant> jsonToList(const QVariant &value) {
        auto json = value.toByteArray();
        return json.isEmpty() ? QList<QVariant>() : QJsonDocument::fromJson(json).array().toVariantList();
    }
}
