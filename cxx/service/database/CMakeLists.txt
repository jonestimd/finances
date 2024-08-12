target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/accountdao.h ${CMAKE_CURRENT_LIST_DIR}/accountdao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/categorydao.h ${CMAKE_CURRENT_LIST_DIR}/categorydao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/connectionpool.h ${CMAKE_CURRENT_LIST_DIR}/connectionpool.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companydao.h ${CMAKE_CURRENT_LIST_DIR}/companydao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/entitydao.h
    ${CMAKE_CURRENT_LIST_DIR}/mapping.h ${CMAKE_CURRENT_LIST_DIR}/mapping.cpp
    ${CMAKE_CURRENT_LIST_DIR}/payeedao.h ${CMAKE_CURRENT_LIST_DIR}/payeedao.cpp
)
