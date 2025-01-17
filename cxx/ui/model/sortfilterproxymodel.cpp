#include "sortfilterproxymodel.h"

SortFilterProxyModel::SortFilterProxyModel(QObject *parent) : QSortFilterProxyModel{parent} {}

void SortFilterProxyModel::addFilter(AcceptRow acceptFunction) {
    acceptFunctions.append(acceptFunction);
    invalidateRowsFilter();
}

void SortFilterProxyModel::clearFilters() {
    acceptFunctions.clear();
    invalidateRowsFilter();
}

bool SortFilterProxyModel::filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const {
    for (auto accept : acceptFunctions) {
        if (!accept(sourceModel()->index(sourceRow, 0, sourceParent))) return false;
    }
    return QSortFilterProxyModel::filterAcceptsRow(sourceRow, sourceParent);
}
