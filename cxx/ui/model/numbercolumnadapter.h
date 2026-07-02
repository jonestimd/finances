#ifndef NUMBERCOLUMNADAPTER_H
#define NUMBERCOLUMNADAPTER_H

#include <QBrush>
#include <QDecNumber.hh>
#include <QVariant>
#include "columnadapter.h"

static int numberAlignment = Qt::AlignVCenter | Qt::AlignTrailing;

template<class T, class Value = QVariant>
class NumberColumnAdapter : public FieldColumnAdapter<T, Value> {
public:
    NumberColumnAdapter(QString title, Value T::* field, bool editable = false, ValidatorFactory *factory = nullptr)
        : FieldColumnAdapter<T, Value>(title, field, editable, factory) {}

    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current, int role) const override {
        if (role == Qt::TextAlignmentRole) return numberAlignment; // TODO move
        return FieldColumnAdapter<T, Value>::value(row, index, current, role);
    }
};

#endif // NUMBERCOLUMNADAPTER_H
