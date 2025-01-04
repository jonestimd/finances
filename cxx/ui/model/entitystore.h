#ifndef ENTITYSTORE_H
#define ENTITYSTORE_H

#include <QHash>
#include <QWidget>

class DataStore;

template<typename T>
class EntityStore
{
    friend DataStore;

    QHash<qlonglong, T> byId{};
    bool loaded{false};

public:
    EntityStore() {};

    const QList<qlonglong> ids() const {
        return byId.keys();
    }

    const qsizetype count() const {
        return byId.size();
    }

    const T value(qlonglong id) const {
        return byId.value(id);
    }

    bool contains(qlonglong id) const {
        return byId.contains(id);
    }

protected:
    virtual void update(const QList<T> &updates, const QList<T> deletes = QList<T>{}) {
        for (auto updated : updates) {
            auto id = updated->id.toLongLong();
            auto oldValue = byId.value(id);
            byId[id] = updated;
            if (oldValue) delete oldValue;
        }
        for (auto i : deletes) delete byId.take(i->id.toLongLong());
    }

    virtual void setValues(QList<T> values) {
        QList<qlonglong> ids;
        for (auto value : values) {
            auto id = value->id.toLongLong();
            auto oldValue = byId.value(id);
            ids.append(id);
            byId.insert(id, value);
            if (oldValue) delete oldValue;
        }
        erase_if(byId, [ids](QHash<qlonglong, T>::iterator i) {
            return !ids.contains(i.key());
        });
        this->loaded = true;
    }
};

#endif // ENTITYSTORE_H
