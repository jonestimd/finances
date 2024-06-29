#ifndef TABLEITEMDELEGATE_H
#define TABLEITEMDELEGATE_H

#include <QStatusBar>
#include <QStyledItemDelegate>

class TableItemDelegate : public QStyledItemDelegate {
    QStatusBar *statusBar;
public:
    explicit TableItemDelegate(QObject *parent = nullptr, QStatusBar *statusBar = nullptr);

    // QStyledItemDelegate interface
protected:
    void initStyleOption(QStyleOptionViewItem *option, const QModelIndex &index) const override;

    // Q_SLOT void setInvalid(QString &text);

    // QAbstractItemDelegate interface
public:
    QWidget *createEditor(QWidget *parent, const QStyleOptionViewItem &option, const QModelIndex &index) const override;
    bool editorEvent(QEvent *event, QAbstractItemModel *model, const QStyleOptionViewItem &option, const QModelIndex &index) override;
};

#endif // TABLEITEMDELEGATE_H
