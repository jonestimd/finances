# cmds:
# $ cmake -S . -B build
# $ cmake --build build -v
# env:
# CMAKE_PREFIX_PATH=/opt/Qt/6.7.2/gcc_64/lib/cmake
cmake_minimum_required(VERSION 3.27)

project(finances VERSION 0.1 LANGUAGES CXX)

set(CMAKE_AUTOUIC ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(ExternalProject)

ExternalProject_Add(qdecimal
    GIT_REPOSITORY git@github.com:semihc/qdecimal.git
    CMAKE_ARGS -DCMAKE_PREFIX_PATH=${CMAKE_PREFIX_PATH}
    INSTALL_COMMAND ""
    BUILD_BYPRODUCTS
        <BINARY_DIR>/decnumber/libdecnumber.a
        <BINARY_DIR>/src/libqdecimal.a
)
ExternalProject_Get_Property(qdecimal SOURCE_DIR BINARY_DIR)
add_library(libdecnumber STATIC IMPORTED)
add_dependencies(libdecnumber qdecimal)
set_target_properties(libdecnumber
    PROPERTIES IMPORTED_LOCATION ${BINARY_DIR}/decnumber/libdecnumber.a
)
add_library(libqdecimal STATIC IMPORTED)
add_dependencies(libqdecimal qdecimal)
set_target_properties(libqdecimal
    PROPERTIES IMPORTED_LOCATION ${BINARY_DIR}/src/libqdecimal.a
)

find_package(QT NAMES Qt6 Qt5 REQUIRED COMPONENTS Widgetsl)
find_package(Qt${QT_VERSION_MAJOR} REQUIRED COMPONENTS Widgets Sql Concurrent Test)

set(PROJECT_RESOURCES
    cxx/ui/resources/finances.qrc
    cxx/ui/resources/finances.svg
    cxx/ui/resources/finances.qss
    cxx/ui/resources/minimal-dark.qss
    cxx/ui/resources/minimal-light.qss
)

if(${QT_VERSION_MAJOR} GREATER_EQUAL 6)
    qt_add_executable(finances
        MANUAL_FINALIZATION
        ${PROJECT_RESOURCES}
    )
    add_dependencies(finances libdecnumber libqdecimal)
    add_subdirectory(cxx/)
# Define target properties for Android with Qt 6 as:
#    set_property(TARGET finances APPEND PROPERTY QT_ANDROID_PACKAGE_SOURCE_DIR
#                 ${CMAKE_CURRENT_SOURCE_DIR}/android)
# For more information, see https://doc.qt.io/qt-6/qt-add-executable.html#target-creation
else()
    if(ANDROID)
        add_library(finances SHARED
            # ${PROJECT_SOURCES}
        )
# Define properties for Android with Qt 5 after find_package() calls as:
#    set(ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android")
    else()
        add_executable(finances
            # ${PROJECT_SOURCES}
        )
    endif()
endif()
# add_subdirectory(tests/)

target_include_directories(finances PRIVATE
    ${SOURCE_DIR}/src
    ${SOURCE_DIR}/decnumber
)

target_link_libraries(finances PRIVATE
    Qt${QT_VERSION_MAJOR}::Widgets
    Qt${QT_VERSION_MAJOR}::Sql
    Qt${QT_VERSION_MAJOR}::Concurrent
    # comment out next 2 lines for initial build with Ninja
    libqdecimal
    libdecnumber
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
