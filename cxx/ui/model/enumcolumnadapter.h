#ifndef ENUMCOLUMNADAPTER_H
#define ENUMCOLUMNADAPTER_H

#include <QVariant>
#include "columnadapter.h"
#include "service/model/basedomain.h"

template<typename T>
concept Name = requires(T *t) {
    { t->name } -> std::convertible_to<QString>;
};

template<class T, Name Value>
class EnumColumnAdapter : public FieldColumnAdapter<T, const Value*> {
    typedef FieldColumnAdapter<T, const Value*> base_adapter;

public:
    typedef std::function<bool(const T*, const Value*)> IsCompatible;

private:
    QHash<const QString, const Value*> *values;
    IsCompatible isCompatible;

public:
    EnumColumnAdapter(QString title, const Value* T::*field, QHash<const QString, const Value*> *values,
                      ValidatorFactory *factory, bool editable = true, IsCompatible isCompatible = nullptr)
        : base_adapter(title, field, editable, factory), values{values}, isCompatible{isCompatible} {}

    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current, int role) const override {
        QVariant value = base_adapter::value(row, index, current, role);
        if (role == Qt::DisplayRole || role == finances::SortRole) {
            if (current.isValid()) return current.value<const EnumValue*>()->name;
            const EnumValue *enumValue = value.value<const EnumValue*>();
            if (enumValue) return enumValue->name;
        } else if (role == Qt::EditRole) {
            return current.isValid() ? current : value;
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
        if (e1) return e2 && base_adapter::isEqual(e1->name, e2->name);
        return !e2;
    }
};

#endif // ENUMCOLUMNADAPTER_H
