#include "../finances.h"
#include "tableitemdelegate.h"

TableItemDelegate::TableItemDelegate(QObject *parent)
    : QStyledItemDelegate{parent}
{}

void TableItemDelegate::initStyleOption(QStyleOptionViewItem *option, const QModelIndex &index) const {
    QStyledItemDelegate::initStyleOption(option, index);
    QVariant highlight = index.data(finances::TextHighlight);
    if (highlight.isValid() && highlight.toBool()) {
        auto brush = option->palette.accent();
        option->palette.setBrush(QPalette::Text, brush);
        option->palette.setBrush(QPalette::HighlightedText, brush);
    }
}
