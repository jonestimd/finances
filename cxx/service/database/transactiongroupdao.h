#ifndef TRANSACTIONGROUPDAO_H
#define TRANSACTIONGROUPDAO_H

#include "entitydao.h"
#include "service/model/transactiongroup.h"

class TransactionGroupDao : public EntityDao<TransactionGroup>
{
public:
    TransactionGroupDao();
};

static TransactionGroupDao transactionGroupDao;

#endif // TRANSACTIONGROUPDAO_H
