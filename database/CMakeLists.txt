target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/dbcontext.h ${CMAKE_CURRENT_LIST_DIR}/dbcontext.cpp
    ${CMAKE_CURRENT_LIST_DIR}/accountdao.h ${CMAKE_CURRENT_LIST_DIR}/accountdao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companydao.h ${CMAKE_CURRENT_LIST_DIR}/companydao.cpp
)

add_subdirectory(model/)
