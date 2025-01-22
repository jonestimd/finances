#ifndef ENTITYSTORE_H
#define ENTITYSTORE_H

#include "background.h"
#include "comboboxmodel.h"
#include "service/model/bulkupdate.h"
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

template<typename T, class Service, typename... GetAllArgs>
class EntityStore : public AbstractEntityStore {
    QHash<qlonglong, const T*> byId{};
    bool loaded{false};
protected:
    Service *service;

public:
    EntityStore(Service *service) : service{service} {};
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

    const T *value(const QVariant &id) const {
        return byId.value(id.toLongLong());
    }

    bool contains(const QVariant &id) const {
        return byId.contains(id.toLongLong());
    }

    const QList<const T*> values() const {
        return byId.values();
    }

    ComboBoxModel *newComboBoxModel(ComboBoxModel::CreateValue createValue = nullptr) const {
        QList<const NamedEntity*> options;
        for (auto entity : values()) options.append(entity);
        return new ComboBoxModel(options, NamedEntity::getName, createValue);
    }

    bool load(EntityView *view, const QString &statusMessage, GetAllArgs... args, bool reload = false) {
        if (!reload && loaded) return true;
        view->disableUi(statusMessage);
        doInBackground(view->statusBar.parentWidget(), [=, this]() {
            setValues(args..., service->getAll(args...));
            QMetaObject::invokeMethod(view, "removeMessage", statusMessage);
            emit valuesLoaded(ids());
        });
        return false;
    }

    void update(QWidget *source, const QList<T*> updates, const QList<T*> adds, const QList<const T*> deletes) {
        doInBackground(source, [=, this]() {
            auto changes = BulkUpdate{updates, adds, deletes};
            update(service->update(changes, user), deletes);
            emit valuesLoaded(ids());
        });
    }

    template<class Model>
    void update(QWidget *source, Model *model) {
        update(source, model->unsavedChanges(), model->unsavedAdds(), model->unsavedDeletes());
    }

protected:
    virtual void update(const QList<const T*> &updates, const QList<const T*> deletes = QList<const T*>{}) {
        for (auto updated : updates) {
            auto id = updated->id.toLongLong();
            auto oldValue = byId.value(id);
            byId[id] = updated;
            if (oldValue) delete oldValue;
        }
        for (auto entity : deletes) delete byId.take(entity->id.toLongLong());
    }

    virtual void setValues(GetAllArgs... args, QHash<qlonglong, const T*> values) {
        for (auto [id, entity] : values.asKeyValueRange()) {
            auto oldEntity = byId.take(id);
            byId.insert(id, entity);
            if (oldEntity) delete oldEntity;
        }
        this->loaded = true;
    }
};

#endif // ENTITYSTORE_H
