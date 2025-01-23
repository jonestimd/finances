#ifndef SECURITIESWINDOW_H
#define SECURITIESWINDOW_H

#include "entityview.h"
#include "statuswindow.h"
#include "ui/model/datastore.h"
#include "ui/model/securitytablemodel.h"
#include <QTableView>

class SecuritiesWindow : public StatusWindow {
    Q_OBJECT
    SecurityStore *store;
    SecurityTableModel model;
    QTableView *itemView{new QTableView(this)};
    QAction *hideZeroAction{finances::iconToggle(finances::HideSource, tr("Hide 0 Shares"), tr("alt+0", "hide 0 shares"), this, SLOT(toggleZeroShares(bool)))};
    EntityView tableSort;

public:
    SecuritiesWindow(DataStore *dataStore);

public Q_SLOTS:
    void loadSecurities();
    void saveSecurities();
    void setSecurities(const QList<qlonglong> ids);
    void toggleZeroShares(bool hide);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);

private:
    bool nonZeroShares(const QModelIndex &sourceIndex) const;
};

#endif // SECURITIESWINDOW_H
