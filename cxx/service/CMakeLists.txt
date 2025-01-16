target_sources(finances
  PRIVATE
  ${CMAKE_CURRENT_LIST_DIR}/servicecontext.h ${CMAKE_CURRENT_LIST_DIR}/servicecontext.cpp
  ${CMAKE_CURRENT_LIST_DIR}/accountservice.h ${CMAKE_CURRENT_LIST_DIR}/accountservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/categoryservice.h ${CMAKE_CURRENT_LIST_DIR}/categoryservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/companyservice.h ${CMAKE_CURRENT_LIST_DIR}/companyservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/entityservice.h
  ${CMAKE_CURRENT_LIST_DIR}/groupservice.h ${CMAKE_CURRENT_LIST_DIR}/groupservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/payeeservice.h ${CMAKE_CURRENT_LIST_DIR}/payeeservice.cpp
)

add_subdirectory(model/)
add_subdirectory(database/)
