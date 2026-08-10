#ifndef ENTITY_DAO_H
#define ENTITY_DAO_H

#include "service/database/dbdialect.h"
#include "sql.h"
#include <QSqlDatabase>
#include <QLoggingCategory>

template<typename V>
struct unwrap_value {
    typedef V value_type;
    static value_type get(V value) {
        return value;
    }
};

template<typename V>
struct unwrap_value<std::optional<V>> {
    typedef V value_type;
    static value_type get(std::optional<V> value) {
        return value.value();
    }
};

typedef struct {
    const char *const createTableSql;
    const char *const getAllSql;
    const char *const updateSql;
    const char *const insertSql;
    const char *const deleteSql;
} DaoQueries;

template<class Entity>
class EntityDao : protected DaoQueries {
    const char *const idColumn;

protected:
    const char* const className;
    const QString staleDataMessage;

    template<class T = Entity>
    static auto loadRows(QSqlQuery &query) {
        QList<const T*> entities;
        while (query.next()) {
            auto entity = new T(query.record());
            entities.append(entity);
        }
        return entities;
    }

    template<class T = Entity, class ID = optional_id>
    static auto load(QSqlQuery &query) {
        typedef unwrap_value<ID> Key;
        typedef typename Key::value_type KeyType;
        QHash<KeyType, const T*> entities;
        while (query.next()) {
            auto entity = new T(query.record());
            entities.insert(Key::get(entity->id), entity);
        }
        return entities;
    }

    virtual void bindUpdateValues(QSqlQuery &query, Entity *entity) {
        sql::bindValue(query, ":id", entity->id);
        sql::bindValue(query, ":version", entity->version);
    }

    virtual void bindInsertValues(QSqlQuery &query, Entity *entity) = 0;

    EntityDao(const DaoQueries &queries, const char *className, const QString staleDataMessage, const char *idColumn = "id")
        : DaoQueries{queries}
        , className{className}
        , staleDataMessage{staleDataMessage}
        , idColumn{idColumn}
    {}

public:
    virtual void createTable(const QSqlDatabase &db) const {
        sql::exec(db, createTableSql, className, "createTable");
    }

    virtual QHash<domain_id, const Entity*> getAll(QSqlDatabase &db) {
        QSqlQuery query(db);
        query.prepare(getAllSql);
        sql::exec(query, className, "getAll");
        return load(query);
    }

    QList<const Entity*> get(const QSqlDatabase &db, QList<domain_id> ids) const {
        QSqlQuery query = dbDialect::prepareGetByIds(db, getAllSql, ids, idColumn);
        sql::exec(query, className, "getByIds");
        return loadRows(query);
    }

    /**
     * @brief update Saves changes to `entities` to the database.
     * @return input `entities` with `id`, `version` and `changeUser` updated.
     */
    virtual QList<const Entity*> update(QSqlDatabase &db, const QList<Entity*> entities, const QString &user) {
        QSqlQuery query(db);
        QList<const Entity*> result;
        result.reserve(entities.length());
        query.prepare(updateSql);
        sql::bindValue(query, ":user", user);
        for (auto entity : entities) {
            bindUpdateValues(query, entity);
            sql::exec(query, className, "update");
            if (query.numRowsAffected() < 1) throw staleDataMessage;
            entity->version = entity->version + 1;
            entity->changeUser = user;
            result.append(entity);
        }
        return result;
    }

    /**
     * @brief add Saves `entities` to the database.
     * @return input `entities` with `id` and `changeUser` updated.
     */
    virtual QList<const Entity*> add(QSqlDatabase &db, const QList<Entity*> entities, const QString &user) {
        QSqlQuery query(db);
        QList<const Entity*> result;
        result.reserve(entities.length());
        query.prepare(insertSql);
        sql::bindValue(query, ":user", user);
        for (auto entity : entities) {
            bindInsertValues(query, entity);
            sql::exec(query, className, "insert");
            entity->id = query.lastInsertId().toLongLong();
            entity->changeUser = user;
            result.append(entity);
        }
        return result;
    }

    virtual void remove(QSqlDatabase &db, const QVariant id) {
        QSqlQuery query(db);
        query.prepare(deleteSql);
        sql::bindValue(query, ":id", id);
        sql::exec(query, className, "remove");
    }

    virtual void remove(QSqlDatabase &db, const QList<const Entity*> entities) {
        QSqlQuery query(db);
        query.prepare(deleteSql);
        for (auto entity : entities) {
            sql::bindValue(query, ":id", entity->id);
            sql::exec(query, className, "remove");
            // TODO check version
        }
    }
};

template<class Entity>
class NamedEntityDao : public EntityDao<Entity> {
public:
    NamedEntityDao(const char *getAllSql, const char *updateSql, const char *insertSql, const char *deleteSql,
                   const char *className, const QString staleDataMessage, const char *idColumn = "id")
        : EntityDao<Entity>(getAllSql, updateSql, insertSql, deleteSql, className, staleDataMessage, idColumn) {}

    NamedEntityDao(const DaoQueries &queries, const char *className, const QString staleDataMessage, const char *idColumn = "id")
        : EntityDao<Entity>(queries, className, staleDataMessage, idColumn) {}

    virtual void bindUpdateValues(QSqlQuery &query, Entity *entity) override {
        EntityDao<Entity>::bindUpdateValues(query, entity);
        sql::bindValue(query,":name", entity->name);
    }

    virtual void bindInsertValues(QSqlQuery &query, Entity *entity) override {
        sql::bindValue(query, ":name", entity->name);
    }
};

#endif // ENTITY_DAO_H
