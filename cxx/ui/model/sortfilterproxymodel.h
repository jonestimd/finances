#ifndef SORTFILTERPROXYMODEL_H
#define SORTFILTERPROXYMODEL_H

#include <QSortFilterProxyModel>

class AdapterItemModel;
class EntityView;

typedef std::function<bool(const QModelIndex &sourceIndex)> AcceptRow;

class SortFilterProxyModel : public QSortFilterProxyModel {
    friend class EntityView;
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
