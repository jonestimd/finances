target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/account.h ${CMAKE_CURRENT_LIST_DIR}/account.cpp
    ${CMAKE_CURRENT_LIST_DIR}/accounttype.h ${CMAKE_CURRENT_LIST_DIR}/accounttype.cpp
    ${CMAKE_CURRENT_LIST_DIR}/amounttype.h ${CMAKE_CURRENT_LIST_DIR}/amounttype.cpp
    ${CMAKE_CURRENT_LIST_DIR}/asset.h ${CMAKE_CURRENT_LIST_DIR}/asset.cpp
    ${CMAKE_CURRENT_LIST_DIR}/assettype.h ${CMAKE_CURRENT_LIST_DIR}/assettype.cpp
    ${CMAKE_CURRENT_LIST_DIR}/basedomain.h ${CMAKE_CURRENT_LIST_DIR}/basedomain.cpp
    ${CMAKE_CURRENT_LIST_DIR}/bulkupdate.h
    ${CMAKE_CURRENT_LIST_DIR}/category.h ${CMAKE_CURRENT_LIST_DIR}/category.cpp
    ${CMAKE_CURRENT_LIST_DIR}/company.h ${CMAKE_CURRENT_LIST_DIR}/company.cpp
    ${CMAKE_CURRENT_LIST_DIR}/decimal.h ${CMAKE_CURRENT_LIST_DIR}/decimal.cpp
    ${CMAKE_CURRENT_LIST_DIR}/payee.h ${CMAKE_CURRENT_LIST_DIR}/payee.cpp
    ${CMAKE_CURRENT_LIST_DIR}/security.h ${CMAKE_CURRENT_LIST_DIR}/security.cpp
    ${CMAKE_CURRENT_LIST_DIR}/securitytype.h ${CMAKE_CURRENT_LIST_DIR}/securitytype.cpp
    ${CMAKE_CURRENT_LIST_DIR}/sql.h ${CMAKE_CURRENT_LIST_DIR}/sql.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactiongroup.h ${CMAKE_CURRENT_LIST_DIR}/transactiongroup.cpp
)
