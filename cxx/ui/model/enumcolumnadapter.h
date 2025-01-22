#ifndef ENUMCOLUMNADAPTER_H
#define ENUMCOLUMNADAPTER_H

#include <QVariant>
#include "columnadapter.h"
#include "service/model/basedomain.h"

template<typename T>
concept Name = requires(T *t) {
    { t->name } -> std::convertible_to<QString>;
};

template<class T, Name V>
class EnumColumnAdapter : public ColumnAdapter<T> {
public:
    typedef std::function<bool(const T*, const V*)> IsCompatible;

private:
    QHash<QString, const V*> *values;
    IsCompatible isCompatible;

public:
    EnumColumnAdapter(QString title, QVariant T::*field, QHash<QString, const V*> *values,
                      ValidatorFactory *factory, bool editable = true, IsCompatible isCompatible = nullptr)
        : ColumnAdapter<T>(title, field, editable, factory), values{values}, isCompatible{isCompatible} {}

    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current, int role) const override {
        auto value = ColumnAdapter<T>::value(row, index, current, role);
        if (role == Qt::DisplayRole || role == finances::SortRole) {
            if (current.isValid()) return current.value<const EnumValue*>()->name;
            const EnumValue *enumValue = values->value(value.toString(), nullptr);
            if (enumValue) return enumValue->name;
        } else if (role == Qt::EditRole) {
            if (current.isValid()) return current;
            const EnumValue *enumValue = values->value(value .toString(), nullptr);
            if (enumValue) return QVariant::fromValue(enumValue);
        } else if (role == finances::OptionsRole) {
            QHash<QString, const EnumValue*> options;
            for (auto [code, option]: values->asKeyValueRange()) {
                if (!isCompatible || isCompatible(row, option)) options[code] = option;
            }
            return QVariant::fromValue(options);
        }
        return QVariant{};
    }

    virtual bool isEqual(const QVariant &value1, const QVariant &value2) const override {
        auto e1 = value1.value<const EnumValue*>(), e2 = value2.value<const EnumValue*>();
        if (e1) return e2 && ColumnAdapter<T>::isEqual(e1->name, e2->name);
        return !e2;
    }

    virtual void setValue(T *row, QVariant value) const override {
        auto entity = value.value<const EnumValue*>();
        ColumnAdapter<T>::setValue(row, entity ? entity->code: QVariant{});
    }
};

#endif // ENUMCOLUMNADAPTER_H
