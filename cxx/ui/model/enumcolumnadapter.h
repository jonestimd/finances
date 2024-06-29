#ifndef ENUMCOLUMNADAPTER_H
#define ENUMCOLUMNADAPTER_H

#include <QVariant>
#include "columnadapter.h"

template<typename T>
concept Name = requires(T t) {
    { t.name } -> std::convertible_to<const char*>;
};

template<class T, Name V>
class EnumColumnAdapter : public ColumnAdapter<T> {
    QHash<QString, V> values;

public:
    EnumColumnAdapter(QString title, QVariant T::*field, QHash<QString, V> values)
        : ColumnAdapter<T>(title, field, false), values{values} {} // TODO editable

    QVariant value(const T *row, int role) const override {
        QVariant value = ColumnAdapter<T>::value(row, role);
        if (role == Qt::DisplayRole) {
            if (value.isValid()) {
                QString code = value.toString();
                if (values.keys().contains(code)) return values[code].name;
            }
        }
        return QVariant{};
    }
};

#endif // ENUMCOLUMNADAPTER_H
