#include "accountsmenu.h"
#include "ui/widget/settings.h"
#include "ui/uicontext.h"

namespace accountsmenu {
#   define HIDE_CLOSED_SETTING "hide.closed.accounts"

    class HideClosedAction : public QAction {
    public:
        HideClosedAction() : QAction{tr("&Hide Closed Accounts")} {
            setCheckable(true);
            setChecked(settings::getValue(HIDE_CLOSED_SETTING).toBool());
            connect(this, &QAction::triggered, this, [this]() {
                settings::saveValue(HIDE_CLOSED_SETTING, isChecked());
            });
        }
    };

    Q_GLOBAL_STATIC(HideClosedAction, hideClosedAction);

    class AccountAction : public QAction {
    public:
        AccountAction(TransactionsWindow *window, const Account *account) {
            setText(account->name.toString());
            connect(this, &QAction::triggered, this, [=]() {
                window->showAccount(account->id.toLongLong());
            });
        }
    };
}
using namespace accountsmenu;

AccountsMenu::AccountsMenu(TransactionsWindow *window, UiContext *context)
    : QMenu(tr("&Accounts"))
    , window{window}
    , store{context->dataStore->accountStore}
    , accountsAction{context->accountsAction()}
{
    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(updateMenu()));
    updateMenu();
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
    for (auto account : store->values()) {
        if (hideClosed && account->closed.toBool()) continue;
        if (account->companyId.isNull()) {
            insertByName(this, new AccountAction(window, account));
        }
        else {
            QMenu *companyMenu = companyMenus.value(account->companyId.toLongLong());
            if (!companyMenu) {
                auto company = store->companyStore.value(account->companyId);
                companyMenu = new QMenu(company->name.toString());
                companyMenus.insert(company->id.toLongLong(), companyMenu);
                insertByName(this, companyMenu);
            }
            insertByName(companyMenu, new AccountAction(window, account));
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
