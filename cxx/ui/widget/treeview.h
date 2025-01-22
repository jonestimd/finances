#ifndef TREEVIEW_H
#define TREEVIEW_H

#include <QTreeView>

class TreeView : public QTreeView {
    Q_OBJECT
    bool childInheritsBackground_{false};

public:
    TreeView();

    bool childInheritsBackground() const;
    void setChildInheritsBackground(bool value);

protected:
    virtual void drawRow(QPainter *painter, const QStyleOptionViewItem &options, const QModelIndex &index) const override;
};

#endif // TREEVIEW_H
