#include "treeview.h"
#include <QPainter>
#include <QKeyEvent>
#include <QSortFilterProxyModel>
#include <QHeaderView>

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

QModelIndex TreeView::nextCell(const QModelIndex &current) const {
    auto next = nextColumn(current);
    if (next.column() > current.column()) return next;
    auto model = this->model();
    auto endColumn = model->columnCount(current) - 1;
    next = current.siblingAtColumn(0);
    if (model->hasChildren(next)) return nextColumn(model->index(0, endColumn, next));
    for (next = current; next.isValid(); next = next.parent()) {
        auto nextRow = next.row() + 1;
        if (nextRow < model->rowCount(next.parent())) return nextColumn(next.sibling(nextRow, endColumn));
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

QModelIndex TreeView::previousCell(const QModelIndex &current) const {
    auto next = previousColumn(current);
    if (next.column() < current.column()) return next;
    auto model = this->model();
    auto endColumn = model->columnCount(current)-1;
    if (current.row() > 0) return previousRow(model, current, endColumn);
    auto parent = current.parent();
    if (parent.isValid()) return model->index(parent.row(), endColumn, parent.parent());
    return QModelIndex{};
}

QModelIndex TreeView::moveCursor(CursorAction cursorAction, Qt::KeyboardModifiers modifiers) {
    if (modifiers == Qt::NoModifier) {
        auto current = currentIndex();
        auto column = current.column();
        switch(cursorAction) {
        case QAbstractItemView::MoveLeft:
            if (column > 0 || !itemsExpandable() || !isExpanded(current.siblingAtColumn(0)) || !model()->hasChildren(current)) {
                return previousColumn(current);
            }
            break;
        case QAbstractItemView::MoveRight:
            if (column > 0 || isExpanded(current.siblingAtColumn(0)) || !model()->hasChildren(current)) {
                return nextColumn(current);
            }
            break;
        case QAbstractItemView::MoveHome:
            return current.siblingAtColumn(0);
        case QAbstractItemView::MoveEnd:
            return current.siblingAtColumn(model()->columnCount(current)-1);
        case QAbstractItemView::MoveNext:
            while ((current = nextCell(current)).isValid()) {
                if (current.flags() & Qt::ItemIsEditable) return current;
            }
            break;
        case QAbstractItemView::MovePrevious:
            while ((current = previousCell(current)).isValid()) {
                if ((current.flags() & Qt::ItemIsEditable)) return current;
            }
        default:
            break;
        }
    }
    return QTreeView::moveCursor(cursorAction, modifiers);
}

bool TreeView::isHidden(const QModelIndex &index) const {
    return header()->isSectionHidden(index.column());
}

QModelIndex TreeView::nextColumn(QModelIndex index) const {
    do {
        if (index.column() < model()->columnCount(index)-1) index = index.siblingAtColumn(index.column()+1);
        else index = index.siblingAtColumn(0);
    } while (isHidden(index));
    return index;
}

QModelIndex TreeView::previousColumn(QModelIndex index) const {
    do {
        if (index.column() > 0) index = index.siblingAtColumn(index.column()-1);
        else index = index.siblingAtColumn(model()->columnCount(index)-1);
    } while (isHidden(index));
    return index;
}
