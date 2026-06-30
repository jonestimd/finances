#ifndef COLUMNADAPTER_H
#define COLUMNADAPTER_H

#include "../finances.h"
#include "../validation/validatorfactory.h"
#include "service/model/basedomain.h"
#include <QDate>
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

template<typename V>
struct extract_value {
    typedef std::void_t<V> value_type;
};

template<typename V>
struct extract_value<std::optional<V>> {
    typedef V value_type;
};


template<class T, class Value = QVariant>
class FieldColumnAdapter : public ColumnAdapter<T> {
    typedef extract_value<Value>::value_type optional_value;
    Value T::* const field;

public:
    FieldColumnAdapter(QString title, Value T::* field, bool editable = true, ValidatorFactory *factory = nullptr)
        : ColumnAdapter<T>(title, [editable](const T *r) { return editable; }, factory), field{field} {}

    FieldColumnAdapter(QString title, Value T::* field, ColumnAdapter<T>::IsEditable isEditable, ValidatorFactory *factory = nullptr)
        : ColumnAdapter<T>{factory, title, isEditable, factory}, field{field} {}

    virtual QVariant rowValue(const T* row) const override {
        auto value = row->*field;
        if constexpr (std::is_convertible_v<Value, QVariant>) return value;
        else if constexpr (std::is_convertible_v<optional_value, QVariant> || std::is_same_v<optional_value, QDecNumber>) {
            return value.has_value() ? QVariant::fromValue(value.value()) : QVariant{};
        } else if constexpr (std::is_same_v<Value, QDecNumber>) {
            return value.isNaN() ? QVariant{} : QVariant::fromValue(value);
        } else static_assert(false, "unsupported value type for FieldColumnAdapter");
    }

    virtual void setValue(T *row, QVariant value) const  override {
        if (value.isValid() && value.toString().isEmpty()) value = QVariant{};
        if constexpr (std::is_same_v<Value, QVariant>) row->*field = value;
        else if constexpr (std::is_convertible_v<optional_value, QVariant> || std::is_same_v<optional_value, QDecNumber>) {
            if (value.isNull()) (row->*field).reset();
            else (row->*field).emplace(value.value<optional_value>());
        } else row->*field = value.value<Value>();
    }
};

#endif // COLUMNADAPTER_H
