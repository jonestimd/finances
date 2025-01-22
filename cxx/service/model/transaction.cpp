#include "transaction.h"
#include "service/database/mapping.h"
#include "service/database/sql.h"
#include <QSqlField>

Transaction::Transaction() {}

Transaction::Transaction(const QVariant &accountId) : accountId{accountId} {}

Transaction::Transaction(const QSqlRecord &record)
    : BaseDomain{record}
    , accountId{record.field("account_id").value()}
    , date{record.field("date").value()}
    , payeeId{sql::getValue(record, "payee_id")}
    , securityId{sql::getValue(record, "security_id")}
    , referenceNumber{sql::getValue(record, "reference_number")}
    , memo{sql::getValue(record, "memo")}
    , cleared{sql::yesNoValue(record, "cleared")}
    , detailIds(mapping::jsonToList(record.field("detail_ids").value()))
{}

bool Transaction::deletable() const {
    return true;
}
