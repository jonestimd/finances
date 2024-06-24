#ifndef BOOLCOLUMNADAPTER_H
#define BOOLCOLUMNADAPTER_H

#include <QBrush>
#include <QDecNumber.hh>
#include <QVariant>
#include "columnadapter.h"

template<class T>
class BoolColumnAdapter : public ColumnAdapter<T> {
public:
    BoolColumnAdapter(QString title, QVariant T::* field) : ColumnAdapter<T>(title, field) {}

    QVariant value(const T *row, int role) const override {
        if (role == Qt::CheckStateRole) {
            QVariant value = ColumnAdapter<T>::value(row, Qt::DisplayRole);
            return value.toBool() ? Qt::Checked : Qt::Unchecked;
        }
        if (role == finances::SortRole) return ColumnAdapter<T>::value(row, Qt::DisplayRole);
        return QVariant{};
    }
};

#endif // BOOLCOLUMNADAPTER_H
