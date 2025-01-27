#include "categorytablemodel.h"

#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/required.h"
#include "../validation/trimmed.h"
#include "../validation/unique.h"
#include <service/model/amounttype.h>

#define CATEGORY_NAME_COLUMN 0

class CategoryValidatorFactory : public UniqueValidatorFactory {
public:
    CategoryValidatorFactory() : UniqueValidatorFactory{CATEGORY_NAME_COLUMN} {}

protected:
    virtual QStringList rowValues(const QString value, const QModelIndex &index) const override {
        auto values = UniqueValidatorFactory::rowValues(value, index);
        auto parentId = static_cast<const Category*>(index.internalPointer())->parentId;
        values.append(parentId.toString());
        return values;
    }
};

CategoryTableModel::CategoryTableModel(DataStore *ds)
    : PodItemModel<Category> {
        QList<ColumnAdapter<Category>*>{
            new ColumnAdapter<Category>(tr("Name"), &Category::name, true, new CategoryValidatorFactory()),
            new ColumnAdapter<Category>(tr("Description"), &Category::description, trimmedValidatorFactory),
            new EnumColumnAdapter<Category, AmountType>(tr("Amount Type"), &Category::amountType, &AmountType::values, requiredValidatorFactory, true),
            new NumberColumnAdapter<Category>(tr("Transactions"), &Category::transactions),
            new ColumnAdapter<Category>(tr("Income"), &Category::income),
            new ColumnAdapter<Category>(tr("Security"), &Category::security),
        },
    }
    , store{ds->categoryStore}
{}

int CategoryTableModel::childCount(const QModelIndex &parent) const {
    return parent.isValid() ? getRow(parent)->childIds.length() : rootIds.length();
}

void CategoryTableModel::setRows(QList<qlonglong> categoryIds) {
    clearChanges();
    beginResetModel();
    rootIds.clear();
    rootIds.append(store->rootIds().values());
    endResetModel();
}

const Category* CategoryTableModel::getRow(const QModelIndex &index) const {
    auto row = static_cast<const Category*>(index.internalPointer());
    return row;
}

int CategoryTableModel::rowCount(const QModelIndex &parent) const {
    auto addCount = pendingAdds(parent).length();
    if (!parent.isValid()) return rootIds.length() + addCount;
    return childCount(parent) + addCount;
}

bool CategoryTableModel::movable(const QModelIndex &index) {
    auto row = getRow(index);
    return row->id.isValid() && store->movable(row->id.toLongLong());
}

QModelIndex CategoryTableModel::index(int row, int column, const QModelIndex &parent) const {
    if (hasIndex(row, column, parent)) {
        if (parent.isValid()) {
            auto p = static_cast<const Category*>(parent.internalPointer());
            auto rowId = store->value(p->id.toLongLong())->childIds[row].toLongLong();
            return createIndex(row, column, store->value(rowId));
        }
        if (row < rootIds.length()) {
            auto rowId = rootIds.value(row);
            return createIndex(row, column, store->value(rowId));
        }
        auto category = newRows[parent][row - rootIds.length()];
        return createIndex(row, column, category);
    }
    return QModelIndex{};
}

QModelIndex CategoryTableModel::parent(const QModelIndex &index) const {
    if (index.isValid()) {
        // TODO pending child add or parent change?
        auto child = static_cast<const Category*>(index.internalPointer());
        auto parentId = child->parentId;
        if (!parentId.isNull()) {
            auto parent = store->value(parentId.toLongLong());
            auto gpId = parent->parentId;
            if (!gpId.isNull()) {
                auto gp = store->value(gpId.toLongLong());
                auto row = gp->childIds.indexOf(child->id);
                return createIndex(row, 0, gp);
            }
            auto row = rootIds.indexOf(parentId);
            return createIndex(row, 0, parent);
        }
    }
    return QModelIndex{};
}
