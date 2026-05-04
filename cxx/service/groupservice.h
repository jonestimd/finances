#ifndef GROUPSERVICE_H
#define GROUPSERVICE_H

#include "entityservice.h"
#include "service/model/transactiongroup.h"
#include "service/database/transactiongroupdao.h"

class GroupService : public EntityService<TransactionGroup, TransactionGroupDao> {
public:
    GroupService(ConnectionPool *connectionPool, TransactionGroupDao &dao);
};

#endif // GROUPSERVICE_H
