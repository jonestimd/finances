#include "payeeservice.h"
#include "service/database/transactiondao.h"

PayeeService::PayeeService(ConnectionPool *connectionPool) : EntityService{connectionPool, payeeDao} {}

QHash<qlonglong, const Payee*> PayeeService::merge(const Payee *payee, const QVariant destinationId, const QString &user) {
    return doInTransaction<QHash<qlonglong, const Payee*>>([=, this](QSqlDatabase &db) {
        transactionDao.replacePayee(db, payee, destinationId, user);
        dao.remove(db, QList{payee});
        return dao.get(db, QList{destinationId});
    });
}
