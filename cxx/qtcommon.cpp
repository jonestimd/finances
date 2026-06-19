
#include "QDecNumber.hh"
#include "service/model/basedomain.h"

namespace qtcommon {
    void registerConverters() {
        QMetaType::registerConverter<QDecNumber, QString>(
            [](const QDecNumber &value) -> QString { return QString(value.toString()); }
        );
        QMetaType::registerConverter<const EnumValue*, QString>(
            [](const EnumValue *value) -> QString { return value ? value->code : ""; }
        );
    }
}
