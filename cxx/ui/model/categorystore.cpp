#include "categorystore.h"
#include "datastore.h"
#include "ui/widget/statusmessage.h"

Q_STATIC_LOGGING_CATEGORY(logger, "store.category")

CategoryStore::CategoryStore(CategoryService* service, DataStore* dataStore)
    : EntityStore(service, &dataStore->messageStore)
    , dataStore{dataStore}
{}

const QSet<domain_id> CategoryStore::rootIds() const {
    QSet<domain_id> categoryIds;
    forEachEntry([=, &categoryIds](domain_id id, const Category* category) {
        if (!category->parentId.has_value()) categoryIds.insert(id);
    });
    return categoryIds;
}

QString CategoryStore::displayName(domain_id categoryId) const {
    auto category = value(categoryId);
    if (category) {
        auto name = category->name;
        if (category->parentId.has_value()) {
            return displayName(category->parentId.value()) + "\u25ba" + name;
        }
        return name;
    }
    else qCDebug(logger, "displayName: category not loaded: %lld", categoryId);
    return "";
}

bool CategoryStore::isAncestor(domain_id categoryId, const domain_id parentId) const {
    auto category = value(categoryId);
    if (!category->parentId.has_value()) return false;
    return category->parentId.value() == parentId || isAncestor(category->parentId.value(), parentId);
}

bool CategoryStore::hasChild(domain_id categoryId, const QString &name) const {
    auto category = value(categoryId);
    auto lowerName = name.toLower();
    for (const auto childId : category->childIds) {
        if (value(childId)->name.toLower() == lowerName) return true;
    }
    return false;
}

void CategoryStore::setParent(QWidget *source, const Category *category, const optional_id& parentId) {
    messageStore->addMessage(tr(SAVING_CATEGORIES));
    doInBackground(source, [this, category, parentId] {
        auto categories = service->setParent(category, parentId, user);
        update(categories.values());
        emit valuesLoaded(ids());
        QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, tr(SAVING_CATEGORIES));
    });
}

void CategoryStore::mergeCategories(QWidget *source, const Category *category, const domain_id destinationId) {
    messageStore->addMessage(tr(SAVING_CATEGORIES));
    doInBackground(source, [this, category, destinationId] {
        auto categories = service->merge(category, destinationId, user);
        dataStore->transactionStore->detailStore.replaceCategory(category->id, destinationId);
        update(categories.values(), QList{category});
        emit valuesLoaded(ids());
        QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, tr(SAVING_CATEGORIES));
    });
}

void CategoryStore::detailsUpdated(const QList<DetailChange> changes) {
    if (updateDetailCounts(changes, &TransactionDetail::categoryId)) {
        emit valuesLoaded(ids());
    }
}
