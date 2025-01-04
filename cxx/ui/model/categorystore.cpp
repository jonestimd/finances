#include "categorystore.h"

CategoryStore::CategoryStore() : EntityStore() {}

const QSet<qlonglong> CategoryStore::rootIds() const {
    return rootIds_;
}

QString CategoryStore::displayName(qlonglong categoryId) const {
    return displayName(value(categoryId));
}

QString CategoryStore::displayName(const Category *category) const {
    auto name = category->name.toString();
    if (category->parentId.isValid()) {
        return displayName(category->parentId.toLongLong()) + "\u25ba" + name;
    }
    return name;
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

void CategoryStore::update(const QList<const Category *> &updates, const QList<const Category *> deletes) {
    for (auto category : deletes) rootIds_.remove(category->id.toLongLong());
    EntityStore::update(updates, deletes);
    for (auto category : updates) {
        auto id = category->id.toLongLong();
        if (category->parentId.isNull()) rootIds_.insert(id);
        else rootIds_.remove(id);
    }
}

void CategoryStore::setValues(QList<const Category *> values) {
    EntityStore::setValues(values);
    rootIds_.clear();
    for (auto id : ids()) {
        if (value(id)->parentId.isNull()) rootIds_.insert(id);
    }
}
