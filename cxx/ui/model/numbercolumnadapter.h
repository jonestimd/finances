#ifndef NUMBERCOLUMNADAPTER_H
#define NUMBERCOLUMNADAPTER_H

#include <QBrush>
#include <QDecNumber.hh>
#include <QVariant>
#include "columnadapter.h"

static int numberAlignment = Qt::AlignVCenter | Qt::AlignTrailing;

template<class T>
class NumberColumnAdapter : public ColumnAdapter<T> {
public:
    NumberColumnAdapter(QString title, QVariant T::* field, bool editable = false)
        : ColumnAdapter<T>(title, field, editable) {}

    QVariant value(const T *row, int role) const override {
        if (role == Qt::TextAlignmentRole) return numberAlignment;
        return ColumnAdapter<T>::value(row, role);
    }
};

#endif // NUMBERCOLUMNADAPTER_H
