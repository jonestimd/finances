#ifndef CATEGORYSTORE_H
#define CATEGORYSTORE_H

#include "entitystore.h"
#include "service/model/category.h"
#include "service/categoryservice.h"

class CategoryStore : public EntityStore<Category, CategoryService> {
    Q_OBJECT

    DataStore* const dataStore;

public:
    CategoryStore(CategoryService *service, DataStore* dataStore);

    const QSet<domain_id> rootIds() const;

    QString displayName(domain_id categoryId) const;
    /**
     * @return `true` if `parentId` is an ancestor of `categoryId`.
     */
    bool isAncestor(domain_id categoryId, const domain_id parentId) const;
    bool hasChild(domain_id categoryId, const QString &name) const;

    void setParent(QWidget *source, const Category *category, const optional_id &parentId);
    void mergeCategories(QWidget *source, const Category *category, const domain_id destinationId);

    using EntityStore::update;

public slots:
    void detailsUpdated(const QList<DetailChange> changes);
};

#endif // CATEGORYSTORE_H
