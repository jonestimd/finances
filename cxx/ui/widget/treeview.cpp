#include "treeview.h"
#include <QPainter>
#include <QKeyEvent>
#include <QSortFilterProxyModel>

bool TreeView::childInheritsBackground() const {
    return childInheritsBackground_;
}

void TreeView::setChildInheritsBackground(bool value) {
    childInheritsBackground_ = value;
    if (value) setAlternatingRowColors(false);
}

TreeView::TreeView() {
    setUniformRowHeights(true);
    setTabKeyNavigation(true);
}

void TreeView::drawRow(QPainter *painter, const QStyleOptionViewItem &options, const QModelIndex &index) const {
    if (childInheritsBackground_) {
        auto rootIndex = index;
        while (rootIndex.parent().isValid()) {
            rootIndex = rootIndex.parent();
        }
        if (rootIndex.row() & 1) {
            auto background = palette().color(QPalette::AlternateBase);
            painter->fillRect(options.rect, background);
        }
    }
    QTreeView::drawRow(painter, options, index);
    auto gridColor = style()->styleHint(QStyle::SH_Table_GridLineColor, nullptr, this);
    if (gridColor == 0) gridColor = palette().color(QPalette::Dark).rgba();
    auto rect = options.rect;
    painter->setPen(QColor::fromRgb(gridColor));
    painter->drawLine(rect.bottomLeft(), rect.bottomRight());
    auto columns = model()->columnCount(index.parent());
    for (int i = 0; i < columns; i++) {
        auto rect = this->visualRect(index.siblingAtColumn(i));
        painter->drawLine(rect.bottomRight(), rect.topRight());
    }
}

static QModelIndex nextCell(const QAbstractItemModel *model, QModelIndex current, int endColumn) {
    auto column = current.column();
    if (column < endColumn) return current.siblingAtColumn(column+1);
    if (model->hasChildren(current)) return model->index(0, 0, current.siblingAtColumn(0));
    while (current.isValid()) {
        auto nextRow = current.row() + 1;
        if (nextRow < model->rowCount(current.parent())) return current.sibling(nextRow, 0);
        current = current.parent();
    }
    return QModelIndex{};
}

static QModelIndex previousRow(const QAbstractItemModel *model, const QModelIndex &index, int endColumn) {
    auto sibling = index.sibling(index.row()-1, 0);
    while (model->rowCount(sibling) > 0) {
        auto lastChild = model->rowCount(sibling) - 1;
        sibling = model->index(lastChild, 0, sibling);
    }
    return sibling.siblingAtColumn(endColumn);
}

static QModelIndex previousCell(const QAbstractItemModel *model, const QModelIndex &current, int endColumn) {
    auto column = current.column();
    if (column > 0) return current.siblingAtColumn(column-1);
    if (current.row() > 0) return previousRow(model, current, endColumn);
    auto parent = current.parent();
    if (parent.isValid()) return model->index(parent.row(), endColumn, parent.parent());
    return QModelIndex{};
}

QModelIndex TreeView::moveCursor(CursorAction cursorAction, Qt::KeyboardModifiers modifiers) {
    if (modifiers == Qt::NoModifier) {
        auto current = currentIndex();
        auto column = current.column();
        auto endColumn = model()->columnCount(current)-1;
        switch(cursorAction) {
        case QAbstractItemView::MoveLeft:
            if (column > 0 || !itemsExpandable() || !isExpanded(current.siblingAtColumn(0)) || !model()->hasChildren(current)) {
                if (column > 0) return current.siblingAtColumn(column-1);
                else return current.siblingAtColumn(endColumn);
            }
            break;
        case QAbstractItemView::MoveRight:
            if (column > 0 || isExpanded(current.siblingAtColumn(0)) || !model()->hasChildren(current)) {
                if (column < endColumn) return current.siblingAtColumn(column+1);
                return current.siblingAtColumn(0);
            }
            break;
        case QAbstractItemView::MoveHome:
            return current.siblingAtColumn(0);
        case QAbstractItemView::MoveEnd:
            return current.siblingAtColumn(endColumn);
        case QAbstractItemView::MoveNext:
            while ((current = nextCell(model(), current, endColumn)).isValid()) {
                if ((current.flags() & Qt::ItemIsEditable)) return current;
            }
            break;
        case QAbstractItemView::MovePrevious:
            while ((current = previousCell(model(), current, endColumn)).isValid()) {
                if ((current.flags() & Qt::ItemIsEditable)) return current;
            }
        default:
            break;
        }
    }
    return QTreeView::moveCursor(cursorAction, modifiers);
}
