#include "groupservice.h"

GroupService::GroupService(ConnectionPool *connectionPool) : EntityService{connectionPool, transactionGroupDao} {}
