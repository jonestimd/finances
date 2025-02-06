#ifndef ACCOUNTSMENU_H
#define ACCOUNTSMENU_H

#include "ui/model/accountstore.h"
#include <QMenu>

class HideClosedAction;

class AccountsMenu : public QMenu {
    Q_OBJECT
    AccountStore *const store;

public:
    AccountsMenu(AccountStore *store);

private:
    static QAction *insertionPoint(QMenu *menu, const QString &text);
    static void insertByName(QMenu *menu, QAction *action);
    static void insertByName(QMenu *menu, QMenu *submenu);

private Q_SLOTS:
    void accountsLoaded(bool hideClosed);
};

#endif // ACCOUNTSMENU_H
