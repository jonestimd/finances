#ifndef FORMATCOLUMNADAPTER_H
#define FORMATCOLUMNADAPTER_H

#include "columnadapter.h"

template<class T>
class FormatColumnAdapter : public ColumnAdapter<T> {
    typedef QString (*FormatterType)(const QVariant &);
public:
    const FormatterType formatter;

    FormatColumnAdapter(QString title, QVariant T::* field, FormatterType formatter, bool editable)
        : ColumnAdapter<T>(title, field, editable), formatter{formatter} {}

    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current, int role) const override {
        QVariant value = ColumnAdapter<T>::value(row, index, current, role);
        switch (role) {
        case Qt::DisplayRole:
        case finances::SortRole:
            if (!value.isNull()) return formatter(value);
            break;
        }
        return value;
    }
};

#endif // FORMATCOLUMNADAPTER_H
