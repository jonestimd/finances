#include "accountstore.h"
#include "ui/model/accounttablemodel.h"
#include "ui/widget/statusmessage.h"

Q_STATIC_LOGGING_CATEGORY(logger, "store.account")

AccountStore::AccountStore(ServiceContext *services, StatusMessageStore* messageStore)
    : EntityStore{&services->accountService, messageStore}
    , companyStore{&services->companyService, messageStore} {}

bool AccountStore::load(EntityView *view, bool reload) {
    bool loaded = EntityStore::load(view, QObject::tr(LOADING_ACCOUNTS), reload);
    if (!loaded) companyStore.load(view, QObject::tr(LOADING_COMPANIES), true);
    return loaded;
}

void AccountStore::update(QWidget* source, AccountTableModel* model) {
    update(source, model->unsavedChanges(), model->unsavedAdds(), model->unsavedDeletes());
}

void AccountStore::update(QWidget* source, QList<Account*> updates, const QList<const Account*> adds, const QList<const Account*> deletes) {
    doInBackground(source, tr(SAVING_ACCOUNTS), [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        QHash<domain_id, const Company*> companies;
        auto accounts = service->update(changes, user, companies);
        update(accounts, deletes);
        companyStore.update(companies.values());
        emit valuesLoaded(ids());
        emit companyStore.valuesLoaded(companyStore.ids());
    });
}

QString AccountStore::qualifiedName(domain_id accountId, QChar delimiter) const {
    auto account = value(accountId);
    if (account) {
        auto name = account->name;
        if (!account->companyId.has_value()) return name;
        auto company = companyStore.value(account->companyId.value());
        if (company) return QString{company->name}.append(delimiter).append(name);
        else qCDebug(logger, "qualifiedName: company not loaded: %lld", account->companyId.value());
    }
    else qCDebug(logger, "qualifiedName: account not loaded: %lld", accountId);
    return QString::number(accountId);
}

void AccountStore::transactionsUpdated(const QList<TransactionChange> changes) {
    if (updateTransactionCounts<domain_id>(changes, &Transaction::accountId)) {
        emit valuesLoaded(ids());
    }
}
