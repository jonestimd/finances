#ifndef SORTFILTERPROXYMODEL_H
#define SORTFILTERPROXYMODEL_H

#include <QSortFilterProxyModel>

typedef std::function<bool(const QModelIndex &sourceIndex)> AcceptRow;

class SortFilterProxyModel : public QSortFilterProxyModel {
    QList<AcceptRow> acceptFunctions{};

public:
    explicit SortFilterProxyModel(QObject *parent = nullptr);

    void addFilter(AcceptRow acceptFunction);
    void clearFilters();

protected:
    virtual bool filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const override;
};

#endif // SORTFILTERPROXYMODEL_H
