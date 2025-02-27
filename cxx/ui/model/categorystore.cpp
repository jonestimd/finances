#include "categorystore.h"

CategoryStore::CategoryStore(CategoryService *service) : EntityStore(service) {}

const QSet<qlonglong> CategoryStore::rootIds() const {
    return rootIds_;
}

QString CategoryStore::displayName(qlonglong categoryId) const {
    return displayName(value(categoryId));
}

QString CategoryStore::displayName(const Category *category) const {
    if (category) {
        auto name = category->name.toString();
        if (category->parentId.isValid()) {
            return displayName(category->parentId.toLongLong()) + "\u25ba" + name;
        }
        return name;
    }
    else qWarning("displayName: category not loaded");
    return "";
}

bool CategoryStore::movable(qlonglong categoryId) const {
    auto category = value(categoryId);
    return !category->parentId.isNull() || rootIds_.size() > 1;
}

bool CategoryStore::isAncestor(qlonglong categoryId, const QVariant parentId) const {
    auto category = value(categoryId);
    if (category->parentId.isNull()) return false;
    return category->parentId == parentId || isAncestor(category->parentId.toLongLong(), parentId);
}

bool CategoryStore::hasChild(qlonglong categoryId, const QVariant &name) const {
    auto category = value(categoryId);
    auto lowerName = name.toString().toLower();
    for (const auto &childId : category->childIds) {
        if (value(childId.toLongLong())->name.toString().toLower() == lowerName) return true;
    }
    return false;
}

void CategoryStore::setParent(QWidget *source, const Category *category, const QVariant parentId) {
    doInBackground(source, [this, category, parentId] {
        auto categories = service->setParent(category, parentId, user);
        update(categories.values());
        emit valuesLoaded(ids());
    });
}

void CategoryStore::mergeCategories(QWidget *source, const Category *category, const QVariant destinationId) {
    doInBackground(source, [this, category, destinationId] {
        auto categories = service->merge(category, destinationId, user);
        update(categories.values(), QList{category});
        emit valuesLoaded(ids());
    });
}

void CategoryStore::update(const QList<const Category *> &updates, const QList<const Category *> deletes) {
    for (auto category : deletes) rootIds_.remove(category->id.toLongLong());
    EntityStore::update(updates, deletes);
    for (auto category : updates) {
        auto id = category->id.toLongLong();
        if (category->parentId.isNull()) rootIds_.insert(id);
        else rootIds_.remove(id);
    }
}

void CategoryStore::setValues(QHash<qlonglong, const Category*> values) {
    EntityStore::setValues(values);
    rootIds_.clear();
    for (auto id : ids()) {
        if (value(id)->parentId.isNull()) rootIds_.insert(id);
    }
}
