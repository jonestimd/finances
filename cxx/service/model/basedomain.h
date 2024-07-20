#ifndef BASEDOMAIN_H
#define BASEDOMAIN_H

#include <QSqlRecord>
#include <QVariant>

class BaseDomain {
public:
    QVariant id;
    QVariant version;
    QVariant changeUser;
    QVariant changeDate;

    BaseDomain();
    BaseDomain(QSqlRecord record);
};

class NamedEntity : public BaseDomain {
public:
    NamedEntity() = default;
    NamedEntity(QSqlRecord);

    virtual QString displayName() const = 0;
};

Q_DECLARE_METATYPE(const NamedEntity*)
// static const int namedEntityTypeId = qRegisterMetaType<NamedEntity>();

template<typename T>
concept NameAndId = std::is_base_of<NamedEntity, T>::value;

template<NameAndId T>
using ValuesSupplier = std::function<const QHash<qlonglong, const T*>()>;

#endif // BASEDOMAIN_H
