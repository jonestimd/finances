#ifndef TABLEITEMDELEGATE_H
#define TABLEITEMDELEGATE_H

#include <QStatusBar>
#include <QStyledItemDelegate>

class TableItemDelegate : public QStyledItemDelegate {
    Q_OBJECT
    QStatusBar *statusBar;
public:
    explicit TableItemDelegate(QObject *parent = nullptr, QStatusBar *statusBar = nullptr);

    // QStyledItemDelegate interface
protected:
    void initStyleOption(QStyleOptionViewItem *option, const QModelIndex &index) const override;

    // QAbstractItemDelegate interface
public:
    void paint(QPainter *painter, const QStyleOptionViewItem &option, const QModelIndex &index) const override;
    QWidget *createEditor(QWidget *parent, const QStyleOptionViewItem &option, const QModelIndex &index) const override;
    void updateEditorGeometry(QWidget *editor, const QStyleOptionViewItem &option, const QModelIndex &index) const override;
    bool editorEvent(QEvent *event, QAbstractItemModel *model, const QStyleOptionViewItem &option, const QModelIndex &index) override;

    Q_SIGNAL void openEditor(QWidget *editor) const;
};

#endif // TABLEITEMDELEGATE_H
