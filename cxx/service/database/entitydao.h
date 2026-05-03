#ifndef ENTITY_DAO_H
#define ENTITY_DAO_H

#include "service/database/dbdialect.h"
#include "sql.h"
#include <QSqlDatabase>
#include <QLoggingCategory>

template<class Entity>
class EntityDao {
    const char *const getAllSql;
    const char *const updateSql;
    const char *const insertSql;
    const char *const deleteSql;
    const char *const idColumn;

protected:
    const QString className;
    const QString staleDataMessage;

    QHash<qlonglong, const Entity*> load(QSqlQuery &query) {
        QHash<qlonglong, const Entity*> entities;
        while (query.next()) {
            auto entity = new Entity(query.record());
            entities.insert(entity->id.toLongLong(), entity);
        }
        return entities;
    }

    virtual void bindUpdateValues(QSqlQuery &query, Entity *entity) {
        SQL_BIND_VALUE(query, ":id", entity->id);
        SQL_BIND_VALUE(query, ":version", entity->version);
    }

    virtual void bindInsertValues(QSqlQuery &query, Entity *entity) = 0;

    virtual QString getLoadAllQuery(QSqlDatabase &db) {
        return getAllSql;
    }

    EntityDao(const char *getAllSql, const char *updateSql, const char *insertSql, const char *deleteSql, const char *className,
              const QString staleDataMessage, const char *idColumn = "id")
        : getAllSql{getAllSql}
        , updateSql{updateSql}
        , insertSql{insertSql}
        , deleteSql{deleteSql}
        , className{className}
        , staleDataMessage{staleDataMessage}
        , idColumn{idColumn}    {}

public:
    virtual QHash<qlonglong, const Entity*> getAll(QSqlDatabase &db) {
        QSqlQuery query(db);
        query.prepare(getLoadAllQuery(db));
        SQL_EXEC(query, "getAll");
        return load(query);
    }

    QHash<qlonglong, const Entity*> get(QSqlDatabase &db, QVariantList ids) {
        QSqlQuery query = dbDialect::prepareGetByIds(db, getAllSql, ids, idColumn);
        SQL_EXEC(query, "getByIds");
        return load(query);
    }

    virtual QList<const Entity*> update(QSqlDatabase &db, const QList<Entity*> entities, const QString &user) {
        QSqlQuery query(db);
        QList<const Entity*> result;
        result.reserve(entities.length());
        query.prepare(updateSql);
        qCInfo(sqlLogger, updateSql);
        SQL_BIND_VALUE(query, ":user", user);
        for (auto entity : entities) {
            bindUpdateValues(query, entity);
            SQL_EXEC(query, "update");
            if (query.numRowsAffected() < 1) throw staleDataMessage;
            entity->version = entity->version.toInt() + 1;
            entity->changeUser = user;
            result.append(entity);
        }
        return result;
    }

    virtual QList<const Entity*> add(QSqlDatabase &db, const QList<Entity*> entities, const QString &user) {
        QSqlQuery query(db);
        QList<const Entity*> result;
        result.reserve(entities.length());
        query.prepare(insertSql);
        qCInfo(sqlLogger, insertSql);
        SQL_BIND_VALUE(query, ":user", user);
        for (auto entity : entities) {
            bindInsertValues(query, entity);
            SQL_EXEC(query, "insert");
            entity->id = query.lastInsertId();
            entity->changeUser = user;
            result.append(entity);
        }
        return result;
    }

    virtual void remove(QSqlDatabase &db, const QVariant id) {
        QSqlQuery query(db);
        query.prepare(deleteSql);
        qCInfo(sqlLogger, deleteSql);
        SQL_BIND_VALUE(query, ":id", id);
        SQL_EXEC(query, "remove");
    }

    virtual void remove(QSqlDatabase &db, const QList<const Entity*> entities) {
        QSqlQuery query(db);
        query.prepare(deleteSql);
        qCInfo(sqlLogger, deleteSql);
        for (auto entity : entities) {
            SQL_BIND_VALUE(query, ":id", entity->id);
            SQL_EXEC(query, "remove");
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

    virtual void bindUpdateValues(QSqlQuery &query, Entity *entity) override {
        EntityDao<Entity>::bindUpdateValues(query, entity);
        SQL_BIND_VALUE(query,":name", entity->name);
    }

    virtual void bindInsertValues(QSqlQuery &query, Entity *entity) override {
        SQL_BIND_VALUE(query, ":name", entity->name);
    }
};

#endif // ENTITY_DAO_H
