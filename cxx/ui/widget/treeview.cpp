#include "treeview.h"
#include <QPainter>

bool TreeView::childInheritsBackground() const {
    return childInheritsBackground_;
}

void TreeView::setChildInheritsBackground(bool value) {
    childInheritsBackground_ = value;
    if (value) setAlternatingRowColors(false);
}

TreeView::TreeView() {
    setUniformRowHeights(true);
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
