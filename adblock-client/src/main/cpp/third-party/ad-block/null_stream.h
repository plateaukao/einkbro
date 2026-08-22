/* Copyright (c) 2026 EinkBro. Distributed under the MPL2 like the rest of
 * the ad-block sources.
 *
 * Drop-in replacement for <iostream> in the ad-block engine. The engine only
 * writes debug statistics to std::cout, which is invisible on Android, while
 * pulling <iostream> into a statically linked libc++ costs ~300 KB per ABI
 * (locale, wchar and number facets). std::cout/std::endl are kept as names
 * so the engine sources stay diff-friendly against upstream.
 */
#ifndef NULL_STREAM_H_
#define NULL_STREAM_H_

namespace std {

struct NullStream {
    template<class T>
    NullStream &operator<<(const T &) { return *this; }
    NullStream &operator<<(NullStream &(*)(NullStream &)) { return *this; }
};

inline NullStream &endl(NullStream &s) { return s; }

static NullStream cout;

}  // namespace std

#endif  // NULL_STREAM_H_
