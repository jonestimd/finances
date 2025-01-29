#include "treeview.h"
#include <QPainter>

TreeView::TreeView() {}

void TreeView::drawRow(QPainter *painter, const QStyleOptionViewItem &options, const QModelIndex &index) const {
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
