#include "tableitemdelegate.h"
#include "../finances.h"
#include "../validation/validatorfactory.h"
#include "dateedit.h"
#include "enumcombobox.h"
#include "qcombobox.h"
#include "relationeditor.h"
#include <QCompleter>
#include <QDateEdit>
#include <QItemEditorFactory>
#include <QPainter>
#include <ui/model/comboboxmodel.h>

TableItemDelegate::TableItemDelegate(QObject *parent, QStatusBar *statusBar)
    : QStyledItemDelegate{parent}, statusBar{statusBar}
{}

void TableItemDelegate::initStyleOption(QStyleOptionViewItem *option, const QModelIndex &index) const {
    QStyledItemDelegate::initStyleOption(option, index);
    QVariant highlight = index.data(finances::TextHighlightRole);
    if (highlight.isValid() && highlight.toBool()) {
        auto value = highlight.value<finances::TextHighlight>();
        QBrush brush(option->palette.text());
        if (value & finances::Accent) brush = option->palette.accent();
        if (value & finances::Dimmed) {
            auto color = brush.color();
            brush.setColor(QColor(color.red(), color.green(), color.blue(), 160));
        }
        option->palette.setBrush(QPalette::Text, brush);
        option->palette.setBrush(QPalette::HighlightedText, brush);
    }
    QVariant unsaved = index.data(finances::UnsavedRole);
    if (unsaved.isValid()) {
        if (unsaved.toInt() == finances::AddUpdate) {
            auto brush = option->palette.brush(QPalette::Base).color();
            int h, s, v;
            brush.getHsv(&h, &s, &v);
            if (s < 64) s = 128;
            h = (h + 180) % 360;
            option->backgroundBrush = QColor::fromHsv(h, s, v);
        } else {
            option->font.setStrikeOut(true);
            option->backgroundBrush = option->palette.accent().color().lighter();
        }
    }
    QVariant value = index.data();
    if (value.typeId() == QMetaType::Bool) {
        option->font = finances::iconFont->font();
        option->text = QChar(value.toBool() ? finances::Checked : finances::Unchecked);
        option->displayAlignment = Qt::AlignCenter;
    }
    auto decoration = index.data(Qt::DecorationRole);
    if (!decoration.isNull()) {
        auto icon = decoration.value<finances::FontIcon>();
        option->icon = finances::materialIcon(icon, option->widget->palette().text().color());
    }
}

void TableItemDelegate::paint(QPainter *p, const QStyleOptionViewItem &opt, const QModelIndex &index) const {
    QStyledItemDelegate::paint(p, opt, index);
    auto validationMessage = index.data(finances::ValidationMessageRole);
    if (!validationMessage.isNull()) {
        const QRegion clipRegion = p->hasClipping() ? (p->clipRegion() & opt.rect) : opt.rect;
        QRect rect = clipRegion.boundingRect();
        auto image = QImage(":/images/invalid.svg");
        auto y = rect.y() + (rect.height() - image.rect().height()) / 2;
        auto x = rect.right() - image.width();
        p->drawImage(x, y, image);
    }
}

QWidget *TableItemDelegate::createEditor(QWidget *parent, const QStyleOptionViewItem &option, const QModelIndex &index) const {
    auto data = index.data(Qt::EditRole);
    QWidget *editor;
    if (data.canConvert<const NamedEntity*>()) {
        auto model = index.data(finances::OptionsRole).value<ComboBoxModel*>();
        editor = new RelationEditor(model, parent);
    } else if (data.canConvert<const EnumValue*>()) {
        auto options = index.data(finances::OptionsRole).value<QHash<QString, const EnumValue*>>();
        editor = new EnumComboBox(options, parent);
    } else if (data.metaType().id() == QMetaType::QDate) {
        editor= new DateEdit(parent);
    } else {
        editor = QStyledItemDelegate::createEditor(parent, option, index);
        auto lineEdit = qobject_cast<QLineEdit*>(editor);
        auto validatorFactory = index.data(finances::ValidatorFactoryRole);
        if (validatorFactory.isValid() && lineEdit) {
            auto factory = validatorFactory.value<ValidatorFactory::Factory>();
            if (factory) {
                auto validator = factory(lineEdit, statusBar);
                lineEdit->setValidator(validator);
                connect(lineEdit, &QLineEdit::textChanged, [=]() { lineEdit->style()->unpolish(lineEdit); });
            }
        }
    }
    emit openEditor(editor);
    return editor;
}

bool TableItemDelegate::editorEvent(QEvent *event, QAbstractItemModel *model, const QStyleOptionViewItem &option, const QModelIndex &index) {
    using enum QEvent::Type;
    auto eventType = event->type();
    if (eventType == MouseButtonPress || eventType == MouseButtonDblClick || eventType == KeyPress && index.flags().testFlag(Qt::ItemIsEditable)) {
        auto value = index.data(Qt::EditRole);
        if (value.typeId() == QMetaType::Bool) {
            if (eventType != MouseButtonDblClick) model->setData(index, !value.toBool());
            return true;
        }
    }
    return QStyledItemDelegate::editorEvent(event, model, option, index);
}
