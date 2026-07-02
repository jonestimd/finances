#ifndef ENTITY_SERVICE_H
#define ENTITY_SERVICE_H

#include "database/connectionpool.h"
#include "model/bulkupdate.h"

template<class Entity, class Dao>
class EntityService {
protected:
    Dao &dao;
    ConnectionPool *connectionPool;
public:
    EntityService(ConnectionPool *connectionPool, Dao &dao) : dao{dao}, connectionPool{connectionPool} {}

    QHash<domain_id, const Entity*> getAll() {
        auto conn = Connection(connectionPool);
        return dao.getAll(conn.db);
    }

    virtual QList<const Entity*> update(BulkUpdate<Entity> &changes, const QString &user) {
        auto conn = Connection(connectionPool);
        try {
            QList<const Entity*> result;
            if (!changes.deletes.empty()) dao.remove(conn.db, changes.deletes);
            if (!changes.updates.empty()) result += dao.update(conn.db, changes.updates, user);
            if (!changes.adds.empty()) result += dao.add(conn.db, changes.adds, user);
            return result;
        } catch(...) {
            conn.db.rollback();
            changes.onError();
            throw;
        }
    }

    const Entity *add(const QString &name, const QString &user) {
        auto entity = new Entity(name);
        auto conn = Connection(connectionPool);
        try {
            dao.add(conn.db, QList{entity}, user);
            return entity;
        } catch(...) {
            conn.db.rollback();
            delete entity;
            throw;
        }
    }

protected:
    template<typename Result>
    Result doInTransaction(std::function<Result(QSqlDatabase&)> update) {
        auto conn = Connection(connectionPool);
        try {
            return update(conn.db);
        } catch(...) {
            conn.db.rollback();
            throw;
        }
    }
};

#endif // ENTITY_SERVICE_H
