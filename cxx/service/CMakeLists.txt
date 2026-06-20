target_sources(finances
  PRIVATE
  ${CMAKE_CURRENT_LIST_DIR}/servicecontext.h ${CMAKE_CURRENT_LIST_DIR}/servicecontext.cpp
  ${CMAKE_CURRENT_LIST_DIR}/accountservice.h ${CMAKE_CURRENT_LIST_DIR}/accountservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/categoryservice.h ${CMAKE_CURRENT_LIST_DIR}/categoryservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/companyservice.h ${CMAKE_CURRENT_LIST_DIR}/companyservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/entityservice.h
  ${CMAKE_CURRENT_LIST_DIR}/groupservice.h ${CMAKE_CURRENT_LIST_DIR}/groupservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/payeeservice.h ${CMAKE_CURRENT_LIST_DIR}/payeeservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/securityservice.h ${CMAKE_CURRENT_LIST_DIR}/securityservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/transactiondetailservice.h ${CMAKE_CURRENT_LIST_DIR}/transactiondetailservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/transactionservice.h ${CMAKE_CURRENT_LIST_DIR}/transactionservice.cpp
)

add_subdirectory(model/)
add_subdirectory(database/)

add_subdirectory(tests/)