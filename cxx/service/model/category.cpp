#include "category.h"
#include "../database/mapping.h"
#include "../database/sql.h"
#include <QSqlField>

Category::Category() : TransactionType{false} {}

Category::Category(const QSqlRecord &record)
    : TransactionType(false, record, "code")
    , amountType{sql::enumValue(record, "amount_type", AmountType::values)}
    , description{sql::getString(record, "description")}
    , income{sql::yesNoValue(record, "income")}
    , security{sql::yesNoValue(record, "security")}
    , parentId{sql::getInt(record, "parent_id")}
    , childIds(mapping::jsonToIntList(record.value("child_ids")))
    , details{sql::getValue(record, "details").toInt()}
{}

bool Category::deletable() const {
    return details == 0 && childIds.empty();
}
