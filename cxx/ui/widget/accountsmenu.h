#ifndef ACCOUNTSMENU_H
#define ACCOUNTSMENU_H

#include "ui/model/accountstore.h"
#include "transactionswindow.h"
#include <QBasicTimer>
#include <QMenu>

class HideClosedAction;

class AccountsMenu : public QMenu {
    Q_OBJECT
    TransactionsWindow *const window;
    AccountStore *const store;
    QAction *const accountsAction;
    QString searchBuffer;
    QBasicTimer searchBufferTimer;

public:
    AccountsMenu(TransactionsWindow *window, UiContext *context);

private:
    static QAction *insertionPoint(QMenu *menu, const QString &text);
    static void insertByName(QMenu *menu, QAction *action);
    static void insertByName(QMenu *menu, QMenu *submenu);

private Q_SLOTS:
    void updateMenu();

protected:
    virtual void keyPressEvent(QKeyEvent *event) override;
    virtual void timerEvent(QTimerEvent *event) override;
};

#endif // ACCOUNTSMENU_H
