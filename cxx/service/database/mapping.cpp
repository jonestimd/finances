#include "mapping.h"

namespace mapping {
    QVariant toYesNo(QVariant &boolValue) {
        return boolValue.toBool() ? "Y" : "N";
    }
}
