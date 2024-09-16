#include "category.h"
#include "amounttype.h"
#include "../database/mapping.h"
#include "sql.h"
#include <QSqlField>

Category::Category() : NamedEntity(), income{false}, security{false}, amountType{DEBIT_DEPOSIT}, childIds() {}

Category::Category(QSqlRecord record) : NamedEntity(record) {
    name = sql::getValue(record, "code");
    amountType = sql::getValue(record, "amount_type");
    description = sql::getValue(record, "description");
    income = sql::yesNoValue(record, "income");
    security = sql::yesNoValue(record, "security");
    parentId = sql::getValue(record, "parent_id");
    childIds = mapping::jsonToList(record.field("child_ids").value());
    transactions = sql::getValue(record, "transactions");
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
