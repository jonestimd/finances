#ifndef COLUMNADAPTER_H
#define COLUMNADAPTER_H

#include "../finances.h"
#include "../validation/validatorfactory.h"
#include "service/model/basedomain.h"
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

    QList<QModelIndex> revalidateRows(QHash<const QModelIndex, QString> &errors, const QModelIndex &index) const;
};

template<class T>
class ColumnAdapter : public AbstractColumnAdapter {
protected:
    typedef std::function<bool(const T*)> IsEditable;

    const IsEditable isEditable;
public:
    ColumnAdapter(QString title, bool editable = true, ValidatorFactory *factory = nullptr)
        : ColumnAdapter(title, [editable](const T *r) { return editable; }, factory) {}

    ColumnAdapter(QString title, IsEditable isEditable, ValidatorFactory *factory = nullptr)
        : AbstractColumnAdapter{factory, title}
        , isEditable{isEditable}
    {}

    /**
     * @param current unsaved value if cell has been modified
     */
    virtual QVariant value(const T *row, const QModelIndex &index, const QVariant current = QVariant{}, int role = Qt::DisplayRole) const {
        switch (role) {
        case Qt::DisplayRole:
        case Qt::EditRole:
        case finances::SortRole:
        case finances::EntityIdRole:
            return current.isValid() ? current : rowValue(row);
        case finances::ValidatorFactoryRole:
            if (validatorFactory) return QVariant::fromValue(validatorFactory->factory(index));
            break;
        }
        return QVariant{};
    };

    virtual QVariant rowValue(const T *row) const = 0;

    virtual void setValue(T *row, QVariant value) const  {}

    virtual Qt::ItemFlags flags(const T *row, bool allowEdit) const {
        return allowEdit && (!isEditable || isEditable(row)) ? Qt::ItemIsEditable : Qt::NoItemFlags;
    }
};

template<class T, class Value = QVariant>
class FieldColumnAdapter : public ColumnAdapter<T> {
    Value T::* const field;

public:
    FieldColumnAdapter(QString title, Value T::* field, bool editable = true, ValidatorFactory *factory = nullptr)
        : ColumnAdapter<T>(title, [editable](const T *r) { return editable; }, factory), field{field} {}

    FieldColumnAdapter(QString title, Value T::* field, ColumnAdapter<T>::IsEditable isEditable, ValidatorFactory *factory = nullptr)
        : ColumnAdapter<T>{factory, title, isEditable, factory}, field{field} {}

    virtual QVariant rowValue(const T* row) const override {
        auto value = row->*field;
        if constexpr (std::is_same_v<Value, QVariant>) return value;
        if constexpr (std::is_same_v<Value, int>) return value;
        if constexpr (std::is_same_v<Value, optional_id>) return domain::toQVaraint(value);
        if constexpr (std::is_same_v<Value, std::optional<QDecNumber>>) {
            return value.has_value() ? QVariant::fromValue(value.value()) : QVariant{};
        }
        if constexpr (std::is_same_v<Value, QDecNumber>) {
            return value.isNaN() ? QVariant{} : QVariant::fromValue(value);
        }
    }

    virtual void setValue(T *row, QVariant value) const  override {
        if (value.isValid() && value.toString().isEmpty()) value = QVariant{};
        if constexpr (std::is_same_v<Value, QVariant>) row->*field = value;
        else if constexpr (std::is_same_v<Value, int>) row->*field = value.toInt();
        else if constexpr (std::is_same_v<Value, optional_id>) row->*field = domain::toOptionalId(value);
        else if constexpr (std::is_same_v<Value, QDecNumber>) row->*field = value.value<QDecNumber>();
        else if constexpr (std::is_same_v<Value, std::optional<QDecNumber>>) {
            if (value.isNull()) (row->*field).reset();
            else (row->*field).emplace(value.value<QDecNumber>());
        } else static_assert(false, "unsupported value type for FieldColumnAdapter");
    }
};

#endif // COLUMNADAPTER_H
