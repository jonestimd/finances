#ifndef RELATIONCOLUMNADAPTER_H
#define RELATIONCOLUMNADAPTER_H

#include "columnadapter.h"
#include "comboboxmodel.h"
#include "service/model/basedomain.h"
#include <QVariant>

template<class T, NameAndId V>
class RelationColumnAdapter : public ColumnAdapter<T> {
    using CreateValue = ComboBoxModel::CreateValue;

    ValuesSupplier<V> values;
    CreateValue createValue;

public:
    RelationColumnAdapter(QString title, QVariant T::*field, ValuesSupplier<V> values, CreateValue newValue = nullptr)
        : ColumnAdapter<T>(title, field, true), values{values}, createValue{newValue} {}

    QVariant value(const T *row, QVariant current, int role) const override {
        QVariant value = ColumnAdapter<T>::value(row, current, role);
        if (role == finances::SortRole && value.isNull()) return "";
        if (role == Qt::DisplayRole || role == finances::SortRole) {
            if (current.isValid() && current.isNull()) return "";
            auto values = this->values();
            if (value.isValid() && values.contains(value.toLongLong())) {
                return values.value(value.toLongLong())->displayName();
            }
        }
        if (role == Qt::EditRole) {
            if (current.isValid()) return current;
            const NamedEntity *v = this->values().value(value.toLongLong());
            return QVariant::fromValue(v);
        }
        if (role == finances::OptionsRole) {
            QList<const NamedEntity*> options;
            for (auto entity : values()) options.append(entity);
            auto model = new ComboBoxModel(options, createValue);
            return QVariant::fromValue(model);
        }
        return value;
    }

    bool isEqual(const QVariant &value1, const QVariant &value2) override {
        auto e1 = value1.value<const NamedEntity*>(), e2 = value2.value<const NamedEntity*>();
        if (e1) return e2 && ColumnAdapter<T>::isEqual(e1->id, e2->id);
        return !e2;
    }

    void setValue(T *row, QVariant value) override {
        auto entity = value.value<const V*>();
        ColumnAdapter<T>::setValue(row, entity ? entity->id : QVariant{});
    }
};

#endif // RELATIONCOLUMNADAPTER_H
