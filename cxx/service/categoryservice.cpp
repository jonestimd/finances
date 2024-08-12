#include "categoryservice.h"

CategoryService::CategoryService(ConnectionPool *connectionPool) : EntityService(connectionPool, categoryDao) {}
