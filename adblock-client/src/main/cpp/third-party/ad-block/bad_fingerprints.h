#pragma once

const char *badSubstrings[] = {"http", "www"};

// BadFingerprints exclusion is not reliable and appreciable for performance optimization.
// Disable it temporarily.
#ifndef ENABLE_BadFingerprints_Exclusion
const char *badFingerprints[] = {};
#else
const char *badFingerprints[] = {};
#endif
