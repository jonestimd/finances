#ifndef SECURITIESWINDOW_H
#define SECURITIESWINDOW_H

#include "appwindow.h"
#include "ui/model/securitytablemodel.h"
#include "ui/store/transactionstore.h"
#include <QTableView>

class UiContext;

class SecuritiesWindow : public EntityWindow<> {
    Q_OBJECT
    SecurityStore* store;
    StatusMessageStore* messageStore;
    TransactionStore* transactionStore;
    QAction *showSplitsAction{iconAction(finances::ArrowSplit, tr("Stock Splits"), tr("alt+s", "splits"), this, SLOT(showSplits()))};
    QAction *hideZeroAction{iconToggle(finances::HideSource, tr("Hide 0 Shares"), tr("alt+0", "hide 0 shares"), this, SLOT(toggleZeroShares(bool)))};

public:
    SecuritiesWindow(UiContext* context);
    ~SecuritiesWindow();

    SecurityTableModel *model() const;

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void setSecurities(const QList<domain_id> ids);
    void toggleZeroShares(bool hide);
    void transactionsUpdated(const QHash<domain_id, TransactionChange> txChanges, const QHash<domain_id, DetailChange> detailChanges);
    void showSplits();

private:
    bool nonZeroShares(const QModelIndex &sourceIndex) const;

protected:
    void showEvent(QShowEvent *event) override;
};

#endif // SECURITIESWINDOW_H
