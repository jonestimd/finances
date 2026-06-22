#ifndef GROUPSTORE_H
#define GROUPSTORE_H

#include "entitystore.h"
#include "service/model/transactiongroup.h"
#include "service/groupservice.h"

class GroupStore : public EntityStore<TransactionGroup, GroupService> {
    Q_OBJECT

public:
    GroupStore(GroupService *service, StatusMessageStore* messageStore);

public slots:
    void detailsUpdated(const QList<DetailChange> changes);
};

#endif // GROUPSTORE_H
