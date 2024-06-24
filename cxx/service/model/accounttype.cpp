#include "accounttype.h"

AccountType::AccountType() : security{false} {
    throw "no account type name";
};

AccountType::AccountType(const char *name, bool security)
    : name{name}, security{security} {}

AccountType& AccountType::operator=(AccountType &that) {
    return that;
}

AccountType AccountType::operator=(AccountType that) {
    return that;
}
