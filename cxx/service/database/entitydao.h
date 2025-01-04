#ifndef ENTITY_DAO_H
#define ENTITY_DAO_H

#include <QtSql/QSqlDatabase>
#include <QtSql>

template<class Entity>
class EntityDao {
    const char *const getAllSql;
    const char *const updateSql;
    const char *const insertSql;
    const char *const deleteSql;
    const QString className;

protected:
    const QString staleDataMessage;

    QList<const Entity*> load(QSqlQuery &query) {
        QList<const Entity*> entities;
        if (query.size() > 0) entities.reserve(query.size());
        while (query.next()) {
            entities.append(new Entity(query.record()));
        }
        return entities;
    }

    void exec(QSqlQuery &query, const char *methodName) {
        if (!query.exec()) {
            qCritical() << className << "." << methodName << ":" << query.lastError();
            throw query.lastError().text();
        }
    }

    virtual void bindUpdateValues(QSqlQuery &query, Entity *entity) {
        query.bindValue(":id", entity->id);
        query.bindValue(":name", entity->name);
        query.bindValue(":version", entity->version);
    }

    virtual void bindInsertValues(QSqlQuery &query, Entity *entity) {
        query.bindValue(":name", entity ->name);
    }

public:
    EntityDao(const char *getAllSql, const char *updateSql, const char *insertSql, const char *deleteSql, const char *className, const QString staleDataMessage)
        : getAllSql{getAllSql}
        , updateSql{updateSql}
        , insertSql{insertSql}
        , deleteSql{deleteSql}
        , className{className}
        , staleDataMessage{staleDataMessage} {}

    virtual QList<const Entity*> getAll(QSqlDatabase &db) {
        QSqlQuery query(getAllSql, db);
        exec(query, "getAll");
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

#endif // ENTITY_DAO_H
