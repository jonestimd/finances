#ifndef MAPPING_H
#define MAPPING_H

#include <QVariant>

namespace mapping {
    QVariant toYesNo(QVariant &value);

    QList<QVariant> jsonToList(const QVariant &value);
};

#endif // MAPPING_H
