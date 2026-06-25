#ifndef CATEGORYSTORE_H
#define CATEGORYSTORE_H

#include "entitystore.h"
#include "service/model/category.h"
#include "service/categoryservice.h"

class CategoryStore : public EntityStore<Category, CategoryService> {
    Q_OBJECT

    DataStore* const dataStore;
    QSet<qlonglong> rootIds_;

public:
    CategoryStore(CategoryService *service, DataStore* dataStore);

    const QSet<qlonglong> rootIds() const;

    QString displayName(qlonglong categoryId) const;
    bool movable(qlonglong categoryId) const;
    /**
     * @return `true` if `parentId` is an ancestor of `categoryId`.
     */
    bool isAncestor(qlonglong categoryId, const qlonglong parentId) const;
    bool hasChild(qlonglong categoryId, const QVariant &name) const;

    void setParent(QWidget *source, const Category *category, const std::optional<qlonglong> &parentId);
    void mergeCategories(QWidget *source, const Category *category, const qlonglong destinationId);

    using EntityStore::update;

public slots:
    void detailsUpdated(const QList<DetailChange> changes);

protected:
    virtual void update(const QList<const Category *> &updates, const QList<const Category *> deletes = QList<const Category*>{}) override;
    virtual void setValues(QHash<qlonglong, const Category*> values) override;
};

#endif // CATEGORYSTORE_H
