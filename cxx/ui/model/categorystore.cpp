#include "categorystore.h"
#include "datastore.h"
#include "ui/widget/statusmessage.h"

Q_STATIC_LOGGING_CATEGORY(logger, "store.category")

CategoryStore::CategoryStore(CategoryService* service, DataStore* dataStore)
    : EntityStore(service, &dataStore->messageStore)
    , dataStore{dataStore}
{}

const QSet<domain_id> CategoryStore::rootIds() const {
    return rootIds_;
}

QString CategoryStore::displayName(domain_id categoryId) const {
    auto category = value(categoryId);
    if (category) {
        auto name = category->name.toString();
        if (category->parentId.has_value()) {
            return displayName(category->parentId.value()) + "\u25ba" + name;
        }
        return name;
    }
    else qCDebug(logger, "displayName: category not loaded: %lld", categoryId);
    return "";
}

bool CategoryStore::movable(domain_id categoryId) const {
    auto category = value(categoryId);
    return category->parentId.has_value() || rootIds_.size() > 1;
}

bool CategoryStore::isAncestor(domain_id categoryId, const domain_id parentId) const {
    auto category = value(categoryId);
    if (!category->parentId.has_value()) return false;
    return category->parentId.value() == parentId || isAncestor(category->parentId.value(), parentId);
}

bool CategoryStore::hasChild(domain_id categoryId, const QVariant &name) const {
    auto category = value(categoryId);
    auto lowerName = name.toString().toLower();
    for (const auto childId : category->childIds) {
        if (value(childId)->name.toString().toLower() == lowerName) return true;
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
        dataStore->transactionStore->detailStore.replaceCategory(category->id.value(), destinationId);
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

void CategoryStore::update(const QList<const Category *> &updates, const QList<const Category *> deletes) {
    for (auto category : deletes) rootIds_.remove(category->id.value());
    EntityStore::update(updates, deletes);
    for (auto category : updates) {
        auto id = category->id.value();
        if (!category->parentId.has_value()) rootIds_.insert(id);
        else rootIds_.remove(id);
    }
}

void CategoryStore::setValues(QHash<domain_id, const Category*> values) {
    EntityStore::setValues(values);
    rootIds_.clear();
    for (auto id : ids()) {
        if (!value(id)->parentId.has_value()) rootIds_.insert(id);
    }
}
