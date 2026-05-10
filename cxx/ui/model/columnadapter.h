#ifndef COLUMNADAPTER_H
#define COLUMNADAPTER_H

#include "../finances.h"
#include "../validation/validatorfactory.h"
#include <QVariant>

class AbstractColumnAdapter {
protected:
    ValidatorFactory *const validatorFactory;
public:
    const QString title;

    AbstractColumnAdapter(ValidatorFactory *factory, const QString title);
    virtual ~AbstractColumnAdapter();

    void initialize(QAbstractItemModel *model);

    virtual QVariant parseValue(const QVariant &value);

    virtual bool isEqual(const QVariant &value1, const QVariant &value2) const;

    const QString isValid(const QModelIndex &index) const;

    QList<QModelIndex> revalidateRows(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const;
};

template<class T>
class ColumnAdapter : public AbstractColumnAdapter {
protected:
    typedef std::function<bool(const T*)> IsEditable;

    QVariant T::* field;
    const IsEditable isEditable;
public:
    ColumnAdapter(QString title, QVariant T::* field, bool editable = true, ValidatorFactory *factory = nullptr)
        : ColumnAdapter(title, field, [editable](const T *r) { return editable; }, factory) {}

    ColumnAdapter(QString title, QVariant T::* field, IsEditable isEditable, ValidatorFactory *factory = nullptr)
        : AbstractColumnAdapter{factory, title}
        , field{field}, isEditable{isEditable} {}

    /**
     * @param current unsaved value if cell has been modified
     */
    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current = QVariant{}, int role = Qt::DisplayRole) const {
        switch (role) {
        case Qt::DisplayRole:
        case Qt::EditRole:
        case finances::SortRole:
        case finances::EntityIdRole:
            return current.isValid() ? current : fieldValue(row);
        case finances::ValidatorFactoryRole:
            if (validatorFactory) return QVariant::fromValue(validatorFactory->factory(index));
            break;
        }
        return QVariant{};
    };

    virtual QVariant fieldValue(const T *row) const {
        return row->*(this->field);
    }

    virtual void setValue(T *row, QVariant value) const {
        row->*(this->field) = value;
    }

    virtual Qt::ItemFlags flags(const T *row, bool allowEdit) const {
        return allowEdit && (!isEditable || isEditable(row)) ? Qt::ItemIsEditable : Qt::NoItemFlags;
    }
};

#endif // COLUMNADAPTER_H
