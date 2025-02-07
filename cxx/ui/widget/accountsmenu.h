#ifndef ACCOUNTSMENU_H
#define ACCOUNTSMENU_H

#include "ui/model/accountstore.h"
#include "ui/uicontext.h"
#include <QMenu>

class HideClosedAction;

class AccountsMenu : public QMenu {
    Q_OBJECT
    UiContext *const context;
    QAction *const accountsAction;

public:
    AccountsMenu(UiContext *context);

private:
    AccountStore *store();

    static QAction *insertionPoint(QMenu *menu, const QString &text);
    static void insertByName(QMenu *menu, QAction *action);
    static void insertByName(QMenu *menu, QMenu *submenu);

private Q_SLOTS:
    void updateMenu();
};

#endif // ACCOUNTSMENU_H
