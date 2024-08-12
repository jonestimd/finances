#include "categorytablemodel.h"

#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "relationcolumnadapter.h"
#include "../validation/required.h"
#include "../validation/trimmed.h"
#include "../validation/unique.h"
#include <service/model/amounttype.h>

#define CATEGORY_NAME_COLUMN 1
#define CATEGORY_PARENT_COLUMN 2

CategoryTableModel::CategoryTableModel(DataStore *ds, QObject *parent)
    : dataStore{ds}
    , PodTableModel<Category> {
        QList<ColumnAdapter<Category>*>{
            new RelationColumnAdapter<Category, Category>(tr("Parent"), &Category::parentId, std::bind(&DataStore::categories, ds)),
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

#include "categorytablemodel.moc"
