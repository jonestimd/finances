#ifndef RELATIONCOLUMNADAPTER_H
#define RELATIONCOLUMNADAPTER_H

#include <QVariant>
#include "columnadapter.h"

template<typename T>
concept NameAndId = requires(T *t) {
    { t->id } -> std::convertible_to<QVariant>;
    { t->name } -> std::convertible_to<QVariant>;
};

template<class T, NameAndId V>
class RelationColumnAdapter : public ColumnAdapter<T> {
    std::function<QList<const V*>()> values;

public:
    RelationColumnAdapter(QString title, QVariant T::*field, std::function<QList<const V*>()> values)
        : ColumnAdapter<T>(title, field, false), values{values} {} // TODO editable

    QVariant value(const T *row, int role) const override {
        QVariant value = ColumnAdapter<T>::value(row, role);
        if (role == finances::SortRole && value.isNull()) return "";
        if (role == Qt::DisplayRole || role == finances::SortRole) {
            foreach (const V* item, this->values()) {
                if (item->id == value) return item->name;
            }
        }
        return value;
    }
};

#endif // RELATIONCOLUMNADAPTER_H
