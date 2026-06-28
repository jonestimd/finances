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
    void valuesLoaded(QList<domain_id> ids);
    void valuesAdded(QList<domain_id> ids);
    void valuesUpdated(QList<domain_id> ids);
    void valuesToBeRemoved(QList<domain_id> ids);
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
    QHash<domain_id, const T*> byId{};
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

    const QList<domain_id> ids() const {
        return byId.keys();
    }

    const qsizetype count() const {
        return byId.size();
    }

    const T *value(domain_id id) const {
        return byId.value(id);
    }

    bool contains(domain_id id) const {
        return byId.contains(id);
    }

    void forEachEntry(std::function<void(domain_id, const T*)> func) const {
        for (auto i = byId.cbegin(); i != byId.cend(); i++) func(i.key(), i.value());
    }

    template<class V>
    void appendValues(QList<const V*> &values, QList<domain_id> excludeIds = QList<domain_id>{}) const {
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
        QList<domain_id> updateIds, addIds;
        for (auto updated : updates) {
            auto id = updated->id.value();
            auto oldValue = byId.value(id);
            byId[id] = updated;
            if (oldValue) {
                updateIds.append(id);
                delete oldValue;
            } else addIds.append(id);
        }
        if (!updateIds.isEmpty()) emit valuesUpdated(updateIds);
        if (!addIds.isEmpty()) emit valuesAdded(addIds);
        if (!deletes.isEmpty()) {
            auto ids = getEntityIds(deletes);
            emit valuesToBeRemoved(ids);
            for (auto entity : deletes) delete byId.take(entity->id.value());
        }
    }

    void removeValues(const QList<domain_id> &ids) {
        for (auto id : ids) {
            auto oldEntity = byId.take(id);
            if (oldEntity) delete oldEntity;
        }
    }

    /**
     * @brief setValues Add/replace entities in the store.
     * @details Replaced entities are deleted.
     */
    virtual void setValues(GetAllArgs... args, const QHash<domain_id, const T*> values) {
        for (auto [id, entity] : values.asKeyValueRange()) {
            auto oldEntity = byId.take(id);
            byId.insert(id, entity);
            if (oldEntity) delete oldEntity;
        }
        this->loaded = true;
    }

    template<class Entity>
    static inline optional_id getRelatedId(const Entity* entity, QVariant Entity::* idField) {
        auto id = entity->*idField;
        return id.isValid() ? id.toLongLong() : optional_id{};
    }

    template<class Entity>
    static inline optional_id getRelatedId(const Entity* entity, optional_id Entity::* idField) {
        return entity->*idField;
    }

    template<class Entity>
    static inline optional_id getRelatedId(const Entity* entity, domain_id Entity::* idField) {
        return entity->*idField;
    }

    template<class IdType>
    bool updateTransactionCounts(const QList<TransactionChange> changes, IdType Transaction::* txField) {
        QSet<domain_id> updateIds;
        for (auto change : changes) {
            auto oldTx = change.oldTransaction;
            auto newTx = change.newTransaction;
            auto oldTxValue = oldTx ? getRelatedId(oldTx, txField) : optional_id{};
            auto newTxValue = newTx ? getRelatedId(newTx, txField) : optional_id{};
            if (oldTxValue.has_value() && oldTxValue != newTxValue) {
                auto refValue = value(oldTxValue.value());
                refValue->transactions = refValue->transactions - 1;
                updateIds.insert(oldTxValue.value());
            }
            if (newTxValue.has_value() && oldTxValue != newTxValue) {
                auto refValue = value(newTxValue.value());
                refValue->transactions = refValue->transactions + 1;
                updateIds.insert(newTxValue.value());
            }
        }
        return !updateIds.isEmpty();
    }

    bool updateDetailCounts(const QList<DetailChange> changes, QVariant TransactionDetail::* detailField) {
        QSet<domain_id> updateIds;
        for (auto change : changes) {
            auto oldDetail = change.oldDetail;
            auto newDetail = change.newDetail;
            auto oldDetailValue = oldDetail ? getRelatedId(oldDetail, detailField) : optional_id{};
            auto newDetailValue = newDetail ? getRelatedId(newDetail, detailField) : optional_id{};
            if (oldDetailValue.has_value() && newDetailValue != oldDetailValue) {
                auto refValue = value(oldDetailValue.value());
                refValue->details = refValue->details.toInt() - 1;
                updateIds.insert(oldDetailValue.value());
            }
            if (newDetailValue.has_value() && newDetailValue != oldDetailValue) {
                auto refValue = value(newDetailValue.value());
                refValue->details = refValue->details.toInt() + 1;
                updateIds.insert(newDetailValue.value());
            }
        }
        return !updateIds.isEmpty();
    }
};

#endif // ENTITYSTORE_H
