target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/detailvalidator.h ${CMAKE_CURRENT_LIST_DIR}/detailvalidator.cpp
    ${CMAKE_CURRENT_LIST_DIR}/validatorfactory.h ${CMAKE_CURRENT_LIST_DIR}/validatorfactory.cpp
    ${CMAKE_CURRENT_LIST_DIR}/numeric.h ${CMAKE_CURRENT_LIST_DIR}/numeric.cpp
    ${CMAKE_CURRENT_LIST_DIR}/required.h ${CMAKE_CURRENT_LIST_DIR}/required.cpp
    ${CMAKE_CURRENT_LIST_DIR}/trimmed.h ${CMAKE_CURRENT_LIST_DIR}/trimmed.cpp
    ${CMAKE_CURRENT_LIST_DIR}/unique.h ${CMAKE_CURRENT_LIST_DIR}/unique.cpp
)
