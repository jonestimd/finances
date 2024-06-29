target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/finances.h ${CMAKE_CURRENT_LIST_DIR}/finances.cpp
)

add_subdirectory(model/)
add_subdirectory(validation/)
add_subdirectory(widget/)
