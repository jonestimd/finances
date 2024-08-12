#include "categorytablemodel.h"

#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
// #include "relationcolumnadapter.h"
#include "../validation/required.h"
#include "../validation/trimmed.h"
#include "../validation/unique.h"
#include <service/model/amounttype.h>

#define CATEGORY_NAME_COLUMN 1
#define CATEGORY_PARENT_COLUMN 2

CategoryTableModel::CategoryTableModel(DataStore *ds, QObject *parent)
    : dataStore{ds}
    , categories()
    , rootIds()
    , PodItemModel<Category> {
        QList<ColumnAdapter<Category>*>{
            // new RelationColumnAdapter<Category, Category>(tr("Parent"), &Category::parentId, std::bind(&DataStore::categories, ds)),
            new ColumnAdapter<Category>(tr("Name"), &Category::name, true, new UniqueValidatorFactory(CATEGORY_NAME_COLUMN, QList<int>{CATEGORY_PARENT_COLUMN})),
            new ColumnAdapter<Category>(tr("Description"), &Category::description, trimmedValidatorFactory),
            new EnumColumnAdapter<Category, AmountType>(tr("Amount Type"), &Category::amountType, &AmountType::values, requiredValidatorFactory, true),
            new NumberColumnAdapter<Category>(tr("Transactions"), &Category::transactions),
            new ColumnAdapter<Category>(tr("Income"), &Category::income),
            new ColumnAdapter<Category>(tr("Security"), &Category::security),
        },
        parent,
    }
{}

int CategoryTableModel::childCount(const QModelIndex &parent) const {
    return parent.isValid() ? getRow(parent)->childIds.length() : rootIds.length();
}

void CategoryTableModel::setRows(QHash<qlonglong, const Category*> categories) {
    clearChanges();
    beginResetModel();
    this->categories.clear();
    rootIds.clear();
    this->categories.insert(categories);
    for (const Category* category : std::as_const(categories)) {
        if (category->parentId.isNull()) rootIds.append(category->id);
    }
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

// const QList<Category*> CategoryTableModel::unsavedAdds() const { // TODO
//     QList<Row*> rows;
//     for (auto row : newRows) {
//         rows.append(new Row(*row));
//     }
//     return rows;
// }

QModelIndex CategoryTableModel::index(int row, int column, const QModelIndex &parent) const {
    if (hasIndex(row, column, parent)) {
        // TODO pending add?
        if (parent.isValid()) {
            auto p = static_cast<const Category*>(parent.internalPointer());
            auto rowId = categories[p->id.toLongLong()]->childIds[row].toLongLong();
            return createIndex(row, column, categories[rowId]);
        }
        auto rowId = rootIds[row].toLongLong();
        return createIndex(row, column, categories[rowId]);
    }
    return QModelIndex{};
}

QModelIndex CategoryTableModel::parent(const QModelIndex &index) const {
    if (index.isValid()) {
        // TODO pending add?
        auto child = static_cast<const Category*>(index.internalPointer());
        auto parentId = child->parentId;
        if (!parentId.isNull()) {
            auto parent = categories[parentId.toLongLong()];
            auto gpId = parent->parentId;
            if (!gpId.isNull()) {
                auto gp = categories[gpId.toLongLong()];
                auto row = gp->childIds.indexOf(child->id);
                return createIndex(row, 0, gp);
            }
            auto row = rootIds.indexOf(parentId);
            return createIndex(row, 0, parent);
        }
    }
    return QModelIndex{};
}
