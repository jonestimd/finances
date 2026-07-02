#include "mapping.h"
#include "QJsonArray"
#include "QJsonDocument"
#include "QJsonValue"

namespace mapping {
    QVariant toYesNo(const QVariant &boolValue) {
        return boolValue.toBool() ? "Y" : "N";
    }

    QList<qlonglong> jsonToIntList(const QVariant &value) {
        auto json = value.toByteArray();
        QList<qlonglong> values;
        if (!json.isEmpty()) {
            auto jsonArray = QJsonDocument::fromJson(json).array();
            for (auto i = jsonArray.cbegin(); i != jsonArray.cend(); i++) values.append((*i).toInteger());
        }
        return values;
    }

}
