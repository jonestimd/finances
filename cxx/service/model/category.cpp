#include "category.h"
#include "../database/mapping.h"
#include "../database/sql.h"
#include <QSqlField>

Category::Category() : TransactionType{false} {}

Category::Category(const QSqlRecord &record)
    : TransactionType(false, record, "code")
    , amountType{sql::getValue(record, "amount_type", DEBIT_DEPOSIT)}
    , description{sql::getValue(record, "description")}
    , income{sql::yesNoValue(record, "income")}
    , security{sql::yesNoValue(record, "security")}
    , parentId{sql::getInt(record, "parent_id")}
    , childIds(mapping::jsonToIntList(record.field("child_ids").value()))
    , details{sql::getValue(record, "details")}
{}

bool Category::deletable() const {
    return details.toInt() == 0 && childIds.empty();
}
