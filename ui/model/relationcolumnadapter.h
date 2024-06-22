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
    QList<V*> *values;

public:
    RelationColumnAdapter(QString title, QVariant T::*field, QList<V*> *values)
        : ColumnAdapter<T>(title, field), values{values} {}

    QVariant value(const T *row, int role) const override {
        QVariant value = ColumnAdapter<T>::value(row, role);
        if (role == Finances::SortRole && value.isNull()) return "";
        if (role == Qt::DisplayRole || role == Finances::SortRole) {
            foreach (V* item, *values) {
                if (item->id == value) return item->name;
            }
        }
        return value;
    }
};

#endif // RELATIONCOLUMNADAPTER_H
