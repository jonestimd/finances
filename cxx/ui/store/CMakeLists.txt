target_sources(finances-ui
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/accountstore.h ${CMAKE_CURRENT_LIST_DIR}/accountstore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/categorystore.h ${CMAKE_CURRENT_LIST_DIR}/categorystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companystore.h ${CMAKE_CURRENT_LIST_DIR}/companystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/datastore.h ${CMAKE_CURRENT_LIST_DIR}/datastore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/entitystore.h ${CMAKE_CURRENT_LIST_DIR}/entitystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/groupstore.h ${CMAKE_CURRENT_LIST_DIR}/groupstore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/payeestore.h ${CMAKE_CURRENT_LIST_DIR}/payeestore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/securitystore.h ${CMAKE_CURRENT_LIST_DIR}/securitystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/statusmessagestore.h ${CMAKE_CURRENT_LIST_DIR}/statusmessagestore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactiondetailstore.h ${CMAKE_CURRENT_LIST_DIR}/transactiondetailstore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactionstore.h ${CMAKE_CURRENT_LIST_DIR}/transactionstore.cpp
)
