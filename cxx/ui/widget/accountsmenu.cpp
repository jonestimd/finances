#include "accountsmenu.h"

namespace accountsmenu {
    class HideClosedAction : public QAction {
    public:
        HideClosedAction() : QAction{tr("&Hide Closed Accounts")} {
            setCheckable(true);
            // setChecked(hide); // TODO load from settings
        }
    };

    Q_GLOBAL_STATIC(HideClosedAction, hideClosedAction);

    class AccountAction : public QAction {
    public:
        AccountAction(UiContext *context, const Account *account) {
            setText(account->name.toString());
            connect(this, &QAction::triggered, this, [=]() {
                context->showTransactions(account->id.toLongLong());
            });
        }
    };
}
using namespace accountsmenu;

AccountsMenu::AccountsMenu(UiContext *context)
    : QMenu(tr("&Accounts"))
    , context{context}
    , accountsAction{context->accountsAction()}
{
    connect(store(), SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(updateMenu()));
    updateMenu();
}

AccountStore *AccountsMenu::store() {
    return context->dataStore->accountStore;
}

QAction *AccountsMenu::insertionPoint(QMenu *menu, const QString &text) {
    auto actions = menu->actions();
    for (auto i = actions.cbegin(); i != actions.cend(); i++) {
        if ((*i)->text() > text) {
            return *i;
        }
    }
    return nullptr;
}

void AccountsMenu::insertByName(QMenu *menu, QAction *action) {
    auto next = insertionPoint(menu, action->text());
    menu->insertAction(next, action);
}

void AccountsMenu::insertByName(QMenu *menu, QMenu *submenu) {
    auto next = insertionPoint(menu, submenu->title());
    menu->insertMenu(next, submenu);
}

void AccountsMenu::updateMenu() {
    auto hideClosed = hideClosedAction->isChecked();
    clear();
    connect(hideClosedAction, SIGNAL(toggled(bool)), this, SLOT(updateMenu()));
    QHash<qlonglong, QMenu*> companyMenus{};
    for (auto account : store()->values()) {
        if (hideClosed && account->closed.toBool()) continue;
        if (account->companyId.isNull()) {
            insertByName(this, new AccountAction(context, account));
        }
        else {
            QMenu *companyMenu = companyMenus.value(account->companyId.toLongLong());
            if (!companyMenu) {
                auto company = store()->companyStore.value(account->companyId);
                companyMenu = new QMenu(company->name.toString());
                companyMenus.insert(company->id.toLongLong(), companyMenu);
                insertByName(this, companyMenu);
            }
            insertByName(companyMenu, new AccountAction(context, account));
        }
    }
    for (auto menu : companyMenus) {
        if (menu->actions().length() == 1) {
            auto action = menu->actions().constFirst();
            action->setText(menu->title() + " \u25b8 " + action->text());
            insertAction(menu->menuAction(), action);
            removeAction(menu->menuAction());
        }
    }
    auto first = isEmpty() ? nullptr : actions().constFirst();
    insertAction(first, hideClosedAction);
    insertAction(first, accountsAction);
    insertSeparator(first);
}
