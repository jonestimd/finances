#ifndef MAPPING_H
#define MAPPING_H

#include <QVariant>

namespace mapping {
    QVariant toYesNo(const QVariant &value);

    QList<QVariant> jsonToList(const QVariant &value);

    QList<qlonglong> jsonToIntList(const QVariant &value);
};

#endif // MAPPING_H
