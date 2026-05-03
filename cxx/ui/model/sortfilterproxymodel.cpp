#include "sortfilterproxymodel.h"

SortFilterProxyModel::SortFilterProxyModel(QObject *parent) : QSortFilterProxyModel{parent} {}

void SortFilterProxyModel::addFilter(AcceptRow acceptFunction) {
    beginFilterChange();
    acceptFunctions.append(acceptFunction);
    endFilterChange(Direction::Columns);
}

void SortFilterProxyModel::clearFilters() {
    beginFilterChange();
    acceptFunctions.clear();
    endFilterChange(Direction::Columns);
}

bool SortFilterProxyModel::filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const {
    for (const auto &accept : acceptFunctions) {
        if (!accept(sourceModel()->index(sourceRow, 0, sourceParent))) return false;
    }
    return QSortFilterProxyModel::filterAcceptsRow(sourceRow, sourceParent);
}
