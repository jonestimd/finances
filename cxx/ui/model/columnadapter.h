#ifndef COLUMNADAPTER_H
#define COLUMNADAPTER_H

#include "../finances.h"
#include "../validation/validatorfactory.h"
#include <QVariant>

template<class T>
class ColumnAdapter {
protected:
    typedef std::function<bool(const T*)> IsEditable;

    QVariant T::* field;
    const IsEditable isEditable;
    ValidatorFactory *const validatorFactory;
public:
    const QString title;

    ColumnAdapter(QString title, QVariant T::* field, bool editable = true, ValidatorFactory *factory = nullptr)
        : ColumnAdapter(title, field, [editable](const T *r) { return editable; }, factory) {}

    ColumnAdapter(QString title, QVariant T::* field, IsEditable isEditable, ValidatorFactory *factory = nullptr)
        : title{title}, field{field}, isEditable{isEditable}, validatorFactory{factory} {}

    ~ColumnAdapter() {
        if (validatorFactory && validatorFactory->multiRow) delete validatorFactory;
    }

    void initialize(QAbstractItemModel *model) {
        if (validatorFactory) validatorFactory->initialize(model);
    }

    /**
     * @param current unsaved value if cell has been modified
     */
    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current = QVariant{}, int role = Qt::DisplayRole) const {
        switch (role) {
        case Qt::DisplayRole:
        case Qt::EditRole:
        case finances::SortRole:
            return current.isValid() ? current : row->*(this->field);
        case finances::ValidatorFactoryRole:
            if (validatorFactory) return QVariant::fromValue(validatorFactory->factory(index));
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

    const QString isValid(const QModelIndex &index) const {
        if (validatorFactory) {
            QString val = index.data().toString();
            return validatorFactory->isValid(index, val);
        }
        return nullptr;
    }

    QList<QModelIndex> revalidate(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const {
        if (validatorFactory) return validatorFactory->revalidate(errors, index);
        return QList<QModelIndex>{};
    }
};

#endif // COLUMNADAPTER_H
