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
    , parentId{sql::getValue(record, "parent_id")}
    , childIds(mapping::jsonToList(record.field("child_ids").value()))
    , transactions{sql::getValue(record, "transactions")}
    , details{sql::getValue(record, "details")}
{}

bool Category::deletable() const {
    return transactions.toInt() == 0 && childIds.empty();
}
