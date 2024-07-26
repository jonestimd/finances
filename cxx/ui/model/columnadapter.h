#ifndef COLUMNADAPTER_H
#define COLUMNADAPTER_H

#include "../finances.h"
#include "../validation/factory.h"
#include <QVariant>

template<class T>
class ColumnAdapter {
protected:
    typedef std::function<bool(const T*)> IsEditable;

    QVariant T::* field;
    const IsEditable isEditable;
    const ValidatorFactory *validatorFactory;
public:
    const QString title;

    ColumnAdapter(QString title, QVariant T::* field, bool editable = true, const ValidatorFactory *factory = nullptr)
        : ColumnAdapter(title, field, [editable](const T *r) { return editable; }, factory) {}

    ColumnAdapter(QString title, QVariant T::* field, IsEditable isEditable, const ValidatorFactory *factory = nullptr)
        : title{title}, field{field}, isEditable{isEditable}, validatorFactory{factory} {}

    virtual QVariant value(const T *row, const QVariant current = QVariant{}, int role = Qt::DisplayRole) const {
        switch (role) {
        case Qt::DisplayRole:
        case Qt::EditRole:
        case finances::SortRole:
            return current.isValid() ? current : row->*(this->field);
        case finances::ValidatorFactoryRole:
            if (validatorFactory) return QVariant::fromValue(validatorFactory);
            break;
        }
        return current.isValid() ? current : QVariant{};
    };

    virtual void setValue(T *row, QVariant value) {
        row->*(this->field) = value;
    }

    virtual bool isEqual(const QVariant &value1, const QVariant &value2) {
        return is_eq(QVariant::compare(value1, value2));
    }

    virtual Qt::ItemFlags flags(const T *row, bool allowEdit) const {
        return allowEdit && (!isEditable || isEditable(row)) ? Qt::ItemIsEditable : Qt::NoItemFlags;
    }

    const QString isValid(const T *row, const QModelIndex &index, QObject *parent) const {
        if (validatorFactory) {
            QString val = value(row).toString();
            return validatorFactory->validator(index, parent)->isValid(val);
        }
        return nullptr;
    }

    QList<QModelIndex> revalidate(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const {
        if (validatorFactory) return validatorFactory->revalidate(errors, index);
        return QList<QModelIndex>{};
    }
};

#endif // COLUMNADAPTER_H
