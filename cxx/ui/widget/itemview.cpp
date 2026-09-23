#include "itemview.h"
#include "tableitemdelegate.h"
#include "ui/finances.h"
#include "ui/model/sortfilterproxymodel.h"

void itemview::init(QObject* parent, SortFilterProxyModel* sortModel, QAbstractItemView* itemView, QHeaderView* viewHeader, QStatusBar* statusBar) {
    auto itemDelegate = new TableItemDelegate{parent, statusBar};
    itemView->setProperty("sortingEnabled", true);
    itemView->setModel(sortModel);
    itemView->setItemDelegate(itemDelegate);
    itemView->setAlternatingRowColors(true);

    viewHeader->setSectionsMovable(true);
    viewHeader->setSortIndicatorShown(true);
    viewHeader->setSortIndicator(0, Qt::SortOrder::AscendingOrder);
    finances::setColumnResize(viewHeader);
}

void itemview::init(QObject* parent, SortFilterProxyModel* model, QTableView* itemView, QStatusBar* statusBar) {
    return itemview::init(parent, model, itemView, itemView->horizontalHeader(), statusBar);
}

void itemview::init(QObject* parent, SortFilterProxyModel* model, QTreeView* itemView, QStatusBar* statusBar) {
    return itemview::init(parent, model, itemView, itemView->header(), statusBar);
}
