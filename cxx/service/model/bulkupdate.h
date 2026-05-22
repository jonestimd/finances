#ifndef BULKUPDATE_H
#define BULKUPDATE_H

#include <QList>

namespace model {
    template<class T>
    QList<T*> copy(const QList<const T*> entities) {
        QList<T*> copies;
        for (const T* entity : entities) copies.append(new T(*entity));
        return copies;
    }
}

template<class T, class Add = T>
class BulkUpdate {
    static_assert(!std::is_reference<T>());
    static_assert(!std::is_pointer<T>());

public:
    const QList<T*> updates;
    const QList<Add*> adds;
    const QList<const T*> deletes;

    BulkUpdate(const QList<T*> updates, const QList<const Add*> adds, QList<const T*> deletes)
        : updates{updates}
        , adds{model::copy(adds)}
        , deletes{deletes} {}

    virtual void onError() {
        for (auto entity : updates) delete entity;
        for (auto entity : adds) delete entity;
    }
};

#endif // BULKUPDATE_H
