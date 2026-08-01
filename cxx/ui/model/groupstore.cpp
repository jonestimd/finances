#include "groupstore.h"

GroupStore::GroupStore(GroupService *service, StatusMessageStore* messageStore)
    : EntityStore{service, messageStore}
{}

void GroupStore::transactionsUpdated(const QHash<domain_id, TransactionChange> txChanges, const QHash<domain_id, DetailChange> detailChanges) {
    if (updateDetailCounts(detailChanges.values(), &TransactionDetail::groupId)) {
        emit valuesLoaded(ids());
    }
}
