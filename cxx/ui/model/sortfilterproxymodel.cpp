#include "sortfilterproxymodel.h"
#include "ui/finances.h"

SortFilterProxyModel::SortFilterProxyModel(QObject *parent) : QSortFilterProxyModel{parent} {}

void SortFilterProxyModel::addFilter(AcceptRow acceptFunction) {
    beginFilterChange();
    acceptFunctions.append(acceptFunction);
    endFilterChange(Direction::Rows);
}

void SortFilterProxyModel::clearFilters() {
    beginFilterChange();
    acceptFunctions.clear();
    endFilterChange(Direction::Rows);
}

void SortFilterProxyModel::setSourceModel(QAbstractItemModel* model) {
    QSortFilterProxyModel::setSourceModel(model);
}

bool SortFilterProxyModel::filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const {
    for (const auto &accept : acceptFunctions) {
        if (!accept(sourceModel()->index(sourceRow, 0, sourceParent))) return false;
    }
    return QSortFilterProxyModel::filterAcceptsRow(sourceRow, sourceParent);
}

bool SortFilterProxyModel::lessThan(const QModelIndex &source_left, const QModelIndex &source_right) const {
    if (source_left.data(finances::UnsavedRole) == finances::Add) {
        if (source_right.data(finances::UnsavedRole) != finances::Add) return sortOrder() == Qt::DescendingOrder;
    } else if (source_right.data(finances::UnsavedRole) == finances::Add) return sortOrder() == Qt::AscendingOrder;
    return QSortFilterProxyModel::lessThan(source_left, source_right);
}
