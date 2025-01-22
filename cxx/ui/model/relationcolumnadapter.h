#ifndef RELATIONCOLUMNADAPTER_H
#define RELATIONCOLUMNADAPTER_H

#include "columnadapter.h"
#include "comboboxmodel.h"
#include "service/model/basedomain.h"
#include <QVariant>

template<class T, NameAndId V, class Store>
    requires requires(Store *s, QVariant id, ComboBoxModel::CreateValue createValue) {
        { s->value(id) } -> std::convertible_to<const V*>;
        { s->contains(id) } -> std::convertible_to<bool>;
        { s->newComboBoxModel(createValue) } -> std::convertible_to<ComboBoxModel*>;
    }
class RelationColumnAdapter : public ColumnAdapter<T> {
    using CreateValue = ComboBoxModel::CreateValue;

    const Store *store;
    CreateValue createValue;

public:
    RelationColumnAdapter(QString title, QVariant T::*field, const Store *store, CreateValue createValue = nullptr,
                          ValidatorFactory *validatorFactory = nullptr)
        : ColumnAdapter<T>(title, field, true, validatorFactory)
        , store{store}
        , createValue{createValue} {}

    /**
     * @param current pointer to new related entity if cell has been modified
     */
    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current, int role) const override {
        // value is the ID of the related entity
        QVariant value = ColumnAdapter<T>::value(row, index, getId(current), role);
        if (role == finances::SortRole && value.isNull()) return "";
        if (role == Qt::DisplayRole || role == finances::SortRole) {
            if (current.isValid() && current.isNull()) return "";
            if (value.isValid() && store->contains(value)) {
                return store->value(value)->name;
            }
        }
        if (role == Qt::EditRole) {
            if (current.isValid()) return current;
            const NamedEntity *v = store->value(value);
            return QVariant::fromValue(v);
        }
        if (role == finances::OptionsRole) {
            return QVariant::fromValue(store->newComboBoxModel(createValue));
        }
        return value;
    }

    virtual QVariant getId(const QVariant &row) const {
        auto entity = row.value<const NamedEntity*>();
        return entity ? entity->id : QVariant{};
    }

    virtual bool isEqual(const QVariant &value1, const QVariant &value2) const override {
        auto e1 = value1.value<const NamedEntity*>(), e2 = value2.value<const NamedEntity*>();
        if (e1) return e2 && ColumnAdapter<T>::isEqual(e1->id, e2->id);
        return !e2;
    }

    virtual void setValue(T *row, QVariant value) const override {
        auto entity = value.value<const NamedEntity*>();
        ColumnAdapter<T>::setValue(row, entity ? entity->id : QVariant{});
    }
};

#endif // RELATIONCOLUMNADAPTER_H
