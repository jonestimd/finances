#ifndef COLUMNADAPTER_H
#define COLUMNADAPTER_H

#include "../finances.h"
#include <QVariant>

template<class T>
class ColumnAdapter {
protected:
    QVariant T::* field;
public:
    const QString title;

    ColumnAdapter(QString title, QVariant T::* field) : title{title}, field{field} {}

    virtual QVariant value(const T *row, int role) const {
        if (role == Qt::DisplayRole || role == Finances::SortRole) return row->*(this->field);
        return QVariant{};
    };
};

#endif // COLUMNADAPTER_H
