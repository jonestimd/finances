#ifndef ENTITYSTORE_H
#define ENTITYSTORE_H

#include "background.h"
#include "service/model/bulkupdate.h"
#include <QHash>
#include <QWidget>

class DataStore;

class EntityStoreSignals : public QObject {
    Q_OBJECT

protected:
    static const QString user;

public:
    EntityStoreSignals(QObject *parent = nullptr);

Q_SIGNALS:
    void valuesLoaded(QList<qlonglong> ids);
};

template<typename T, class Service>
class EntityStore : public EntityStoreSignals {
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

    const T *value(qlonglong id) const {
        return byId.value(id);
    }

    bool contains(qlonglong id) const {
        return byId.contains(id);
    }

    bool load(QWidget *source, bool reload = false) {
        if (!reload && loaded) return true;
        doInBackground(source, [=, this]() {
            setValues(service->getAll());
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
        for (auto i : deletes) delete byId.take(i->id.toLongLong());
    }

    virtual void setValues(QHash<qlonglong, const T*> values) {
        auto oldValues = byId;
        byId = values;
        qDeleteAll(oldValues);
        this->loaded = true;
    }
};

#endif // ENTITYSTORE_H
