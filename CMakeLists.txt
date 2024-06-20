cmake_minimum_required(VERSION 3.27)

project(finances VERSION 0.1 LANGUAGES CXX)

set(CMAKE_AUTOUIC ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(ExternalProject)
find_package(QT NAMES Qt6 Qt5 REQUIRED COMPONENTS Widgetsl)
find_package(Qt${QT_VERSION_MAJOR} REQUIRED COMPONENTS Widgets Sql Test)

set(PROJECT_SOURCES
    main.cpp
    # showstyles.cpp
)

if(${QT_VERSION_MAJOR} GREATER_EQUAL 6)
    qt_add_executable(finances
        MANUAL_FINALIZATION
        ${PROJECT_SOURCES}
        ui/resources/finances.qrc
        ui/resources/finances.svg
        ui/resources/dark.qss
        ui/resources/light.qss
    )
    add_subdirectory(database/)
    add_subdirectory(ui/)
# Define target properties for Android with Qt 6 as:
#    set_property(TARGET finances APPEND PROPERTY QT_ANDROID_PACKAGE_SOURCE_DIR
#                 ${CMAKE_CURRENT_SOURCE_DIR}/android)
# For more information, see https://doc.qt.io/qt-6/qt-add-executable.html#target-creation
else()
    if(ANDROID)
        add_library(finances SHARED
            ${PROJECT_SOURCES}
        )
# Define properties for Android with Qt 5 after find_package() calls as:
#    set(ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android")
    else()
        add_executable(finances
            ${PROJECT_SOURCES}
        )
    endif()
endif()

ExternalProject_Add(qdecimal
    GIT_REPOSITORY git@github.com:semihc/qdecimal.git
    CMAKE_ARGS -DCMAKE_PREFIX_PATH=${CMAKE_PREFIX_PATH}
    INSTALL_COMMAND ""
    BUILD_BYPRODUCTS <BINARY_DIR>/src/libqdecimal.a
)
ExternalProject_Get_Property(qdecimal SOURCE_DIR BINARY_DIR)
add_dependencies(finances qdecimal)
# add_subdirectory(tests/)

target_include_directories(finances PRIVATE
    ${SOURCE_DIR}/src
    ${SOURCE_DIR}/decnumber
)

target_link_libraries(finances PRIVATE
    Qt${QT_VERSION_MAJOR}::Widgets
    Qt${QT_VERSION_MAJOR}::Sql
    ${BINARY_DIR}/src/libqdecimal.a
    ${BINARY_DIR}/decnumber/libdecnumber.a
)

# Qt for iOS sets MACOSX_BUNDLE_GUI_IDENTIFIER automatically since Qt 6.1.
# If you are developing for iOS or macOS you should consider setting an
# explicit, fixed bundle identifier manually though.
if(${QT_VERSION} VERSION_LESS 6.1.0)
  set(BUNDLE_ID_OPTION MACOSX_BUNDLE_GUI_IDENTIFIER com.example.finances)
endif()
set_target_properties(finances PROPERTIES
    ${BUNDLE_ID_OPTION}
    MACOSX_BUNDLE_BUNDLE_VERSION ${PROJECT_VERSION}
    MACOSX_BUNDLE_SHORT_VERSION_STRING ${PROJECT_VERSION_MAJOR}.${PROJECT_VERSION_MINOR}
    MACOSX_BUNDLE TRUE
    WIN32_EXECUTABLE TRUE
)

include(GNUInstallDirs)
install(TARGETS finances
    BUNDLE DESTINATION .
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
)

if(QT_VERSION_MAJOR EQUAL 6)
    qt_finalize_executable(finances)
endif()
