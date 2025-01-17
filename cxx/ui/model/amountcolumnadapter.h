#ifndef AMOUNTCOLUMNADAPTER_H
#define AMOUNTCOLUMNADAPTER_H

#include "../finances.h"
#include "numbercolumnadapter.h"
#include <QBrush>
#include <QDecNumber.hh>
#include <QVariant>

template<class T>
class AmountColumnAdapter : public NumberColumnAdapter<T> {
    typedef QString (*FormatterType)(const T*, const QVariant &);
public:
    const FormatterType formatter;

    AmountColumnAdapter(QString title, QVariant T::* field, FormatterType formatter, bool editable)
        : NumberColumnAdapter<T>(title, field, editable), formatter{formatter} {}

    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current, int role) const override {
        QVariant value = NumberColumnAdapter<T>::value(row, index, current, role);
        if (role == Qt::DisplayRole) return formatter(row, value);
        if (role == finances::SortRole) return value.value<QDecNumber>().toDouble();
        if (role == finances::TextHighlightRole) {
            QVariant amount = NumberColumnAdapter<T>::value(row, index, current, Qt::DisplayRole);
            if (amount.value<QDecNumber>().isNegative()) return true;
        }
        return value;
    }
};

#endif // AMOUNTCOLUMNADAPTER_H
