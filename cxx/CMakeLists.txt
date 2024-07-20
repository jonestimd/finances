target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/main.cpp
    # ${CMAKE_CURRENT_LIST_DIR}/showstyles.cpp
)

add_subdirectory(service/)
add_subdirectory(ui/)
add_subdirectory(tests/)
