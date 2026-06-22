#include "groupstore.h"

GroupStore::GroupStore(GroupService *service, StatusMessageStore* messageStore)
    : EntityStore{service, messageStore}
{}

void GroupStore::detailsUpdated(const QList<DetailChange> changes) {
    if (updateDetailCounts(changes, &TransactionDetail::groupId)) {
        emit valuesLoaded(ids());
    }
}
