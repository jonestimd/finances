#ifndef COLUMNADAPTER_H
#define COLUMNADAPTER_H

#include "../finances.h"
#include "../validation/factory.h"
#include <QVariant>

template<class T>
class ColumnAdapter {
protected:
    QVariant T::* field;
    const bool editable;
    const ValidatorFactory *validatorFactory;
public:
    const QString title;

    ColumnAdapter(QString title, QVariant T::* field, bool editable = true, ValidatorFactory *factory = nullptr)
        : title{title}, field{field}, editable{editable}, validatorFactory{factory} {}

    virtual QVariant value(const T *row, int role = Qt::DisplayRole) const {
        switch (role) {
        case Qt::DisplayRole:
        case Qt::EditRole:
        case finances::SortRole:
            return row->*(this->field);
        case finances::ValidatorFactoryRole:
            if (validatorFactory) return QVariant::fromValue(validatorFactory);
            break;
        }
        return QVariant{};
    };

    virtual void setValue(T *row, QVariant value) {
        row->*(this->field) = value;
    }

    virtual Qt::ItemFlags flags(const T *row, bool allowEdit) const {
        return allowEdit && editable ? Qt::ItemIsEditable : Qt::NoItemFlags;
    }

    const QString isValid(const T *row, const QModelIndex &index, QObject *parent) const {
        if (validatorFactory) {
            QString val = value(row).toString();
            return validatorFactory->validator(index, parent)->isValid(val);
        }
        return nullptr;
    }
};

#endif // COLUMNADAPTER_H
