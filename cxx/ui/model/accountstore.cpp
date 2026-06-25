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
    messageStore->addMessage(tr(SAVING_ACCOUNTS));
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        QHash<qlonglong, const Company*> companies;
        auto accounts = service->update(changes, user, companies);
        update(accounts, deletes);
        companyStore.update(companies.values());
        emit valuesLoaded(ids());
        emit companyStore.valuesLoaded(companyStore.ids());
        QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, tr(SAVING_ACCOUNTS));
    });
}

QString AccountStore::qualifiedName(qlonglong accountId, QChar delimiter) const {
    auto account = value(accountId);
    if (account) {
        auto name = account->name.toString();
        if (account->companyId.isNull()) return name;
        auto company = companyStore.value(account->companyId.toLongLong());
        if (company) return company->name.toString().append(delimiter).append(name);
        else qCDebug(logger, "qualifiedName: company not loaded: %lld", account->companyId.toLongLong());
    }
    else qCDebug(logger, "qualifiedName: account not loaded: %lld", accountId);
    return QString::number(accountId);
}

void AccountStore::transactionsUpdated(const QList<TransactionChange> changes) {
    if (updateTransactionCounts<qlonglong>(changes, &Transaction::accountId)) {
        emit valuesLoaded(ids());
    }
}
