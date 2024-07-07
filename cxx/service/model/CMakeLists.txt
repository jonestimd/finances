target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/bulkupdate.h
    ${CMAKE_CURRENT_LIST_DIR}/basedomain.h ${CMAKE_CURRENT_LIST_DIR}/basedomain.cpp
    ${CMAKE_CURRENT_LIST_DIR}/account.h ${CMAKE_CURRENT_LIST_DIR}/account.cpp
    ${CMAKE_CURRENT_LIST_DIR}/accounttype.h ${CMAKE_CURRENT_LIST_DIR}/accounttype.cpp
    ${CMAKE_CURRENT_LIST_DIR}/company.h ${CMAKE_CURRENT_LIST_DIR}/company.cpp
    ${CMAKE_CURRENT_LIST_DIR}/decimal.h ${CMAKE_CURRENT_LIST_DIR}/decimal.cpp
    ${CMAKE_CURRENT_LIST_DIR}/sql.h ${CMAKE_CURRENT_LIST_DIR}/sql.cpp
)
