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
}
using namespace accountsmenu;

AccountsMenu::AccountsMenu(AccountStore *store)
    : QMenu(tr("&Accounts"))
    , store{store}
{
    accountsLoaded(hideClosedAction->isChecked());
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
    if (next) menu->insertAction(next, action);
    else menu->addAction(action);
}

void AccountsMenu::insertByName(QMenu *menu, QMenu *submenu) {
    auto next = insertionPoint(menu, submenu->title());
    if (next) menu->insertMenu(next, submenu);
    else menu->addMenu(submenu);
}

void AccountsMenu::accountsLoaded(bool hideClosed) {
    clear();
    connect(hideClosedAction, SIGNAL(toggled(bool)), this, SLOT(accountsLoaded(bool)));
    QHash<qlonglong, QMenu*> companyMenus{};
    for (auto account : store->values()) {
        if (hideClosed && account->closed.toBool()) continue;
        if (account->companyId.isNull()) {
            insertByName(this, new QAction(account->name.toString()));
        }
        else {
            QMenu *companyMenu = companyMenus.value(account->companyId.toLongLong());
            if (!companyMenu) {
                auto company = store->companyStore.value(account->companyId);
                companyMenu = new QMenu(company->name.toString());
                companyMenus.insert(company->id.toLongLong(), companyMenu);
                insertByName(this, companyMenu);
            }
            insertByName(companyMenu, new QAction(account->name.toString()));
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
    auto first = actions().constFirst();
    insertAction(first, hideClosedAction);
    insertAction(first, new QAction(tr("&Organize Accounts")));
    insertSeparator(first);
}
