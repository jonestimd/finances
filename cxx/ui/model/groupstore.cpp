#include "groupstore.h"

GroupStore::GroupStore(GroupService *service)
    : EntityStore{service}
{}

void GroupStore::detailsUpdated(const QList<DetailChange> changes) {
    if (updateDetailCounts(changes, &TransactionDetail::groupId)) {
        emit valuesLoaded(ids());
    }
}
