#include "accountsmenu.h"
#include "ui/widget/settings.h"
#include "ui/uicontext.h"

namespace accountsmenu {
#   define HIDE_CLOSED_SETTING "hide.closed.accounts"

    class HideClosedAction : public QAction {
    public:
        HideClosedAction() : QAction{tr("Hide Closed Accounts")} {
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
        AccountAction(AccountsMenu *menu, TransactionsWindow *window, const Account *account)
            : QAction(menu)
        {
            setText(account->name.toString().replace('&', "&&"));
            connect(this, &QAction::triggered, this, [=]() {
                window->showAccount(account->id.value());
            });
        }
    };
}
using namespace accountsmenu;

AccountsMenu::AccountsMenu(TransactionsWindow *window, UiContext *context)
    : QMenu(tr("&Accounts"), window)
    , window{window}
    , store{context->dataStore->accountStore}
    , accountsAction{context->accountsAction()}
{
    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(updateMenu()));
    connect(&store->companyStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(updateMenu()));
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
    QHash<domain_id, QMenu*> companyMenus{};
    store->forEachEntry([&](domain_id id, const Account* account) {
        if (hideClosed && account->closed) return;
        if (!account->companyId.has_value()) {
            insertByName(this, new AccountAction(this, window, account));
        } else {
            QMenu *companyMenu = companyMenus.value(account->companyId.value());
            if (!companyMenu) {
                auto company = store->companyStore.value(account->companyId.value());
                if (company) {
                    companyMenu = new QMenu(company->name.toString().replace('&', "&&"), this);
                    companyMenus.insert(company->id.value(), companyMenu);
                    insertByName(this, companyMenu);
                }
            }
            if (companyMenu) insertByName(companyMenu, new AccountAction(this, window, account));
        }
    });
    for (auto menu : companyMenus) {
        if (menu->actions().length() == 1) {
            auto action = menu->actions().constFirst();
            action->setText(menu->title() + " \u25b8 " + action->text());
            insertAction(menu->menuAction(), action);
            removeAction(menu->menuAction());
        } else {
            QList<QChar> mnemonics{};
            const auto &actions = menu->actions();
            for (auto action : actions) {
                QString text = action->text();
                for (auto i = 0; i < text.length(); i++) {
                    if (!mnemonics.contains(text.at(i).toLower())) {
                        mnemonics.append(text.at(i).toLower());
                        action->setText(text.insert(i, '&'));
                        break;
                    }
                }
            }
        }
    }
    auto first = isEmpty() ? nullptr : actions().constFirst();
    insertAction(first, hideClosedAction);
    insertAction(first, accountsAction);
    insertSeparator(first);
}

void AccountsMenu::keyPressEvent(QKeyEvent *event) {
    QMenu::keyPressEvent(event);
    if (!event->isAccepted()) {
        searchBuffer += event->text();
        searchBufferTimer.start(1000, this);
        const QList<QAction*> &items = actions();
        for (int i = 2, end = items.count(); i < end; i++) {
            if (items.at(i)->text().startsWith(searchBuffer, Qt::CaseInsensitive)) {
                setActiveAction(items.at(i));
                event->accept();
                break;
            }
        }
    }
}

void AccountsMenu::timerEvent(QTimerEvent *event) {
    if (event->timerId() == searchBufferTimer.timerId()) searchBuffer.clear();
    else QMenu::timerEvent(event);
}
