#ifndef SECURITIESWINDOW_H
#define SECURITIESWINDOW_H

#include "appwindow.h"
#include "ui/model/datastore.h"
#include "ui/model/securitytablemodel.h"
#include <QTableView>

class SecuritiesWindow : public AppWindow {
    Q_OBJECT
    SecurityStore *store;
    QAction *hideZeroAction{finances::iconToggle(finances::HideSource, tr("Hide 0 Shares"), tr("alt+0", "hide 0 shares"), this, SLOT(toggleZeroShares(bool)))};

public:
    SecuritiesWindow(DataStore *dataStore);

    SecurityTableModel *model() const;

    Q_INVOKABLE void loadData() override;
    Q_INVOKABLE void saveData() override;

public Q_SLOTS:
    void setSecurities(const QList<qlonglong> ids);
    void toggleZeroShares(bool hide);

private:
    bool nonZeroShares(const QModelIndex &sourceIndex) const;
};

#endif // SECURITIESWINDOW_H
