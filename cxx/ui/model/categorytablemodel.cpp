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
        if (parentId.has_value()) values.append(QString::number(parentId.value()));
        return values;
    }
};

CategoryTableModel::CategoryTableModel(DataStore *ds)
    : PodItemModel<Category, CategoryStore> {
        ds->categoryStore,
        QList<ColumnAdapter<Category>*>{
            new FieldColumnAdapter<Category>(tr("Name"), &Category::name, true, new CategoryValidatorFactory()),
            new FieldColumnAdapter<Category>(tr("Description"), &Category::description, trimmedValidatorFactory),
            new EnumColumnAdapter<Category, AmountType>(tr("Amount Type"), &Category::amountType, &AmountType::values, requiredValidatorFactory, true),
            new NumberColumnAdapter<Category, int>(tr("Transactions"), &Category::details),
            new FieldColumnAdapter<Category>(tr("Income"), &Category::income),
            new FieldColumnAdapter<Category>(tr("Security"), &Category::security),
        },
    }
{}

int CategoryTableModel::childCount(const QModelIndex &parent) const {
    return parent.isValid() ? getRow(parent)->childIds.length() : rootIds.length();
}

void CategoryTableModel::setRows(QList<domain_id> categoryIds) {
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
    return row->id.has_value() && (row->parentId.has_value() || rootIds.size() > 1);
}

QModelIndex CategoryTableModel::index(int row, int column, const QModelIndex &parent) const {
    if (hasIndex(row, column, parent)) {
        if (parent.isValid()) {
            auto p = static_cast<const Category*>(parent.internalPointer());
            auto rowId = store->value(p->id.value())->childIds[row];
            return createIndex(row, column, store->value(rowId));
        }
        if (row < rootIds.length()) {
            auto rowId = rootIds.value(row);
            return createIndex(row, column, store->value(rowId));
        }
        auto category = newRows.value(parent).at(row - rootIds.length());
        return createIndex(row, column, category);
    }
    return QModelIndex{};
}

QModelIndex CategoryTableModel::parent(const QModelIndex &index) const {
    if (index.isValid()) {
        // TODO pending child add or parent change?
        auto child = static_cast<const Category*>(index.internalPointer());
        auto parentId = child->parentId;
        if (parentId.has_value()) {
            auto parent = store->value(parentId.value());
            auto gpId = parent->parentId;
            if (gpId.has_value()) {
                auto gp = store->value(gpId.value());
                auto row = gp->childIds.indexOf(child->id);
                return createIndex(row, 0, gp);
            }
            auto row = rootIds.indexOf(parentId);
            return createIndex(row, 0, parent);
        }
    }
    return QModelIndex{};
}
