if (${CPACK_GENERATOR} STREQUAL "DEB")
    set(CPACK_PRE_BUILD_SCRIPTS "${CMAKE_CURRENT_LIST_DIR}/debian.cmake")
    # TODO (maybe) use vv instead of ^^ and create links in /usr to exe, icon and desktop files
    # set(CPACK_PACKAGING_INSTALL_PREFIX /opt/finances)
elseif (${CPACK_GENERATOR} STREQUAL "AppImage")
    message(NOTICE "AppImage options ${CPACK_PACKAGE_NAME}")

    get_cmake_property(_variableNames VARIABLES)
    list (SORT _variableNames)
    foreach (_variableName ${_variableNames})
        message(STATUS "--> ${_variableName}=${${_variableName}}")
    endforeach()

    set(CPACK_PACKAGE_NAME finances-qt)
    set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}-${CPACK_SYSTEM_NAME}")
    set(CPACK_PACKAGE_ICON finances-qt)
    set(CPACK_PRE_BUILD_SCRIPTS "${CMAKE_CURRENT_LIST_DIR}/appimage.cmake")
endif()
