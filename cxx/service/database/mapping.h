#ifndef MAPPING_H
#define MAPPING_H

#include <QVariant>

namespace mapping {
    QVariant toYesNo(const QVariant &value);

    QList<qlonglong> jsonToSortedIntList(const QVariant &value);
};

#endif // MAPPING_H
