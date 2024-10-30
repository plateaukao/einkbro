/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#ifndef AD_BLOCK_CLIENT_H_
#define AD_BLOCK_CLIENT_H_

#include <string>
#include <set>
#include "./filter.h"
#include "cosmetic_filter.h"

class CosmeticFilter;

class BloomFilter;

class BadFingerprintsHashSet;

class NoFingerprintDomain;

template<class T>
class HashSet;

template<class K, class V>
class HashMap;

class AdBlockClient {
public:
    AdBlockClient();

    ~AdBlockClient();

    void clear();

//   bool parse(const char *input);
    bool parse(const char *input, bool preserveRules = false);

    bool matches(const char *input,
                 FilterOption contextOption = FONoFilterOption,
                 const char *contextDomain = nullptr,
                 Filter **matchedFilter = nullptr,
                 Filter **matchedExceptionFilter = nullptr);

    bool findMatchingFilters(const char *input,
                             FilterOption contextOption,
                             const char *contextDomain,
                             Filter **matchingFilter,
                             Filter **matchingExceptionFilter);

    char *getElementHidingSelectors(const char *host, int hostLen) const;

    char *getElementHidingExceptionSelectors(const char *host, int hostLen) const;

    const char *getElementHidingSelectors(const char *contextUrl);

    const LinkedList<std::string> *getExtendedCssSelectors(const char *contextUrl);

    const LinkedList<std::string> *getCssRules(const char *contextUrl);

    const LinkedList<std::string> *getScriptlets(const char *contextUrl);

    void addTag(const std::string &tag);

    void removeTag(const std::string &tag);

    bool tagExists(const std::string &tag) const;

    // Serializes a the parsed data and bloom filter data into a single buffer.
    // The returned buffer should be deleted.
    char *serialize(int *size, bool ignoreHtmlFilters = true) const;

    // Deserializes the buffer, a size is not needed since a serialized.
    // buffer is self described
    bool deserialize(char *buffer);

    void enableBadFingerprintDetection();

    const char *getDeserializedBuffer() {
        return deserializedBuffer;
    }

    static bool getFingerprint(char *buffer, const char *input);

    static bool getFingerprint(char *buffer, const Filter &f);

    Filter *filters;
    Filter *htmlFilters;
    Filter *exceptionFilters;
    Filter *noFingerprintFilters;
    Filter *noFingerprintExceptionFilters;
    Filter *noFingerprintDomainOnlyFilters;
    Filter *noFingerprintAntiDomainOnlyFilters;
    Filter *noFingerprintDomainOnlyExceptionFilters;
    Filter *noFingerprintAntiDomainOnlyExceptionFilters;

    int numFilters;
    int numCosmeticFilters;
    int numHtmlFilters;
    int numScriptletFilters;
    int numExceptionFilters;
    int numNoFingerprintFilters;
    int numNoFingerprintExceptionFilters;
    int numNoFingerprintDomainOnlyFilters;
    int numNoFingerprintAntiDomainOnlyFilters;
    int numNoFingerprintDomainOnlyExceptionFilters;
    int numNoFingerprintAntiDomainOnlyExceptionFilters;
    int numHostAnchoredFilters;
    int numHostAnchoredExceptionFilters;

    BloomFilter *bloomFilter;
    BloomFilter *exceptionBloomFilter;
    HashSet<Filter> *hostAnchoredHashSet;
    HashSet<Filter> *hostAnchoredExceptionHashSet;
    HashSet<NoFingerprintDomain> *noFingerprintDomainHashSet;
    HashSet<NoFingerprintDomain> *noFingerprintAntiDomainHashSet;
    HashSet<NoFingerprintDomain> *noFingerprintDomainExceptionHashSet;
    HashSet<NoFingerprintDomain> *noFingerprintAntiDomainExceptionHashSet;

    bool isGenericElementHidingEnabled;
    CosmeticFilter *genericElementHidingSelectors;
    HashMap<NoFingerprintDomain, CosmeticFilter> *elementHidingSelectorHashMap;
    HashMap<NoFingerprintDomain, CosmeticFilter> *elementHidingExceptionSelectorHashMap;
    HashMap<NoFingerprintDomain, CosmeticFilterHashSet> *extendedCssMap;

    HashMap<NoFingerprintDomain, CosmeticFilterHashSet> *cssRulesMap;

    HashMap<NoFingerprintDomain, CosmeticFilterHashSet> *scriptletMap;

    // Used only in the perf program to create a list of bad fingerprints
    BadFingerprintsHashSet *badFingerprintsHashSet;

    // Stats kept for matching
    unsigned int numFalsePositives;
    unsigned int numExceptionFalsePositives;
    unsigned int numBloomFilterSaves;
    unsigned int numExceptionBloomFilterSaves;
    unsigned int numHashSetSaves;
    unsigned int numExceptionHashSetSaves;

    static const int kFingerprintSize;

protected:
    // Determines if a passed in array of filter pointers matches for any of
    // the input
    bool hasMatchingFilters(Filter *filter, int numFilters, const char *input,
                            int inputLen, FilterOption contextOption, const char *contextDomain,
                            BloomFilter *inputBloomFilter, const char *inputHost, int inputHostLen,
                            Filter **matchingFilter = nullptr) const;

    bool isHostAnchoredHashSetMiss(const char *input, int inputLen,
                                   HashSet<Filter> *hashSet,
                                   const char *inputHost,
                                   int inputHostLen,
                                   FilterOption contextOption,
                                   const char *contextDomain,
                                   Filter **foundFilter = nullptr) const;

    static void initBloomFilter(BloomFilter **, const char *buffer, int len);

    template<class T>
    bool initHashSet(HashSet<T> **, char *buffer, int len);

    template<class K, class V>
    bool initHashMap(HashMap<K, V> **, char *buffer, int len);

    HashMap<NoFingerprintDomain, CosmeticFilter> *elementHidingSelectorsCache;
    HashMap<NoFingerprintDomain, LinkedList<std::string>> *extendedCssCache;
    HashMap<NoFingerprintDomain, LinkedList<std::string>> *cssRulesCache;
    HashMap<NoFingerprintDomain, LinkedList<std::string>> *scriptletCache;
    char *deserializedBuffer;
    std::set<std::string> tags;
};

extern std::set<std::string> unknownOptions;
extern const char *separatorCharacters;

void parseFilter(const char *input, const char *end, Filter *f,
                 BloomFilter *bloomFilter = nullptr,
                 BloomFilter *exceptionBloomFilter = nullptr,
                 HashSet<Filter> *hostAnchoredHashSet = nullptr,
                 HashSet<Filter> *hostAnchoredExceptionHashSet = nullptr,
                 HashSet<CosmeticFilter> *simpleCosmeticFilters = nullptr,
                 bool preserveRules = false);

void parseFilter(const char *input, Filter *f,
                 BloomFilter *bloomFilter = nullptr,
                 BloomFilter *exceptionBloomFilter = nullptr,
                 HashSet<Filter> *hostAnchoredHashSet = nullptr,
                 HashSet<Filter> *hostAnchoredExceptionHashSet = nullptr,
                 HashSet<CosmeticFilter> *simpleCosmeticFilters = nullptr,
                 bool preserveRules = false);

bool isSeparatorChar(char c);
int findFirstSeparatorChar(const char *input, const char *end);

#endif  // AD_BLOCK_CLIENT_H_
