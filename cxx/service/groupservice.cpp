#include "groupservice.h"

GroupService::GroupService(ConnectionPool *connectionPool, TransactionGroupDao &dao)
    : EntityService{connectionPool, dao} {}
