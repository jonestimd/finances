#include "category.h"
#include <QSqlField>

Category::Category() : NamedEntity() {}

Category::Category(QSqlRecord record) : NamedEntity(record) {
    name = record.field("code").value();
    amountType = record.field("amount_type").value();
    description = record.field("description").value();
    income = record.field("income").value().toString() == "Y";
    security = record.field("security").value().toString() == "Y";
    parentId = record.field("parent_id").value();
    transactions = record.field("transactions").value();
}

bool Category::deletable() const {
    return transactions.toInt() == 0;
}

QString Category::displayName() const {
    if (parentId.isValid() && categories.contains(parentId.toLongLong())) {
        auto parentName = categories.value(parentId.toLongLong())->displayName();
        return parentName + "\u25ba" + name.toString();
    }
    return name.toString();
}

QHash<qlonglong, const Category*> Category::categories{};
