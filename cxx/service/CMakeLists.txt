target_sources(finances
  PRIVATE
  ${CMAKE_CURRENT_LIST_DIR}/servicecontext.h ${CMAKE_CURRENT_LIST_DIR}/servicecontext.cpp
  ${CMAKE_CURRENT_LIST_DIR}/accountservice.h ${CMAKE_CURRENT_LIST_DIR}/accountservice.cpp
  ${CMAKE_CURRENT_LIST_DIR}/companyservice.h ${CMAKE_CURRENT_LIST_DIR}/companyservice.cpp
)

add_subdirectory(model/)
add_subdirectory(database/)
