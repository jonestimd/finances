#include "category.h"
#include "../database/mapping.h"
#include "../database/sql.h"
#include <QSqlField>

Category::Category() : NamedEntity() {}

Category::Category(const QSqlRecord &record) : NamedEntity(record, "code") {
    amountType = sql::getValue(record, "amount_type");
    description = sql::getValue(record, "description");
    income = sql::yesNoValue(record, "income");
    security = sql::yesNoValue(record, "security");
    parentId = sql::getValue(record, "parent_id");
    childIds = mapping::jsonToList(record.field("child_ids").value());
    transactions = sql::getValue(record, "transactions");
    details = sql::getValue(record, "details");
}

Category::Category(const QString &name) : NamedEntity{name} {}

bool Category::deletable() const {
    return transactions.toInt() == 0 && childIds.empty();
}
