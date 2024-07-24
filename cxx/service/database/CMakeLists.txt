target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/accountdao.h ${CMAKE_CURRENT_LIST_DIR}/accountdao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/connectionpool.h ${CMAKE_CURRENT_LIST_DIR}/connectionpool.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companydao.h ${CMAKE_CURRENT_LIST_DIR}/companydao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/mapping.h ${CMAKE_CURRENT_LIST_DIR}/mapping.cpp
)
