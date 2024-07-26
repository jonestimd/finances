target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/factory.h ${CMAKE_CURRENT_LIST_DIR}/factory.cpp
    ${CMAKE_CURRENT_LIST_DIR}/composite.h ${CMAKE_CURRENT_LIST_DIR}/composite.cpp
    ${CMAKE_CURRENT_LIST_DIR}/required.h ${CMAKE_CURRENT_LIST_DIR}/required.cpp
    ${CMAKE_CURRENT_LIST_DIR}/trimmed.h ${CMAKE_CURRENT_LIST_DIR}/trimmed.cpp
    ${CMAKE_CURRENT_LIST_DIR}/unique.h ${CMAKE_CURRENT_LIST_DIR}/unique.cpp
)
