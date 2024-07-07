#ifndef BULKUPDATE_H
#define BULKUPDATE_H

#include <QList>

template<class T>
struct BulkUpdate {
    static_assert(!std::is_reference<T>());
    static_assert(!std::is_pointer<T>());

    const QList<T*> updates;
    const QList<T*> adds;
    const QList<const T*> deletes;

    BulkUpdate(const QList<T*> updates, const QList<T*> adds, QList<const T*> deletes)
        : updates{updates}
        , adds{adds}
        , deletes{deletes} {}

    void onError() {
        for (auto entity : updates) delete entity;
        for (auto entity : adds) delete entity;
    }
};

#endif // BULKUPDATE_H
