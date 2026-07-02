#ifndef FORMATCOLUMNADAPTER_H
#define FORMATCOLUMNADAPTER_H

#include "columnadapter.h"

template<class T, class Value = QVariant>
class FormatColumnAdapter : public FieldColumnAdapter<T, Value> {
    typedef QString (*FormatterType)(const QVariant &);
public:
    const FormatterType formatter;

    FormatColumnAdapter(QString title, Value T::* field, FormatterType formatter, bool editable)
        : FieldColumnAdapter<T, Value>(title, field, editable), formatter{formatter} {}

    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current, int role) const override {
        QVariant value = FieldColumnAdapter<T, Value>::value(row, index, current, role);
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
