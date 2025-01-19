#ifndef ENTITY_DAO_H
#define ENTITY_DAO_H

#include "sql.h"
#include <QtSql/QSqlDatabase>
#include <QtSql>

template<class Entity>
class EntityDao {
    const char *const getAllSql;
    const QString getByIdsSql;
    const char *const updateSql;
    const char *const insertSql;
    const char *const deleteSql;
    const QString className;

protected:
    const QString staleDataMessage;

    QHash<qlonglong, const Entity*> load(QSqlQuery &query) {
        QHash<qlonglong, const Entity*> entities;
        while (query.next()) {
            auto entity = new Entity(query.record());
            entities.insert(entity->id.toLongLong(), entity);
        }
        return entities;
    }

    void exec(QSqlQuery &query, const char *queryName) {
        sql::exec(query, className, queryName);
    }

    virtual void bindUpdateValues(QSqlQuery &query, Entity *entity) {
        query.bindValue(":id", entity->id);
        query.bindValue(":version", entity->version);
    }

    virtual void bindInsertValues(QSqlQuery &query, Entity *entity) = 0;

    EntityDao(const char *getAllSql, const char *updateSql, const char *insertSql, const char *deleteSql, const char *className, const QString staleDataMessage)
        : getAllSql{getAllSql}
        , updateSql{updateSql}
        , insertSql{insertSql}
        , deleteSql{deleteSql}
        , getByIdsSql{QString(getAllSql) + "\nwhere id member of (:ids)"}
        , className{className}
        , staleDataMessage{staleDataMessage} {}

public:
    virtual QHash<qlonglong, const Entity*> getAll(QSqlDatabase &db) {
        QSqlQuery query(db);
        query.prepare(getAllSql);
        exec(query, "getAll");
        return load(query);
    }

    QHash<qlonglong, const Entity*> get(QSqlDatabase &db, QVariantList ids) {
        QSqlQuery query(db);
        query.prepare(getByIdsSql);
        sql::bindList(query, ids, ":ids");
        exec(query, "getByIds");
        return load(query);
    }

    virtual QList<const Entity*> update(QSqlDatabase &db, QList<Entity*> entities, const QString &user) {
        QSqlQuery query(db);
        QList<const Entity*> result;
        result.reserve(entities.length());
        query.prepare(updateSql);
        query.bindValue(":user", user);
        for (auto entity : entities) {
            bindUpdateValues(query, entity);
            exec(query, "update");
            if (query.numRowsAffected() < 1) throw staleDataMessage;
            entity->version = entity->version.toInt() + 1;
            entity->changeUser = user;
            result.append(entity);
        }
        return result;
    }

    virtual QList<const Entity*> add(QSqlDatabase &db, QList<Entity*> entities, const QString &user) {
        QSqlQuery query(db);
        QList<const Entity*> result;
        result.reserve(entities.length());
        query.prepare(insertSql);
        query.bindValue(":user", user);
        for (auto entity : entities) {
            bindInsertValues(query, entity);
            exec(query, "insert");
            entity->id = query.lastInsertId();
            entity->changeUser = user;
            result.append(entity);
        }
        return result;
    }

    virtual void remove(QSqlDatabase &db, QList<const Entity*> entities) {
        QSqlQuery query(db);
        query.prepare(deleteSql);
        for (auto entity : entities) {
            query.bindValue(":id", entity ->id);
            exec(query, "remove");
        }
    }
};

template<class Entity>
class NamedEntityDao : public EntityDao<Entity> {
public:
    NamedEntityDao(const char *getAllSql, const char *updateSql, const char *insertSql, const char *deleteSql, const char *className, const QString staleDataMessage)
        : EntityDao<Entity>(getAllSql, updateSql, insertSql, deleteSql, className, staleDataMessage) {}

    virtual void bindUpdateValues(QSqlQuery &query, Entity *entity) override {
        EntityDao<Entity>::bindUpdateValues(query, entity);
        query.bindValue(":name", entity->name);
    }

    virtual void bindInsertValues(QSqlQuery &query, Entity *entity) override {
        query.bindValue(":name", entity ->name);
    }
};

#endif // ENTITY_DAO_H
