#ifndef GROUPSTORE_H
#define GROUPSTORE_H

#include "entitystore.h"
#include "service/model/transactiongroup.h"
#include "service/servicecontext.h"

class GroupStore : public EntityStore<TransactionGroup, GroupService> {
    Q_OBJECT

public:
    GroupStore(GroupService *service, StatusMessageStore* messageStore);

public slots:
    void transactionsUpdated(const QHash<domain_id, TransactionChange> txChanges, const QHash<domain_id, DetailChange> detailChanges);
};

#endif // GROUPSTORE_H
