#include "tableitemdelegate.h"
#include "../finances.h"
#include "../validation/factory.h"

TableItemDelegate::TableItemDelegate(QObject *parent, QStatusBar *statusBar)
    : QStyledItemDelegate{parent}, statusBar{statusBar} {}

void TableItemDelegate::initStyleOption(QStyleOptionViewItem *option, const QModelIndex &index) const {
    QStyledItemDelegate::initStyleOption(option, index);
    QVariant highlight = index.data(finances::TextHighlight);
    if (highlight.isValid() && highlight.toBool()) {
        auto brush = option->palette.accent();
        option->palette.setBrush(QPalette::Text, brush);
        option->palette.setBrush(QPalette::HighlightedText, brush);
    }
    QVariant unsaved = index.data(finances::Unsaved);
    if (unsaved.isValid() && unsaved.toBool()) {
        auto brush = option->palette.brush(QPalette::Base).color();
        int h, s, v;
        brush.getHsv(&h, &s, &v);
        if (s < 64) s = 128;
        h = (h + 180) % 360;
        option->backgroundBrush = QColor::fromHsv(h, s, v);
    }
}

QWidget *TableItemDelegate::createEditor(QWidget *parent, const QStyleOptionViewItem &option, const QModelIndex &index) const {
    auto editor = QStyledItemDelegate::createEditor(parent, option, index);
    auto validatorFactory = index.data(finances::ValidatorFactory);
    if (validatorFactory.isValid()) {
        auto lineEdit = qobject_cast<QLineEdit*>(editor);
        if (lineEdit) {
            auto factory = validatorFactory.value<ValidatorFactory*>();
            if (factory) {
                auto validator = factory->validator(index, lineEdit, statusBar);
                lineEdit->setValidator(validator);
                connect(lineEdit, &QLineEdit::textChanged, [=]() { lineEdit->style()->unpolish(lineEdit); });
            }
        }
    }
    return editor;
}

bool TableItemDelegate::editorEvent(QEvent *event, QAbstractItemModel *model, const QStyleOptionViewItem &option, const QModelIndex &index) {
    // TODO handle click on boolean cell?
    return QStyledItemDelegate::editorEvent(event, model, option, index);
}
