# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

project(adblock-client)
# Set the default build type to Release if not specified
if(NOT CMAKE_BUILD_TYPE)
        set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()

# Define different compiler flags for different build types
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Oz -s")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Oz -s")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        adblock-client

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/adblockclient-lib.cpp
        src/main/cpp/third-party/ad-block/ad_block_client.cc
        src/main/cpp/third-party/ad-block/cosmetic_filter.cc
        src/main/cpp/third-party/ad-block/filter.cc
        src/main/cpp/third-party/ad-block/no_fingerprint_domain.cc
        src/main/cpp/third-party/ad-block/context_domain.cc
        src/main/cpp/third-party/ad-block/protocol.cc
        src/main/cpp/third-party/bloom-filter-cpp/BloomFilter.cpp
        src/main/cpp/third-party/hashset-cpp/hashFn.cc
        src/main/cpp/third-party/hashset-cpp/hash_set.cc
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        adblock-client

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})