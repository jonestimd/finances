target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/validatorfactory.h ${CMAKE_CURRENT_LIST_DIR}/validatorfactory.cpp
    ${CMAKE_CURRENT_LIST_DIR}/required.h ${CMAKE_CURRENT_LIST_DIR}/required.cpp
    ${CMAKE_CURRENT_LIST_DIR}/trimmed.h ${CMAKE_CURRENT_LIST_DIR}/trimmed.cpp
    ${CMAKE_CURRENT_LIST_DIR}/unique.h ${CMAKE_CURRENT_LIST_DIR}/unique.cpp
)
