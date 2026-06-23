#ifndef ENTITYSTORE_H
#define ENTITYSTORE_H

#include "background.h"
#include "comboboxmodel.h"
#include "service/model/bulkupdate.h"
#include "service/model/transaction.h"
#include "statusmessagestore.h"
#include "ui/widget/entityview.h"
#include <QHash>
#include <QWidget>

class DataStore;

class AbstractEntityStore : public QObject {
    Q_OBJECT

protected:
    static const QString user;

public:
    AbstractEntityStore(QObject *parent = nullptr);

Q_SIGNALS:
    void valuesLoaded(QList<qlonglong> ids);
};

/**
 * @brief Template for a class that stores database entities (rows).
 * @details The `EntityStore` assumes ownership of the database entities and is responsible
 * for deleting them.
 * @tparam T The class that represents a row in the database table.
 * @tparam Service the class that provides access to the database table.
 * The class must provide the following methods:
 *   - `getAll`: retrieves entities from the database
 *   - `update`: saves changes to the database
 * @tparam GetAllArgs Types for the arguments used to retrieve data for a UI view (usually empty).
 */
template<typename T, class Service, typename... GetAllArgs>
class EntityStore : public AbstractEntityStore {
    QHash<qlonglong, const T*> byId{};
    bool loaded{false};
protected:
    Service* const service;
    StatusMessageStore* const messageStore;

public:
    EntityStore(Service *service, StatusMessageStore* messageStore) : service{service}, messageStore{messageStore} {};
    ~EntityStore() {
        qDeleteAll(byId);
        byId.clear();
    }

    const QList<qlonglong> ids() const {
        return byId.keys();
    }

    const qsizetype count() const {
        return byId.size();
    }

    const T *value(qlonglong id) const {
        return byId.value(id);
    }

    bool contains(qlonglong id) const {
        return byId.contains(id);
    }

    void forEachEntry(std::function<void(qlonglong, const T*)> func) const {
        for (auto i = byId.cbegin(); i != byId.cend(); i++) func(i.key(), i.value());
    }

    template<class V>
    void appendValues(QList<const V*> &values, QList<qlonglong> excludeIds = QList<qlonglong>{}) const {
        for (auto i = byId.cbegin(); i != byId.cend(); i++) {
            if (!excludeIds.contains(i.key())) values.append(i.value());
        }
    }

    ComboBoxModel *newComboBoxModel(ComboBoxModel::CreateValue createValue = nullptr) const {
        QList<const NamedEntity*> options;
        for (auto i = byId.cbegin(); i != byId.cend(); i++) options.append(i.value());
        return new ComboBoxModel(options, NamedEntity::getName, createValue);
    }

    /**
     * @return Returns `true` if the data is already loaded or `false` if the data is loading in the background.
     */
    bool load(EntityView *view, const QString &statusMessage, GetAllArgs... args, bool reload = false) {
        if (!reload && loaded) return true;
        messageStore->addMessage(statusMessage);
        doInBackground(view->statusBar.parentWidget(), [=, this]() {
            setValues(args..., service->getAll(args...));
            emit valuesLoaded(ids());
            QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, statusMessage);
        });
        return false;
    }

    /**
     * @brief update Call service to persist changes and update stored entities.
     */
    void update(QWidget *source, const QList<T*> updates, const QList<const T*> adds, const QList<const T*> deletes, const QString message) {
        messageStore->addMessage(message);
        doInBackground(source, [=, this]() {
            auto changes = BulkUpdate{updates, adds, deletes};
            update(service->update(changes, user), deletes);
            emit valuesLoaded(ids());
            QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, message);
        });
    }

    template<class Model>
    void update(QWidget *source, Model *model, const QString message) {
        update(source, model->unsavedChanges(), model->unsavedAdds(), model->unsavedDeletes(), message);
    }

protected:
    /**
     * @brief update Update entities with persisted changes from service.
     * @param updates Added/updated entities.  Replaced entities are deleted.
     * @param deletes Entities that have been removed from database.
     */
    virtual void update(const QList<const T*> &updates, const QList<const T*> deletes = QList<const T*>{}) {
        for (auto updated : updates) {
            auto id = updated->id.value();
            auto oldValue = byId.value(id);
            byId[id] = updated;
            if (oldValue) delete oldValue;
        }
        for (auto entity : deletes) delete byId.take(entity->id.value());
    }

    void removeValues(const QList<qlonglong> &ids) {
        for (auto id : ids) {
            auto oldEntity = byId.take(id);
            if (oldEntity) delete oldEntity;
        }
    }

    /**
     * @brief setValues Add/replace entities in the store.
     * @details Replaced entities are deleted.
     */
    virtual void setValues(GetAllArgs... args, const QHash<qlonglong, const T*> values) {
        for (auto [id, entity] : values.asKeyValueRange()) {
            auto oldEntity = byId.take(id);
            byId.insert(id, entity);
            if (oldEntity) delete oldEntity;
        }
        this->loaded = true;
    }

    bool updateTransactionCounts(const QList<TransactionChange> changes, QVariant Transaction::* txField) {
        QSet<qlonglong> updateIds;
        for (auto change : changes) {
            auto oldTx = change.oldTransaction;
            auto newTx = change.newTransaction;
            auto oldTxValue = oldTx ? oldTx->*txField : QVariant{};
            auto newTxValue = newTx ? newTx->*txField : QVariant{};
            if (oldTxValue.isValid() && newTxValue != oldTxValue) {
                auto refValue = value(oldTxValue.toLongLong());
                refValue->transactions = refValue->transactions.toInt() - 1;
                updateIds.insert(oldTxValue.toLongLong());
            }
            if (newTxValue.isValid() && newTxValue != oldTxValue) {
                auto refValue = value(newTxValue.toLongLong());
                refValue->transactions = refValue->transactions.toInt() + 1;
                updateIds.insert(newTxValue.toLongLong());
            }
        }
        return !updateIds.isEmpty();
    }

    bool updateDetailCounts(const QList<DetailChange> changes, QVariant TransactionDetail::* detailField) {
        QSet<qlonglong> updateIds;
        for (auto change : changes) {
            auto oldDetail = change.oldDetail;
            auto newDetail = change.newDetail;
            auto oldDetailValue = oldDetail ? oldDetail->*detailField : QVariant{};
            auto newDetailValue = newDetail ? newDetail->*detailField : QVariant{};
            if (oldDetailValue.isValid() && newDetailValue != oldDetailValue) {
                auto refValue = value(oldDetailValue.toLongLong());
                refValue->details = refValue->details.toInt() - 1;
                updateIds.insert(oldDetailValue.toLongLong());
            }
            if (newDetailValue.isValid() && newDetailValue != oldDetailValue) {
                auto refValue = value(newDetailValue.toLongLong());
                refValue->details = refValue->details.toInt() + 1;
                updateIds.insert(newDetailValue.toLongLong());
            }
        }
        return !updateIds.isEmpty();
    }
};

#endif // ENTITYSTORE_H
