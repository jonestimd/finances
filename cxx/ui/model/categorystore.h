#ifndef CATEGORYSTORE_H
#define CATEGORYSTORE_H

#include "entitystore.h"
#include "service/model/category.h"

class CategoryStore : public EntityStore<const Category*> {
    friend DataStore;

    QSet<qlonglong> rootIds_;

public:
    CategoryStore();

    const QSet<qlonglong> rootIds() const;

    QString displayName(qlonglong categoryId) const;
    QString displayName(const Category *category) const;
    bool movable(qlonglong categoryId) const;
    bool isAncestor(qlonglong categoryId, const QVariant parentId) const;

protected:
    virtual void update(const QList<const Category *> &updates, const QList<const Category *> deletes = QList<const Category*>{}) override;
    virtual void setValues(QList<const Category*> values) override;
};

#endif // CATEGORYSTORE_H
