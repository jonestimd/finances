#ifndef SORTFILTERPROXYMODEL_H
#define SORTFILTERPROXYMODEL_H

#include <QSortFilterProxyModel>
#include "ui/widget/entityview.h"

class AdapterItemModel;

typedef std::function<bool(const QModelIndex &sourceIndex)> AcceptRow;

class SortFilterProxyModel : public QSortFilterProxyModel {
    friend void EntityView::setModel(AdapterItemModel*);
    QList<AcceptRow> acceptFunctions{};

public:
    explicit SortFilterProxyModel(QObject *parent = nullptr);

    void addFilter(AcceptRow acceptFunction);
    void clearFilters();

private:
    virtual void setSourceModel(QAbstractItemModel*) override;

protected:
    virtual bool filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const override;

    virtual bool lessThan(const QModelIndex &source_left, const QModelIndex &source_right) const override;
};

#endif // SORTFILTERPROXYMODEL_H
