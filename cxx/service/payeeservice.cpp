#include "payeeservice.h"

PayeeService::PayeeService(ConnectionPool *connectionPool) : EntityService{connectionPool, payeeDao} {}
