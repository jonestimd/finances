#ifndef ITEM_VIEW_H
#define ITEM_VIEW_H

#include <QAbstractItemView>
#include <QHeaderView>
#include <QStatusBar>
#include <QTableView>
#include <QTreeView>

class SortFilterProxyModel;

namespace itemview {
    /**
     * @brief init Initializes the view with a `SortFilterProxyModel` and `TableItemDelegate`.
     * @param parent `QObject` that owns the sort model and table item delegate.
     * @param model The source model for the `SortFilterProxyModel`.
     */
    void init(QObject* parent, SortFilterProxyModel* model, QAbstractItemView* itemView, QHeaderView* viewHeader, QStatusBar* statusBar);

    void init(QObject* parent, SortFilterProxyModel* model, QTableView* itemView, QStatusBar* statusBar);
    void init(QObject* parent, SortFilterProxyModel* model, QTreeView* itemView, QStatusBar* statusBar);
}

#endif // ITEM_VIEW_H
