/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#include <string.h>
#include <stdio.h>
#include "./protocol.h"
#include "./ad_block_client.h"
#include "./bad_fingerprint.h"
#include "./bad_fingerprints.h"
#include "./cosmetic_filter.h"
#include "../hashset-cpp/hashFn.h"
#include "./no_fingerprint_domain.h"

#include "../bloom-filter-cpp/BloomFilter.h"

#include "linked_list.h"

#ifdef PERF_STATS
#include <iostream>
using std::cout;
using std::endl;
#endif

std::set<std::string> unknownOptions;

// Fast hash function applicable to 2 byte char checks
class HashFn2Byte : public HashFn {
public:
  HashFn2Byte() : HashFn(0, false) {
  }

  uint64_t operator()(const char *input, int len,
                      unsigned char lastCharCode, uint64_t lastHash) override;

  uint64_t operator()(const char *input, int len) override;
};

const int kMaxLineLength = 2048;

const int AdBlockClient::kFingerprintSize = 6;

static HashFn2Byte hashFn2Byte;

static char *rule_definition_fallback = "-";

/**
 * Finds the host within the passed in URL and returns its length
 */
const char *getUrlHost(const char *input, int *len) {
  const char *p = input;
  while (*p != '\0' && *p != ':') {
    p++;
  }
  if (*p == ':') {
    p++;
  }
  while (*p == '/') {
    p++;
  }
  const char *q = p;
  while (*q != '\0') {
    q++;
  }
  *len = findFirstSeparatorChar(p, q);
  return p;
}

void AddFilterDomainsToHashSet(Filter *filter,
                               HashSet<NoFingerprintDomain> *hashSet) {
  if (filter->domainList) {
    char *filter_domain_list = filter->domainList;
    int start_offset = 0;
    int len = 0;
    const char *p = filter_domain_list;
    while (true) {
      if (*p == '|' || *p == '\0') {
        const char *domain = filter_domain_list + start_offset;
        if (len > 0 && *domain != '~') {
          char buffer[1024];
          memset(buffer, 0, 1024);
          memcpy(buffer, domain, len);
          // cout << "Adding filter: " << buffer << endl;
          hashSet->Add(NoFingerprintDomain(domain, len));
        } else if (len > 0 && *domain == '~') {
          char buffer[1024];
          memset(buffer, 0, 1024);
          memcpy(buffer, domain + 1, len - 1);
          // cout << "Adding anti filter: " << buffer << endl;
          hashSet->Add(NoFingerprintDomain(domain + 1, len - 1));
        }
        start_offset += len + 1;
        len = -1;
      }
      if (*p == '\0') {
        break;
      }
      p++;
      len++;
    }
  }
}

void putElementHidingFilterToHashMap(Filter *filter, CosmeticFilterHashMap *hashMap) {
  if (filter->domainList && filter->data) {
    // copy the domainList char* which will be deleted by deconstructor
    size_t size = strlen(filter->domainList) + 1;
    char *filter_domain_list = new char[size];
    memcpy(filter_domain_list, filter->domainList, size);

    int len;
    const char *p = filter_domain_list;
    const char *q;
    while (*p != '\0') {
      len = 0;
      while (*p == ' ') {
        p++;
      }
      q = p;
      while (*q != '\0' && *q != ',') {
        len++;
        q++;
      }
      if (len > 0) {
        hashMap->putCosmeticFilter(NoFingerprintDomain(p, len), CosmeticFilter(filter->data));
        p += len;
      }
      if (*p == '\0') {
        break;
      }
      p++;
    }
  }
}

template<class T>
bool getFrom(HashMap<NoFingerprintDomain, T> *hashMap, const char *host, int hostLen,
             std::function<void(T *)> onFind) {
  if (hostLen <= 0)
    return false;
  bool find = false;
  // start from the penultimate
  const char *p = host + hostLen - 2;
  const char *start = p + 1;
  // skip past the TLD
  while (*p != '.' && p > host) {
    p--;
  }
  p--;
  // start matching parent domains
  while (p > host) {
    if (*p == '.') {
      start = p + 1;
      const size_t domainLen = hostLen - (start - host);
      // match parent domain
      if (T *f = hashMap->get(NoFingerprintDomain(start, domainLen))) {
        onFind(f);
        find = true;
      }
    }
    p--;
  }
  // match the host
  if (T *f = hashMap->get(NoFingerprintDomain(host, hostLen))) {
    onFind(f);
    find = true;
  }
  return find;
}

char *removeException(char *src, uint32_t srcLen, char *exception) {
  if (srcLen == 0) {
    srcLen = 1;
  }
  char buffer[srcLen];
  memset(buffer, 0, srcLen);
  char *sStart = src, *sEnd = src, *eStart, *eEnd;
  int len = 0, sLen = 0, eLen;
  while (*sStart != '\0') {
    while (*sEnd != ',' && *sEnd != '\0') {
      sEnd++;
      sLen++;
    }
    eLen = 0;
    eStart = eEnd = exception;
    bool contains = false;
    while (*eStart != '\0') {
      while (*eEnd != ',' && *eEnd != '\0') {
        eEnd++;
        eLen++;
      }
#ifdef PERF_STATS
      cout << sStart<<"|"<<eStart<<"|"<<sLen<<","<<eLen<<endl;
#endif
      if (sLen == eLen && memcmp(sStart, eStart, sLen) == 0) {
        contains = true;
        break;
      }
      if (*eEnd == '\0') {
        break;
      }
      eEnd += 2;
      eStart = eEnd;
      eLen = 0;
    }
    if (!contains) {
      if (len > 0) {
        memcpy(buffer + len, ", ", 2);
        len += 2;
      }
      memcpy(buffer + len, sStart, sLen);
      len += sLen;
    }
    if (*sEnd == '\0') {
      break;
    }
    sEnd += 2;
    sStart = sEnd;
    sLen = 0;
  }
  len++;
  char *result = new char[len];
  memcpy(result, buffer, len);
  return result;
}

char *AdBlockClient::getElementHidingSelectors(const char *host, int hostLen) const {
  CosmeticFilterHashSet filterHashSet(5);
  auto onFind = [&filterHashSet](CosmeticFilter *f) { filterHashSet.Add(*f); };
  if (getFrom<CosmeticFilter>(elementHidingSelectorHashMap, host, hostLen, onFind)) {
    return filterHashSet.toStylesheet();
  }
  return nullptr;
}

char *AdBlockClient::getElementHidingExceptionSelectors(const char *host, int hostLen) const {
  CosmeticFilterHashSet filterHashSet(5);
  auto onFind = [&filterHashSet](CosmeticFilter *f) { filterHashSet.Add(*f); };
  if (getFrom<CosmeticFilter>(elementHidingExceptionSelectorHashMap, host, hostLen, onFind)) {
    return filterHashSet.toStylesheet();
  }
  return nullptr;
}

const char *AdBlockClient::getElementHidingSelectors(const char *contextUrl) {
  int urlLen = static_cast<int>(strlen(contextUrl));
  if (!isBlockableProtocol(contextUrl, urlLen)) {
    return nullptr;
  }
  std::string buffer;
  int hostLen;
  const char *host = getUrlHost(contextUrl, &hostLen);
  if (!elementHidingSelectorsCache) {
    elementHidingSelectorsCache = new HashMap<NoFingerprintDomain, CosmeticFilter>(100);
  } else if (const CosmeticFilter *f =
      elementHidingSelectorsCache->get(NoFingerprintDomain(host, hostLen))) {
    return f->data;
  }
  char *selectors = getElementHidingSelectors(host, hostLen);
  if (selectors) {
    buffer.append(selectors);
  }
  if (isGenericElementHidingEnabled) {
    if (buffer.length() > 0) {
      buffer.append(", ");
    }
    auto genericLen = static_cast<uint32_t>(strlen(genericElementHidingSelectors->data));
    if (genericLen > 0) {
      buffer.append(genericElementHidingSelectors->data);
    }
  }
  uint32_t len = buffer.length();
  char *combine = const_cast<char *>(buffer.c_str());
  char *exception = getElementHidingExceptionSelectors(host, hostLen);
  if (exception) {
    combine = removeException(combine, len, exception);
  }
  auto *filter = new CosmeticFilter(combine);
  elementHidingSelectorsCache->put(NoFingerprintDomain(host, hostLen), filter);
  delete[] selectors;
  delete[] exception;
  if (exception) {
    delete[] combine;
  }
  return filter->data;
}

const LinkedList<std::string> *getRulesFrom(HashMap<NoFingerprintDomain,
                                                    CosmeticFilterHashSet> *map,
                                            HashMap<NoFingerprintDomain,
                                                    LinkedList<std::string>> **cache,
                                            const char *contextUrl) {
  int urlLen = static_cast<int>(strlen(contextUrl));
  if (!isBlockableProtocol(contextUrl, urlLen)) {
    return nullptr;
  }
  int hostLen;
  const char *host = getUrlHost(contextUrl, &hostLen);
  if (!*cache) {
    *cache = new HashMap<NoFingerprintDomain, LinkedList<std::string>>(100);
  } else if (const LinkedList<std::string> *f = (*cache)->get(NoFingerprintDomain(host, hostLen))) {
    return f;
  }
  auto *result = new LinkedList<std::string>();
  auto onFind = [result](CosmeticFilterHashSet *set) { result->concat(set->toStringList()); };
  if (getFrom<CosmeticFilterHashSet>(map, host, hostLen, onFind)) {
    (*cache)->put(NoFingerprintDomain(host, hostLen), result);
  }
  return result;
}

const LinkedList<std::string> *AdBlockClient::getExtendedCssSelectors(const char *contextUrl) {
  return getRulesFrom(extendedCssMap, &extendedCssCache, contextUrl);
}

const LinkedList<std::string> *AdBlockClient::getCssRules(const char *contextUrl) {
  return getRulesFrom(cssRulesMap, &cssRulesCache, contextUrl);
}

const LinkedList<std::string> *AdBlockClient::getScriptlets(const char *contextUrl) {
  return getRulesFrom(scriptletMap, &scriptletCache, contextUrl);
}

bool extractScriptletArgsAsData(Filter &filter) {
  if (!filter.data) {
    return false;// fix nullptr when scriptlet rule data is empty
  }
  char *p = filter.data;
  char *q = filter.data + filter.dataLen;
  while (*p != '(' && *p != '\0') {
    p++;
  }
  p++;// skip '('
  while (*q != ')' && q != p) {
    q--;
  }
  // copy extracted data
  int len = static_cast<int>(q - p);
  if (len > 0) {
    char *args = new char[len + 1];
    args[len] = '\0';
    memcpy(args, p, len);
    delete[] filter.data;
    filter.data = args;
  }
  return true;
}

inline bool isFingerprintChar(char c) {
  return c != '|' && c != '*' && c != '^';
}

static HashSet<NoFingerprintDomain> createBadFingerprintsHashSet() {
  HashSet<NoFingerprintDomain> hashSet(15000, false);
  for (auto &badFingerprint : badFingerprints) {
    hashSet.Add(NoFingerprintDomain(badFingerprint, AdBlockClient::kFingerprintSize));
  }
  return hashSet;
}

// NoFingerprintDomain is used to keep BadFingerprint
// because it supports for borrowed data and has implemented hash function.
static HashSet<NoFingerprintDomain> staticBadFingerprintsHashSet = createBadFingerprintsHashSet();

bool isBadFingerprint(const char *fingerprint, const char *fingerprintEnd) {
  auto len = static_cast<uint32_t>(fingerprintEnd - fingerprint);
  return staticBadFingerprintsHashSet.Exists(NoFingerprintDomain(fingerprint, len));
}

bool hasBadSubstring(const char *fingerprint, const char *fingerprintEnd) {
  for (auto &badSubstring : badSubstrings) {
    const char *p = strstr(fingerprint, badSubstring);
    if (p && (p - fingerprint) + strlen(badSubstring)
        <= (unsigned int) (fingerprintEnd - fingerprint)) {
      return true;
    }
  }
  return false;
}

/**
 * Obtains a fingerprint for the specified filter
 */
bool AdBlockClient::getFingerprint(char *buffer, const char *input) {
  if (!input) {
    return false;
  }
  int size = 0;
  const char *p = input;
  const char *start = input;
  while (*p != '\0') {
    if (!isFingerprintChar(*p)) {
      size = 0;
      p++;
      start = p;
      continue;
    }
    if (buffer) {
      buffer[size] = *p;
    }
    if (hasBadSubstring(start, start + size + 1)) {
      size = 0;
      start++;
      p = start;
      continue;
    }
    size++;

    if (size == kFingerprintSize) {
      if (buffer) {
        buffer[size] = '\0';
      }
      if (isBadFingerprint(start, start + size)) {
        size = 0;
        start++;
        p = start;
        continue;
      }
      return true;
    }
    p++;
  }
  if (buffer) {
    buffer[0] = '\0';
  }
  return false;
}

bool AdBlockClient::getFingerprint(char *buffer, const Filter &f) {
  if (f.filterType & FTRegex) {
    // cout << "Get fingerprint for regex returning false; " << endl;
    return false;
  }

  if (f.filterType & FTHostAnchored) {
    if (AdBlockClient::getFingerprint(buffer, f.data + strlen(f.host))) {
      return true;
    }
  }

  bool b = AdBlockClient::getFingerprint(buffer, f.data);
  // if (!b && f.data) {
  //   cout << "No fingerprint for: " << f.data << endl;
  // }
  return b;
}

// Separator chars are one of: :?/=^;
signed char separatorBuffer[32] = {0, 0, 0, 0, 16, -128, 0, -92, 0, 0, 0, 64};
bool isSeparatorChar(char c) {
  return !!(separatorBuffer[(unsigned char) c / 8] & 1 << (unsigned char) c % 8);
}

int findFirstSeparatorChar(const char *input, const char *end) {
  const char *p = input;
  while (p != end) {
    if (isSeparatorChar(*p)) {
      return static_cast<int>(p - input);
    }
    p++;
  }
  return static_cast<int>(end - input);
}

void parseFilter(const char *input, Filter *f, BloomFilter *bloomFilter,
                 BloomFilter *exceptionBloomFilter,
                 HashSet<Filter> *hostAnchoredHashSet,
                 HashSet<Filter> *hostAnchoredExceptionHashSet,
                 HashSet<CosmeticFilter> *simpleCosmeticFilters,
                 bool preserveRules) {
  const char *end = input;
  while (*end != '\0')
    end++;
  parseFilter(input, end, f, bloomFilter, exceptionBloomFilter,
              hostAnchoredHashSet, hostAnchoredExceptionHashSet, simpleCosmeticFilters);
}

enum FilterParseState {
  FPStart,
  FPGenericException,
  FPOneBar,
  FPOneAt,
  FPData,
  // Same as data but won't consider any special char handling like | or $
  FPDataOnly
};

// Not currently multithreaded safe due to the static buffer named 'data'
void parseFilter(const char *input, const char *end, Filter *f,
                 BloomFilter *bloomFilter,
                 BloomFilter *exceptionBloomFilter,
                 HashSet<Filter> *hostAnchoredHashSet,
                 HashSet<Filter> *hostAnchoredExceptionHashSet,
                 HashSet<CosmeticFilter> *simpleCosmeticFilters,
                 bool preserveRules) {
  FilterParseState parseState = FPStart;
  const char *p = input;
  const char *filterRuleStart = p;
  const char *filterRuleEndPos = p;
  char data[kMaxLineLength];
  memset(data, 0, sizeof data);
  int i = 0;

  bool earlyBreak = false;
  while (p != end && !earlyBreak) {
    // Check for the filter being too long
    if ((p - input) >= kMaxLineLength - 1) {
      return;
    }

    if (parseState != FPDataOnly) {
      if (parseState == FPOneBar && *p != '|') {
        parseState = FPData;
        f->filterType = static_cast<FilterType>(f->filterType | FTLeftAnchored);
      }

      switch (*p) {
      case '|':
        if (parseState == FPStart || parseState == FPGenericException) {
          parseState = FPOneBar;
          filterRuleEndPos++;
          p++;
          continue;
        } else if (parseState == FPOneBar) {
          f->filterType = static_cast<FilterType>(f->filterType | FTHostAnchored);
          parseState = FPData;
          filterRuleEndPos++;
          p++;

          int len = findFirstSeparatorChar(p, end);
          // It's possible we have a host anchored filter
          // which also has a right anchored filter.
          if (len > 0 && p[len - 1] == '|') {
            len--;
          }
          f->host = new char[len + 1];
          f->host[len] = '\0';
          memcpy(f->host, p, len);

          if ((*(p + len) == '^' && (*(p + len + 1) == '\0'
              || *(p + len + 1) == '$' || isEndOfLine(*(p + len + 1)))) ||
              *(p + len) == '\0' || *(p + len) == '$' ||
              isEndOfLine(*(p + len))) {
            f->filterType = static_cast<FilterType>(f->filterType | FTHostOnly);
          }

          continue;
        } else {
          f->filterType = static_cast<FilterType>(f->filterType | FTRightAnchored);
          parseState = FPData;
          filterRuleEndPos++;
          p++;
          continue;
        }
        break;
      case '@':
        if (parseState == FPStart || parseState == FPGenericException) {
          parseState = FPOneAt;
          filterRuleEndPos++;
          p++;
          continue;
        } else if (parseState == FPOneAt) {
          f->filterType = FTException;
          parseState = FPGenericException;
          filterRuleEndPos++;
          p++;
          continue;
        }
        break;
      case '!':
      case '[':
        if (parseState == FPStart || parseState == FPGenericException) {
          f->filterType = FTComment;
          // We don't care about comments right now
          return;
        }
        break;
      case '\r':
      case '\n':
      case '\t':
      case ' ':
        // Skip leading whitespace
        if (parseState == FPStart) {
          filterRuleStart++;
          filterRuleEndPos++;
          p++;
          continue;
        }
        break;
      case '/': {
        if (parseState == FPStart || parseState == FPGenericException) {
          filterRuleEndPos = end;
          while (*filterRuleEndPos != '/') {
            filterRuleEndPos--;
          }
          int regexLen = filterRuleEndPos - p - 1;
          filterRuleEndPos++;

          if (regexLen > 0 && (filterRuleEndPos == end || *filterRuleEndPos == '$')) {
            f->data = new char[regexLen + 1];
            f->data[regexLen] = '\0';
            memcpy(f->data, p + 1, regexLen);

            f->filterType = FTRegex;
            p = filterRuleEndPos;
            parseState = FPData;
            continue;
          } else {
            // it isn't a regex rule, recovery and break
            filterRuleEndPos = p;
            parseState = FPData;
          }
        }
        break;
      }
      case '$':
        // Handle adguard HTML filtering rules syntax
        // e.g. example.org$$script[data-src="banner"]
        // see https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#html-filtering-rules-syntax-1
        if (*(p + 1) == '$') {
          if (i != 0) {
            f->domainList = new char[i + 1];
            memcpy(f->domainList, data, i + 1);
            i = 0;
          }
          parseState = FPDataOnly;
          f->filterType = FTHTMLFiltering;
          p += 2;
          filterRuleEndPos += 2;
          continue;
        }
        while (*filterRuleEndPos != '\0' && !isEndOfLine(*filterRuleEndPos)) {
          filterRuleEndPos++;
        }
        f->parseOptions(p + 1);
        earlyBreak = true;
        continue;
      case '#':
        // ublock uses some comments of the form #[space]
        if (parseState == FPStart || parseState == FPGenericException) {
          if (*(p + 1) == ' ') {
            f->filterType = FTComment;
            // We don't care about comments right now
            return;
          }
        }

        if (*(p + 1) == '@') {// exception rule
          switch (*(p + 2)) {
          case '#':// example.org#@#.banner
            f->filterType = FTElementHidingException;
            p += 3;
            break;
          case '$':// example.com#@$#.textad { visibility: hidden; }
            f->filterType = FTCssException;
            p += 4;
            break;
          case '?':// ignore
          case '%':// example.com#@%#window.__gaq = undefined;
          default:
            break;
          }
        } else {
          switch (*(p + 1)) {
          case '#':// example.org##.banner
            f->filterType = FTElementHiding;
            p += 2;
            break;
          case '$':// example.com#$#body { background-color: #333!important; }
            f->filterType = FTCss;
            p += 3;
            break;
          case '?':// https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#extended-css-selectors-1
            f->filterType = FTExtendedCss;
            p += 3;
            break;
          case '%':
            // example.org#%#//scriptlet("abort-on-property-read", "alert")
            if (*(p + 3) == '/' && *(p + 4) == '/') {
              f->filterType = FTScriptlet;
              p += 3;
            }
          default:
            break;
          }
        }
        if (*(p - 1) == '#') {
          if (f->filterType == FTNoFilterType) {
            return;// ignore unsupported rule
          }
          if (i != 0) {
            f->domainList = new char[i + 1];
            memcpy(f->domainList, data, i + 1);
            i = 0;
          }
          parseState = FPDataOnly;
          continue;
        }

        // Copied from default label to avoid warning (unannotated
        // fall-through between switch labels)
        parseState = FPData;
        break;
      default:
        parseState = FPData;
      }
    }
    data[i] = *p;
    i++;
    filterRuleEndPos++;
    p++;
  }

  if (parseState == FPStart) {
    f->filterType = FTEmpty;
    return;
  }

  if (preserveRules) {
    int ruleTextLength = filterRuleEndPos - filterRuleStart;
    f->ruleDefinition = new char[ruleTextLength + 1];
    memcpy(f->ruleDefinition, filterRuleStart, ruleTextLength);
    f->ruleDefinition[ruleTextLength] = '\0';
  }

  // regex rule has finished
  if (f->filterType == FTRegex) {
    return;
  }

  if (i > 0) {
    data[i] = '\0';
    f->data = new char[i + 1];
    memcpy(f->data, data, i + 1);
  } else {
    f->data = nullptr;
  }
  f->dataLen = i;

  char fingerprintBuffer[AdBlockClient::kFingerprintSize + 1];
  fingerprintBuffer[AdBlockClient::kFingerprintSize] = '\0';

  if (f->filterType == FTCss || f->filterType == FTCssException
      || f->filterType == FTScriptlet || f->filterType == FTExtendedCss) {
    return;// do nothing
  }
  if (f->filterType == FTElementHiding) {
    if (simpleCosmeticFilters && !f->domainList) {
      simpleCosmeticFilters->Add(CosmeticFilter(data));
    }
  } else if (f->filterType == FTElementHidingException) {
    if (simpleCosmeticFilters && !f->domainList) {
      simpleCosmeticFilters->Remove(CosmeticFilter(data));
    }
  } else if (exceptionBloomFilter
      && (f->filterType & FTException) && (f->filterType & FTHostOnly)) {
    // cout << "add host anchored exception bloom filter: " << f->host << endl;
    hostAnchoredExceptionHashSet->Add(*f);
  } else if (hostAnchoredHashSet && (f->filterType & FTHostOnly)) {
    // cout << "add host anchored bloom filter: " << f->host << endl;
    hostAnchoredHashSet->Add(*f);
  } else if (AdBlockClient::getFingerprint(fingerprintBuffer, *f)) {
    if (exceptionBloomFilter && f->filterType & FTException) {
      exceptionBloomFilter->add(fingerprintBuffer);
    } else if (bloomFilter) {
      // cout << "add fingerprint: " << fingerprintBuffer
      // << ", from string: " << f->data << endl;
      bloomFilter->add(fingerprintBuffer);
    }
  }
}

AdBlockClient::AdBlockClient() : filters(nullptr),
                                 htmlFilters(nullptr),
                                 exceptionFilters(nullptr),
                                 noFingerprintFilters(nullptr),
                                 noFingerprintExceptionFilters(nullptr),
                                 noFingerprintDomainOnlyFilters(nullptr),
                                 noFingerprintAntiDomainOnlyFilters(nullptr),
                                 noFingerprintDomainOnlyExceptionFilters(nullptr),
                                 noFingerprintAntiDomainOnlyExceptionFilters(nullptr),
                                 numFilters(0),
                                 numCosmeticFilters(0),
                                 numHtmlFilters(0),
                                 numScriptletFilters(0),
                                 numExceptionFilters(0),
                                 numNoFingerprintFilters(0),
                                 numNoFingerprintExceptionFilters(0),
                                 numNoFingerprintDomainOnlyFilters(0),
                                 numNoFingerprintAntiDomainOnlyFilters(0),
                                 numNoFingerprintDomainOnlyExceptionFilters(0),
                                 numNoFingerprintAntiDomainOnlyExceptionFilters(0),
                                 numHostAnchoredFilters(0),
                                 numHostAnchoredExceptionFilters(0),
                                 bloomFilter(nullptr),
                                 exceptionBloomFilter(nullptr),
                                 hostAnchoredHashSet(nullptr),
                                 hostAnchoredExceptionHashSet(nullptr),
                                 noFingerprintDomainHashSet(nullptr),
                                 noFingerprintAntiDomainHashSet(nullptr),
                                 noFingerprintDomainExceptionHashSet(nullptr),
                                 noFingerprintAntiDomainExceptionHashSet(nullptr),
                                 badFingerprintsHashSet(nullptr),
                                 numFalsePositives(0),
                                 numExceptionFalsePositives(0),
                                 numBloomFilterSaves(0),
                                 numExceptionBloomFilterSaves(0),
                                 numHashSetSaves(0),
                                 numExceptionHashSetSaves(0),
                                 deserializedBuffer(nullptr),
                                 elementHidingSelectorHashMap(nullptr),
                                 elementHidingExceptionSelectorHashMap(nullptr),
                                 genericElementHidingSelectors(nullptr),
                                 elementHidingSelectorsCache(nullptr),
                                 isGenericElementHidingEnabled(false),
                                 extendedCssMap(nullptr),
                                 extendedCssCache(nullptr),
                                 cssRulesMap(nullptr),
                                 cssRulesCache(nullptr),
                                 scriptletMap(nullptr),
                                 scriptletCache(nullptr) {
}

AdBlockClient::~AdBlockClient() {
  clear();
}

// Clears all data and stats from the AdBlockClient
void AdBlockClient::clear() {
  if (filters) {
    delete[] filters;
    filters = nullptr;
  }
  if (htmlFilters) {
    delete[] htmlFilters;
    htmlFilters = nullptr;
  }
  if (exceptionFilters) {
    delete[] exceptionFilters;
    exceptionFilters = nullptr;
  }
  if (noFingerprintFilters) {
    delete[] noFingerprintFilters;
    noFingerprintFilters = nullptr;
  }
  if (noFingerprintExceptionFilters) {
    delete[] noFingerprintExceptionFilters;
    noFingerprintExceptionFilters = nullptr;
  }
  if (noFingerprintDomainOnlyFilters) {
    delete[] noFingerprintDomainOnlyFilters;
    noFingerprintDomainOnlyFilters = nullptr;
  }
  if (noFingerprintAntiDomainOnlyFilters) {
    delete[] noFingerprintAntiDomainOnlyFilters;
    noFingerprintAntiDomainOnlyFilters = nullptr;
  }
  if (noFingerprintDomainOnlyExceptionFilters) {
    delete[] noFingerprintDomainOnlyExceptionFilters;
    noFingerprintDomainOnlyExceptionFilters = nullptr;
  }
  if (noFingerprintAntiDomainOnlyExceptionFilters) {
    delete[] noFingerprintAntiDomainOnlyExceptionFilters;
    noFingerprintAntiDomainOnlyExceptionFilters = nullptr;
  }
  if (bloomFilter) {
    delete bloomFilter;
    bloomFilter = nullptr;
  }
  if (exceptionBloomFilter) {
    delete exceptionBloomFilter;
    exceptionBloomFilter = nullptr;
  }
  if (hostAnchoredHashSet) {
    delete hostAnchoredHashSet;
    hostAnchoredHashSet = nullptr;
  }
  if (hostAnchoredExceptionHashSet) {
    delete hostAnchoredExceptionHashSet;
    hostAnchoredExceptionHashSet = nullptr;
  }
  if (noFingerprintDomainHashSet) {
    delete noFingerprintDomainHashSet;
    noFingerprintDomainHashSet = nullptr;
  }
  if (noFingerprintAntiDomainHashSet) {
    delete noFingerprintAntiDomainHashSet;
    noFingerprintAntiDomainHashSet = nullptr;
  }
  if (noFingerprintDomainExceptionHashSet) {
    delete noFingerprintDomainExceptionHashSet;
    noFingerprintDomainExceptionHashSet = nullptr;
  }
  if (noFingerprintAntiDomainExceptionHashSet) {
    delete noFingerprintAntiDomainExceptionHashSet;
    noFingerprintAntiDomainExceptionHashSet = nullptr;
  }
  if (badFingerprintsHashSet) {
    delete badFingerprintsHashSet;
    badFingerprintsHashSet = nullptr;
  }
  if (elementHidingSelectorHashMap) {
    delete elementHidingSelectorHashMap;
    elementHidingSelectorHashMap = nullptr;
  }
  if (elementHidingExceptionSelectorHashMap) {
    delete elementHidingExceptionSelectorHashMap;
    elementHidingExceptionSelectorHashMap = nullptr;
  }
  if (genericElementHidingSelectors) {
    delete genericElementHidingSelectors;
    genericElementHidingSelectors = nullptr;
  }
  if (elementHidingSelectorsCache) {
    delete elementHidingSelectorsCache;
    elementHidingSelectorsCache = nullptr;
  }
  if (extendedCssMap) {
    delete extendedCssMap;
    extendedCssMap = nullptr;
  }
  if (extendedCssCache) {
    delete extendedCssCache;
    extendedCssCache = nullptr;
  }
  if (cssRulesMap) {
    delete cssRulesMap;
    cssRulesMap = nullptr;
  }
  if (cssRulesCache) {
    delete cssRulesCache;
    cssRulesCache = nullptr;
  }
  if (scriptletMap) {
    delete scriptletMap;
    scriptletMap = nullptr;
  }
  if (scriptletCache) {
    delete scriptletCache;
    scriptletCache = nullptr;
  }

  numFilters = 0;
  numCosmeticFilters = 0;
  numHtmlFilters = 0;
  numScriptletFilters = 0;
  numExceptionFilters = 0;
  numNoFingerprintFilters = 0;
  numNoFingerprintExceptionFilters = 0;
  numNoFingerprintDomainOnlyFilters = 0;
  numNoFingerprintAntiDomainOnlyFilters = 0;
  numNoFingerprintDomainOnlyExceptionFilters = 0;
  numNoFingerprintAntiDomainOnlyExceptionFilters = 0;
  numHostAnchoredFilters = 0;
  numHostAnchoredExceptionFilters = 0;
  numFalsePositives = 0;
  numExceptionFalsePositives = 0;
  numBloomFilterSaves = 0;
  numExceptionBloomFilterSaves = 0;
  numHashSetSaves = 0;
  numExceptionHashSetSaves = 0;
}

bool AdBlockClient::hasMatchingFilters(Filter *filter, int numFilters,
                                       const char *input,
                                       int inputLen,
                                       FilterOption contextOption,
                                       const char *contextDomain,
                                       BloomFilter *inputBloomFilter,
                                       const char *inputHost,
                                       int inputHostLen,
                                       Filter **matchingFilter) const {
  for (int i = 0; i < numFilters; i++) {
    if (filter->matches(input, inputLen, contextOption,
                        contextDomain, inputBloomFilter, inputHost, inputHostLen)) {
      if (filter->tagLen == 0 || tagExists(std::string(filter->tag, filter->tagLen))) {
        if (matchingFilter) {
          *matchingFilter = filter;
        }
        return true;
      }
    }
    filter++;
  }
  if (matchingFilter) {
    *matchingFilter = nullptr;
  }
  return false;
}

void discoverMatchingPrefix(BadFingerprintsHashSet *badFingerprintsHashSet,
                            const char *str,
                            BloomFilter *bloomFilter,
                            int prefixLen = AdBlockClient::kFingerprintSize) {
  char sz[32];
  memset(sz, 0, sizeof(sz));
  int strLen = static_cast<int>(strlen(str));
  for (int i = 0; i < strLen - prefixLen + 1; i++) {
    if (bloomFilter->exists(str + i, prefixLen)) {
      memcpy(sz, str + i, prefixLen);
      // cout <<  "Bad fingerprint: " << sz << endl;
      if (badFingerprintsHashSet) {
        badFingerprintsHashSet->Add(BadFingerprint(sz));
      }
      // We only want the first bad fingerprint since that's the one
      // that led us here.
      // If you do all bad fingerprint detection here it will lead to too many
      // bad fingerprints, which then leads to too many no fingerprint rules.
      // And too many no fingerprint rules causes perf problems.
      return;
    }
    // memcpy(sz, str + i, prefixLen);
    // cout <<  "Good fingerprint: " << sz;
  }
}

bool isNoFingerprintDomainHashSetMiss(HashSet<NoFingerprintDomain> *hashSet,
                                      const char *host, int hostLen) {
  if (!hashSet) {
    return false;
  }
  const char *start = host + hostLen;
  // Skip past the TLD
  while (start != host) {
    start--;
    if (*(start) == '.') {
      break;
    }
  }
  while (start != host) {
    if (*(start - 1) == '.') {
      if (hashSet->Find(NoFingerprintDomain(start,
                                            static_cast<int>(host + hostLen - start)))) {
        return false;
      }
    }
    start--;
  }
  return !hashSet->Find(NoFingerprintDomain(host, hostLen));
}

bool AdBlockClient::isHostAnchoredHashSetMiss(const char *input, int inputLen,
                                              HashSet<Filter> *hashSet,
                                              const char *inputHost,
                                              int inputHostLen,
                                              FilterOption contextOption,
                                              const char *contextDomain,
                                              Filter **foundFilter) const {
  if (!hashSet) {
    return false;
  }

  const char *start = inputHost + inputHostLen;
  // Skip past the TLD
  while (start != inputHost) {
    start--;
    if (*(start) == '.') {
      break;
    }
  }

  while (start != inputHost) {
    if (*(start - 1) == '.') {
      Filter *filter = hashSet->Find(Filter(start,
                                            static_cast<int>(inputHost + inputHostLen - start),
                                            nullptr, start, inputHostLen - (start - inputHost)));
      if (filter && filter->matches(input, inputLen, contextOption, contextDomain)) {
        if (filter->tagLen == 0 ||
            tagExists(std::string(filter->tag, filter->tagLen))) {
          if (foundFilter) {
            *foundFilter = filter;
          }
          return false;
        }
      }
    }
    start--;
  }

  Filter *filter = hashSet->Find(Filter(start,
                                        static_cast<int>(inputHost + inputHostLen - start), nullptr,
                                        start, inputHostLen));
  if (!filter) {
    return true;
  }
  bool result = !filter->matches(input, inputLen, contextOption, contextDomain);
  if (!result) {
    if (filter->tagLen > 0 &&
        !tagExists(std::string(filter->tag, filter->tagLen))) {
      return true;
    }
    if (foundFilter) {
      *foundFilter = filter;
    }
  }
  return result;
}

bool AdBlockClient::matches(const char *input, FilterOption contextOption,
                            const char *contextDomain, Filter **matchedFilter,
                            Filter **matchedExceptionFilter) {
  if (matchedFilter) {
    *matchedFilter = nullptr;
  }
  if (matchedExceptionFilter) {
    *matchedExceptionFilter = nullptr;
  }
  int inputLen = static_cast<int>(strlen(input));

  if (!isBlockableProtocol(input, inputLen)) {
    return false;
  }

  int inputHostLen;
  const char *inputHost = getUrlHost(input, &inputHostLen);

  int contextDomainLen = 0;
  if (contextDomain) {
    contextDomainLen = static_cast<int>(strlen(contextDomain));
  }
  // If neither first party nor third party was specified, try to figure it out
  if (contextDomain && !(contextOption & (FOThirdParty | FONotThirdParty))) {
    if (isThirdPartyHost(contextDomain, contextDomainLen,
                         inputHost, static_cast<int>(inputHostLen))) {
      contextOption = static_cast<FilterOption>(contextOption | FOThirdParty);
    } else {
      contextOption = static_cast<FilterOption>(contextOption | FONotThirdParty);
    }
  }

  // Optimization for the manual filter checks which are needed.
  // Avoid having to check individual filters if the filter parts are not found
  // inside the input bloom filter.
  HashFn2Byte hashFns[] = {hashFn2Byte};
  BloomFilter inputBloomFilter(10, 1024, hashFns, 1);
  for (int i = 1; i < inputLen; i++) {
    inputBloomFilter.add(input + i - 1, 2);
  }

  // We always have to check noFingerprintFilters because the bloom filter opt
  // cannot be used for them
  bool hasMatch = false;

  // Only bother checking the no fingerprint domain related filters if needed
  if (!isNoFingerprintDomainHashSetMiss(
      noFingerprintDomainHashSet, contextDomain, contextDomainLen)) {
    hasMatch = hasMatch || hasMatchingFilters(noFingerprintDomainOnlyFilters,
                                              numNoFingerprintDomainOnlyFilters, input, inputLen,
                                              contextOption,
                                              contextDomain, &inputBloomFilter, inputHost,
                                              inputHostLen,
                                              matchedFilter);
  }
  if (isNoFingerprintDomainHashSetMiss(
      noFingerprintAntiDomainHashSet, contextDomain, contextDomainLen)) {
    hasMatch = hasMatch ||
        hasMatchingFilters(noFingerprintAntiDomainOnlyFilters,
                           numNoFingerprintAntiDomainOnlyFilters, input, inputLen,
                           contextOption,
                           contextDomain, &inputBloomFilter, inputHost, inputHostLen,
                           matchedFilter);
  }

  // If no noFingerprintFilters were hit, check the bloom filter substring
  // fingerprint for the normal filter list.
  if (!hasMatch) {
    bool bloomFilterMiss = bloomFilter
        && !bloomFilter->substringExists(input, AdBlockClient::kFingerprintSize);
    bool hostAnchoredHashSetMiss = isHostAnchoredHashSetMiss(input, inputLen,
                                                        hostAnchoredHashSet, inputHost,
                                                        inputHostLen,
                                                        contextOption, contextDomain,
                                                        matchedFilter);
    if (bloomFilterMiss && hostAnchoredHashSetMiss) {
      if (bloomFilterMiss) {
        numBloomFilterSaves++;
      }
      if (hostAnchoredHashSetMiss) {
        numHashSetSaves++;
      }
    }

    if (!hostAnchoredHashSetMiss) {
      numHashSetSaves++;
      hasMatch = true;
    }

    // We need to check the filters list manually because there is either a match
    // or a false positive
    if (hostAnchoredHashSetMiss && !bloomFilterMiss) {
      hasMatch = hasMatchingFilters(filters, numFilters, input, inputLen,
                                    contextOption, contextDomain, &inputBloomFilter,
                                    inputHost, inputHostLen, matchedFilter);
      // If there's still no match after checking the block filters, then no need
      // to try to block this because there is a false positive.
      if (!hasMatch) {
        numFalsePositives++;
        if (badFingerprintsHashSet) {
          // cout << "false positive for input: " << input << " bloomFilterMiss: "
          // << bloomFilterMiss << ", hostAnchoredHashSetMiss: "
          // << hostAnchoredHashSetMiss << endl;
          discoverMatchingPrefix(badFingerprintsHashSet, input, bloomFilter);
        }
      }
    }
  }

  // Iteration at the end can increase efficiency.
  hasMatch = hasMatch || hasMatchingFilters(noFingerprintFilters,
                                            numNoFingerprintFilters, input, inputLen, contextOption,
                                            contextDomain, &inputBloomFilter, inputHost,
                                            inputHostLen,
                                            matchedFilter);

  bool hasExceptionMatch = false;

  // Only bother checking the no fingerprint domain related filters if needed
  if (!isNoFingerprintDomainHashSetMiss(
      noFingerprintDomainExceptionHashSet, contextDomain, contextDomainLen)) {
    hasExceptionMatch = hasExceptionMatch ||
        hasMatchingFilters(noFingerprintDomainOnlyExceptionFilters,
                           numNoFingerprintDomainOnlyExceptionFilters, input,
                           inputLen,
                           contextOption, contextDomain, &inputBloomFilter,
                           inputHost,
                           inputHostLen, matchedExceptionFilter);
  }

  if (isNoFingerprintDomainHashSetMiss(
      noFingerprintAntiDomainExceptionHashSet, contextDomain,
      contextDomainLen)) {
    hasExceptionMatch = hasExceptionMatch ||
        hasMatchingFilters(noFingerprintAntiDomainOnlyExceptionFilters,
                           numNoFingerprintAntiDomainOnlyExceptionFilters, input,
                           inputLen,
                           contextOption, contextDomain, &inputBloomFilter,
                           inputHost, inputHostLen,
                           matchedExceptionFilter);
  }

  // If there's a matching no fingerprint exception then we can just return
  // right away because we shouldn't block
  if (!hasExceptionMatch) {
    bool bloomExceptionFilterMiss = exceptionBloomFilter
        && !exceptionBloomFilter->substringExists(input, AdBlockClient::kFingerprintSize);
    bool hostAnchoredExceptionHashSetMiss =
        isHostAnchoredHashSetMiss(input, inputLen, hostAnchoredExceptionHashSet,
                                  inputHost, inputHostLen, contextOption, contextDomain,
                                  matchedExceptionFilter);

    if (bloomExceptionFilterMiss && hostAnchoredExceptionHashSetMiss) {
      if (bloomExceptionFilterMiss) {
        numExceptionBloomFilterSaves++;
      }
      if (hostAnchoredExceptionHashSetMiss) {
        numExceptionHashSetSaves++;
      }
    }

    if (!hostAnchoredExceptionHashSetMiss) {
      numExceptionHashSetSaves++;
      hasExceptionMatch = true;
    }

    if (hostAnchoredExceptionHashSetMiss && !bloomExceptionFilterMiss) {
      hasExceptionMatch = hasMatchingFilters(exceptionFilters, numExceptionFilters, input,
                                             inputLen, contextOption, contextDomain,
                                             &inputBloomFilter, inputHost, inputHostLen,
                                             matchedExceptionFilter);
      if (!hasExceptionMatch) {
        // False positive on the exception filter list
        numExceptionFalsePositives++;
        // cout << "exception false positive for input: " << input << endl;
        if (badFingerprintsHashSet) {
          discoverMatchingPrefix(badFingerprintsHashSet,
                                 input, exceptionBloomFilter);
        }
      }
    }
  }

  // Iteration at the end can increase efficiency.
  hasExceptionMatch = hasExceptionMatch ||
      hasMatchingFilters(noFingerprintExceptionFilters,
                         numNoFingerprintExceptionFilters, input, inputLen,
                         contextOption,
                         contextDomain, &inputBloomFilter, inputHost, inputHostLen,
                         matchedExceptionFilter);

  // If rule definitions have not been saved, use fallback.
  if (*matchedFilter && !(*matchedFilter)->ruleDefinition) {
    (*matchedFilter)->ruleDefinition = rule_definition_fallback;
  }
  if (*matchedExceptionFilter && !(*matchedExceptionFilter)->ruleDefinition) {
    (*matchedExceptionFilter)->ruleDefinition = rule_definition_fallback;
  }

  return hasMatch && !hasExceptionMatch;
}

/**
 * Obtains the first matching filter or nullptr, and if one is found, finds
 * the first matching exception filter or nullptr.
 *
 * @return true if the filter should be blocked
 */
bool AdBlockClient::findMatchingFilters(const char *input,
                                        FilterOption contextOption,
                                        const char *contextDomain,
                                        Filter **matchingFilter,
                                        Filter **matchingExceptionFilter) {
  *matchingFilter = nullptr;
  *matchingExceptionFilter = nullptr;
  int inputLen = static_cast<int>(strlen(input));
  int inputHostLen;
  const char *inputHost = getUrlHost(input, &inputHostLen);

  int contextDomainLen = 0;
  if (contextDomain) {
    contextDomainLen = static_cast<int>(strlen(contextDomain));
  }
  // If neither first party nor third party was specified, try to figure it out
  if (contextDomain && !(contextOption & (FOThirdParty | FONotThirdParty))) {
    if (isThirdPartyHost(contextDomain, contextDomainLen,
                         inputHost, static_cast<int>(inputHostLen))) {
      contextOption = static_cast<FilterOption>(contextOption | FOThirdParty);
    } else {
      contextOption = static_cast<FilterOption>(contextOption | FONotThirdParty);
    }
  }

  hasMatchingFilters(noFingerprintFilters,
                     numNoFingerprintFilters, input, inputLen, contextOption,
                     contextDomain, nullptr,
                     inputHost, inputHostLen, matchingFilter);

  if (!*matchingFilter) {
    hasMatchingFilters(noFingerprintDomainOnlyFilters,
                       numNoFingerprintDomainOnlyFilters, input, inputLen, contextOption,
                       contextDomain, nullptr,
                       inputHost, inputHostLen, matchingFilter);
  }
  if (!*matchingFilter) {
    hasMatchingFilters(noFingerprintAntiDomainOnlyFilters,
                       numNoFingerprintAntiDomainOnlyFilters, input, inputLen, contextOption,
                       contextDomain, nullptr,
                       inputHost, inputHostLen, matchingFilter);
  }

  if (!*matchingFilter) {
    hasMatchingFilters(filters,
                       numFilters, input, inputLen, contextOption,
                       contextDomain, nullptr,
                       inputHost, inputHostLen, matchingFilter);
  }

  if (!*matchingFilter) {
    isHostAnchoredHashSetMiss(input, inputLen,
                              hostAnchoredHashSet, inputHost, inputHostLen,
                              contextOption, contextDomain, matchingFilter);
  }

  if (!*matchingFilter) {
    return false;
  }

  hasMatchingFilters(noFingerprintExceptionFilters,
                     numNoFingerprintExceptionFilters, input, inputLen, contextOption,
                     contextDomain,
                     nullptr, inputHost, inputHostLen, matchingExceptionFilter);

  if (!*matchingExceptionFilter) {
    hasMatchingFilters(noFingerprintDomainOnlyExceptionFilters,
                       numNoFingerprintDomainOnlyExceptionFilters, input, inputLen,
                       contextOption, contextDomain, nullptr, inputHost, inputHostLen,
                       matchingExceptionFilter);
  }

  if (!*matchingExceptionFilter) {
    hasMatchingFilters(noFingerprintAntiDomainOnlyExceptionFilters,
                       numNoFingerprintAntiDomainOnlyExceptionFilters, input, inputLen,
                       contextOption, contextDomain, nullptr, inputHost, inputHostLen,
                       matchingExceptionFilter);
  }

  if (!*matchingExceptionFilter) {
    isHostAnchoredHashSetMiss(input, inputLen, hostAnchoredExceptionHashSet,
                              inputHost, inputHostLen, contextOption, contextDomain,
                              matchingExceptionFilter);
  }

  if (!*matchingExceptionFilter) {
    hasMatchingFilters(exceptionFilters,
                       numExceptionFilters, input, inputLen, contextOption,
                       contextDomain,
                       nullptr, inputHost, inputHostLen, matchingExceptionFilter);
  }
  return !*matchingExceptionFilter;
}

void AdBlockClient::initBloomFilter(BloomFilter **pp,
                                    const char *buffer, int len) {
  if (*pp) {
    delete *pp;
  }
  if (len > 0) {
    *pp = new BloomFilter(buffer, len);
  }
}

template<class T>
bool AdBlockClient::initHashSet(HashSet<T> **pp, char *buffer, int len) {
  if (*pp) {
    delete *pp;
  }
  if (len > 0) {
    *pp = new HashSet<T>(0, false);

    return (*pp)->Deserialize(buffer, len);
  }

  return true;
}

template<class K, class V>
bool AdBlockClient::initHashMap(HashMap<K, V> **pp, char *buffer, int len) {
  if (*pp) {
    delete *pp;
  }
  if (len > 0) {
    *pp = new HashMap<K, V>(0);

    return (*pp)->Deserialize(buffer, len);
  }

  return true;
}

// Parses the filter data into a few collections of filters and enables
// efficient querying.
bool AdBlockClient::parse(const char *input, bool preserveRules) {
  // If the user is parsing and we have regex support,
  // then we can determine the fingerprints for the bloom filter.
  // Otherwise it needs to be done manually via initBloomFilter and
  // initExceptionBloomFilter
  if (!bloomFilter) {
    bloomFilter = new BloomFilter(15, 80000);
  }
  if (!exceptionBloomFilter) {
    exceptionBloomFilter = new BloomFilter(10, 20000);
  }
  if (!hostAnchoredHashSet) {
    // Optimized to be 1:1 with the easylist / easyprivacy
    // number of host anchored hosts.
    hostAnchoredHashSet = new HashSet<Filter>(18000, false);
  }
  if (!hostAnchoredExceptionHashSet) {
    // Optimized to be 1:1 with the easylist / easyprivacy
    // number of host anchored exception hosts.
    hostAnchoredExceptionHashSet = new HashSet<Filter>(2000, false);
  }
  if (!noFingerprintDomainHashSet) {
    noFingerprintDomainHashSet = new HashSet<NoFingerprintDomain>(1000, false);
  }
  if (!noFingerprintAntiDomainHashSet) {
    noFingerprintAntiDomainHashSet =
        new HashSet<NoFingerprintDomain>(100, false);
  }
  if (!noFingerprintDomainExceptionHashSet) {
    noFingerprintDomainExceptionHashSet =
        new HashSet<NoFingerprintDomain>(1000, false);
  }
  if (!noFingerprintAntiDomainExceptionHashSet) {
    noFingerprintAntiDomainExceptionHashSet =
        new HashSet<NoFingerprintDomain>(100, false);
  }

  int newNumFilters = 0;
  int newNumCosmeticFilters = 0;
  int newNumHtmlFilters = 0;
  int newNumScriptletFilters = 0;
  int newNumExceptionFilters = 0;
  int newNumNoFingerprintFilters = 0;
  int newNumNoFingerprintExceptionFilters = 0;
  int newNumNoFingerprintDomainOnlyFilters = 0;
  int newNumNoFingerprintAntiDomainOnlyFilters = 0;
  int newNumNoFingerprintDomainOnlyExceptionFilters = 0;
  int newNumNoFingerprintAntiDomainOnlyExceptionFilters = 0;
  int newNumHostAnchoredFilters = 0;
  int newNumHostAnchoredExceptionFilters = 0;

  int numCssRules = 0;
  int numExtendedCss = 0;

  // Simple cosmetic filters apply to all sites without exception
  CosmeticFilterHashSet genericCosmeticFilters(1000);

  // record parsing results in a linked list
  LinkedList<Filter> filterList;

  const char *lineStart = input;
  const char *p = lineStart + 1;

  while (true) {
    if ((isEndOfLine(*p) || *p == '\0') && p > lineStart) {
      Filter f;
      parseFilter(lineStart, p, &f, bloomFilter, exceptionBloomFilter,
                  hostAnchoredHashSet,
                  hostAnchoredExceptionHashSet,
                  &genericCosmeticFilters,
                  preserveRules);
      if (f.isValid()) {
        filterList.push_back(f);
        switch (f.filterType & FTListTypesMask) {
        case FTException:
          if (f.filterType & FTHostOnly) {
            newNumHostAnchoredExceptionFilters++;
          } else if (AdBlockClient::getFingerprint(nullptr, f)) {
            newNumExceptionFilters++;
          } else if (f.isDomainOnlyFilter()) {
            newNumNoFingerprintDomainOnlyExceptionFilters++;
          } else if (f.isAntiDomainOnlyFilter()) {
            newNumNoFingerprintAntiDomainOnlyExceptionFilters++;
          } else {
            newNumNoFingerprintExceptionFilters++;
          }
          break;
        case FTExtendedCss:
          numExtendedCss++;// ExtendedCss belong to Element Hiding
          // without break
        case FTElementHiding:
        case FTElementHidingException:
          newNumCosmeticFilters++;
          break;
        case FTCss:
        case FTCssException:
          numCssRules++;
          newNumCosmeticFilters++;
          break;
        case FTHTMLFiltering:
          newNumHtmlFilters++;
          break;
        case FTScriptlet:
          newNumScriptletFilters++;
          break;
        case FTEmpty:
        case FTComment:
          // No need to store comments
          break;
        default:
          if (f.filterType & FTHostOnly) {
            newNumHostAnchoredFilters++;
          } else if (AdBlockClient::getFingerprint(nullptr, f)) {
            newNumFilters++;
          } else if (f.isDomainOnlyFilter()) {
            newNumNoFingerprintDomainOnlyFilters++;
          } else if (f.isAntiDomainOnlyFilter()) {
            newNumNoFingerprintAntiDomainOnlyFilters++;
          } else {
            newNumNoFingerprintFilters++;
          }
          break;
        }
      }
      lineStart = p + 1;
    }

    if (*p == '\0') {
      break;
    }

    p++;
  }

#ifdef PERF_STATS
  cout << "Fingerprint size: " << AdBlockClient::kFingerprintSize << endl;
  cout << "Num new filters: " << newNumFilters << endl;
  cout << "Num new cosmetic filters: " << newNumCosmeticFilters << endl;
  cout << "Num new HTML filters: " << newNumHtmlFilters << endl;
  cout << "Num new exception filters: " << newNumExceptionFilters << endl;
  cout << "Num new no fingerprint filters: "
    << newNumNoFingerprintFilters << endl;
  cout << "Num new no fingerprint exception filters: "
    << newNumNoFingerprintExceptionFilters << endl;
  cout << "Num new host anchored filters: "
    << newNumHostAnchoredFilters << endl;
  cout << "Num new host anchored exception filters: "
    << newNumHostAnchoredExceptionFilters << endl;
  cout << "Num new no fingerprint domain only filters: "
    << newNumNoFingerprintDomainOnlyFilters << endl;
  cout << "Num new no fingerprint anti-domain only filters: "
    << newNumNoFingerprintAntiDomainOnlyFilters << endl;
  cout << "Num new no fingerprint domain only exception filters: "
    << newNumNoFingerprintDomainOnlyExceptionFilters << endl;
  cout << "Num new no fingerprint anti-domain only exception filters: "
    << newNumNoFingerprintAntiDomainOnlyExceptionFilters << endl;
#endif

  auto *newFilters = new Filter[newNumFilters + numFilters];
  auto *newHtmlFilters =
      new Filter[newNumHtmlFilters + numHtmlFilters];
  auto *newExceptionFilters =
      new Filter[newNumExceptionFilters + numExceptionFilters];
  auto *newNoFingerprintFilters =
      new Filter[newNumNoFingerprintFilters + numNoFingerprintFilters];
  auto *newNoFingerprintExceptionFilters =
      new Filter[newNumNoFingerprintExceptionFilters
          + numNoFingerprintExceptionFilters];
  auto *newNoFingerprintDomainOnlyFilters =
      new Filter[newNumNoFingerprintDomainOnlyFilters +
          numNoFingerprintDomainOnlyFilters];
  auto *newNoFingerprintAntiDomainOnlyFilters =
      new Filter[newNumNoFingerprintAntiDomainOnlyFilters +
          numNoFingerprintAntiDomainOnlyFilters];
  auto *newNoFingerprintDomainOnlyExceptionFilters =
      new Filter[newNumNoFingerprintDomainOnlyExceptionFilters
          + numNoFingerprintDomainOnlyExceptionFilters];
  auto *newNoFingerprintAntiDomainOnlyExceptionFilters =
      new Filter[newNumNoFingerprintAntiDomainOnlyExceptionFilters
          + numNoFingerprintAntiDomainOnlyExceptionFilters];

  Filter *curFilters = newFilters;
  Filter *curHtmlFilters = newHtmlFilters;
  Filter *curExceptionFilters = newExceptionFilters;
  Filter *curNoFingerprintFilters = newNoFingerprintFilters;
  Filter *curNoFingerprintExceptionFilters = newNoFingerprintExceptionFilters;
  Filter *curNoFingerprintDomainOnlyFilters = newNoFingerprintDomainOnlyFilters;
  Filter *curNoFingerprintAntiDomainOnlyFilters =
      newNoFingerprintAntiDomainOnlyFilters;
  Filter *curNoFingerprintDomainOnlyExceptionFilters =
      newNoFingerprintDomainOnlyExceptionFilters;
  Filter *curNoFingerprintAntiDomainOnlyExceptionFilters =
      newNoFingerprintAntiDomainOnlyExceptionFilters;

  // If we've had a parse before copy the old data into the new data structure
  if (filters || htmlFilters || exceptionFilters ||
      noFingerprintFilters || noFingerprintExceptionFilters ||
      noFingerprintDomainOnlyFilters ||
      noFingerprintDomainOnlyExceptionFilters ||
      noFingerprintAntiDomainOnlyFilters ||
      noFingerprintAntiDomainOnlyExceptionFilters) {
    // - Copy the old data in, we can't simply use memcpy here
    // since filters manages some pointers that get deleted.
    // - Set the old filter lists borrowedMemory to true
    // since it'll be taken by the new filters.
    for (int i = 0; i < numFilters; i++) {
      newFilters[i].swapData(&(filters[i]));
      filters[i].borrowed_data = true;
    }
    for (int i = 0; i < numHtmlFilters; i++) {
      newHtmlFilters[i].swapData(&(htmlFilters[i]));
      htmlFilters[i].borrowed_data = true;
    }
    for (int i = 0; i < numExceptionFilters; i++) {
      newExceptionFilters[i].swapData(&(exceptionFilters[i]));
      exceptionFilters[i].borrowed_data = true;
    }
    for (int i = 0; i < numNoFingerprintFilters; i++) {
      newNoFingerprintFilters[i].swapData(&(noFingerprintFilters[i]));
      noFingerprintFilters[i].borrowed_data = true;
    }
    for (int i = 0; i < numNoFingerprintExceptionFilters; i++) {
      newNoFingerprintExceptionFilters[i].swapData(
          &(noFingerprintExceptionFilters[i]));
      noFingerprintExceptionFilters[i].borrowed_data = true;
    }
    for (int i = 0; i < numNoFingerprintDomainOnlyFilters; i++) {
      newNoFingerprintDomainOnlyFilters[i].swapData(
          &(noFingerprintDomainOnlyFilters[i]));
      noFingerprintDomainOnlyFilters[i].borrowed_data = true;
    }
    for (int i = 0; i < numNoFingerprintAntiDomainOnlyFilters; i++) {
      newNoFingerprintAntiDomainOnlyFilters[i].swapData(
          &(noFingerprintAntiDomainOnlyFilters[i]));
      noFingerprintAntiDomainOnlyFilters[i].borrowed_data = true;
    }
    for (int i = 0; i < numNoFingerprintDomainOnlyExceptionFilters; i++) {
      newNoFingerprintDomainOnlyExceptionFilters[i].swapData(
          &(noFingerprintDomainOnlyExceptionFilters[i]));
      noFingerprintDomainOnlyExceptionFilters[i].borrowed_data = true;
    }
    for (int i = 0; i < numNoFingerprintAntiDomainOnlyExceptionFilters; i++) {
      newNoFingerprintAntiDomainOnlyExceptionFilters[i].swapData(
          &(noFingerprintAntiDomainOnlyExceptionFilters[i]));
      noFingerprintAntiDomainOnlyExceptionFilters[i].borrowed_data = true;
    }

    // Free up the old memory for filter storage
    delete[] filters;
    delete[] htmlFilters;
    delete[] exceptionFilters;
    delete[] noFingerprintFilters;
    delete[] noFingerprintExceptionFilters;
    delete[] noFingerprintDomainOnlyFilters;
    delete[] noFingerprintAntiDomainOnlyFilters;
    delete[] noFingerprintDomainOnlyExceptionFilters;
    delete[] noFingerprintAntiDomainOnlyExceptionFilters;

    // Adjust the current pointers to be just after the copied in data
    curFilters += numFilters;
    curHtmlFilters += numHtmlFilters;
    curExceptionFilters += numExceptionFilters;
    curNoFingerprintFilters += numNoFingerprintFilters;
    curNoFingerprintExceptionFilters += numNoFingerprintExceptionFilters;
    curNoFingerprintDomainOnlyFilters += numNoFingerprintDomainOnlyFilters;
    curNoFingerprintAntiDomainOnlyFilters += numNoFingerprintAntiDomainOnlyFilters;
    curNoFingerprintDomainOnlyExceptionFilters += numNoFingerprintDomainOnlyExceptionFilters;
    curNoFingerprintAntiDomainOnlyExceptionFilters +=
        numNoFingerprintAntiDomainOnlyExceptionFilters;
  }

  // And finally update with the new counts
  numFilters += newNumFilters;
  numCosmeticFilters += newNumCosmeticFilters;
  numHtmlFilters += newNumHtmlFilters;
  numScriptletFilters += newNumScriptletFilters;
  numExceptionFilters += newNumExceptionFilters;
  numNoFingerprintFilters += newNumNoFingerprintFilters;
  numNoFingerprintExceptionFilters += newNumNoFingerprintExceptionFilters;
  numNoFingerprintDomainOnlyFilters += newNumNoFingerprintDomainOnlyFilters;
  numNoFingerprintAntiDomainOnlyFilters += newNumNoFingerprintAntiDomainOnlyFilters;
  numNoFingerprintDomainOnlyExceptionFilters += newNumNoFingerprintDomainOnlyExceptionFilters;
  numNoFingerprintAntiDomainOnlyExceptionFilters +=
      newNumNoFingerprintAntiDomainOnlyExceptionFilters;
  numHostAnchoredFilters += newNumHostAnchoredFilters;
  numHostAnchoredExceptionFilters += newNumHostAnchoredExceptionFilters;

  // Adjust the new member list pointers
  filters = newFilters;
  htmlFilters = newHtmlFilters;
  exceptionFilters = newExceptionFilters;
  noFingerprintFilters = newNoFingerprintFilters;
  noFingerprintExceptionFilters = newNoFingerprintExceptionFilters;
  noFingerprintDomainOnlyFilters = newNoFingerprintDomainOnlyFilters;
  noFingerprintAntiDomainOnlyFilters = newNoFingerprintAntiDomainOnlyFilters;
  noFingerprintDomainOnlyExceptionFilters =
      newNoFingerprintDomainOnlyExceptionFilters;
  noFingerprintAntiDomainOnlyExceptionFilters =
      newNoFingerprintAntiDomainOnlyExceptionFilters;

  CosmeticFilterHashMap elementHidingFilterHashMap(6000);
  CosmeticFilterHashMap elementHidingExceptionFilterHashMap(2000);
  // rules count is often close to rules domain count, so we use rules count here
  auto *extendedCssHashMap = new CosmeticFilterHashMap(numExtendedCss);
  auto *cssRulesHashMap = new CosmeticFilterHashMap(numCssRules);
  auto *scriptletHashMap = new CosmeticFilterHashMap(numScriptletFilters);

  for (auto f : filterList) {
    switch (f.filterType & FTListTypesMask) {
    case FTException:
      if (f.filterType & FTHostOnly) {
        // do nothing, handled by hash set.
      } else if (AdBlockClient::getFingerprint(nullptr, f)) {
        (*curExceptionFilters).swapData(&f);
        curExceptionFilters++;
      } else if (f.isDomainOnlyFilter()) {
        AddFilterDomainsToHashSet(&f, noFingerprintDomainExceptionHashSet);
        (*curNoFingerprintDomainOnlyExceptionFilters).swapData(&f);
        curNoFingerprintDomainOnlyExceptionFilters++;
      } else if (f.isAntiDomainOnlyFilter()) {
        AddFilterDomainsToHashSet(&f, noFingerprintAntiDomainExceptionHashSet);
        (*curNoFingerprintAntiDomainOnlyExceptionFilters).swapData(&f);
        curNoFingerprintAntiDomainOnlyExceptionFilters++;
      } else {
        (*curNoFingerprintExceptionFilters).swapData(&f);
        curNoFingerprintExceptionFilters++;
      }
      break;
    case FTElementHiding:
      putElementHidingFilterToHashMap(&f, &elementHidingFilterHashMap);
      break;
    case FTElementHidingException:
      putElementHidingFilterToHashMap(&f, &elementHidingExceptionFilterHashMap);
      break;
    case FTExtendedCss:
      putElementHidingFilterToHashMap(&f, extendedCssHashMap);
      break;
    case FTCss:
      putElementHidingFilterToHashMap(&f, cssRulesHashMap);
      break;
    case FTCssException:
      // unsupported
      break;
    case FTHTMLFiltering:
      (*curHtmlFilters).swapData(&f);
      curHtmlFilters++;
      break;
    case FTScriptlet:
      if (extractScriptletArgsAsData(f)) {
        putElementHidingFilterToHashMap(&f, scriptletHashMap);
      }
      break;
    case FTEmpty:
    case FTComment:
      // No need to store
      break;
    default:
      if (f.filterType & FTHostOnly) {
        // Do nothing
      } else if (AdBlockClient::getFingerprint(nullptr, f)) {
        (*curFilters).swapData(&f);
        curFilters++;
      } else if (f.isDomainOnlyFilter()) {
        AddFilterDomainsToHashSet(&f, noFingerprintDomainHashSet);
        (*curNoFingerprintDomainOnlyFilters).swapData(&f);
        curNoFingerprintDomainOnlyFilters++;
      } else if (f.isAntiDomainOnlyFilter()) {
        AddFilterDomainsToHashSet(&f, noFingerprintAntiDomainHashSet);
        (*curNoFingerprintAntiDomainOnlyFilters).swapData(&f);
        curNoFingerprintAntiDomainOnlyFilters++;
      } else {
        (*curNoFingerprintFilters).swapData(&f);
        curNoFingerprintFilters++;
      }
      break;
    }
  }

  if (!elementHidingSelectorHashMap) {
    elementHidingSelectorHashMap =
        new HashMap<NoFingerprintDomain, CosmeticFilter>(elementHidingFilterHashMap.GetSize());
  }
  if (!elementHidingExceptionSelectorHashMap) {
    elementHidingExceptionSelectorHashMap =
        new HashMap<NoFingerprintDomain, CosmeticFilter>(
            elementHidingExceptionFilterHashMap.GetSize());
  }
  elementHidingFilterHashMap.toElementHidingSelectorMap(elementHidingSelectorHashMap);
  elementHidingExceptionFilterHashMap
      .toElementHidingSelectorMap(elementHidingExceptionSelectorHashMap);

  if (genericElementHidingSelectors) {
    genericCosmeticFilters.Add(CosmeticFilter(genericElementHidingSelectors->data));
  }
  char *selectorsNew = genericCosmeticFilters.toStylesheet();
  delete genericElementHidingSelectors;
  genericElementHidingSelectors = new CosmeticFilter(selectorsNew);
  delete[] selectorsNew;

  delete cssRulesMap;
  cssRulesMap = cssRulesHashMap;

  delete extendedCssMap;
  extendedCssMap = extendedCssHashMap;

  delete scriptletMap;
  scriptletMap = scriptletHashMap;

#ifdef PERF_STATS
  cout << "Simple cosmetic filter size: "
    << genericCosmeticFilters.GetSize() << endl;
#endif

  return true;
}

void AdBlockClient::addTag(const std::string &tag) {
  if (tags.find(tag) == tags.end()) {
    tags.insert(tag);
  }
}

void AdBlockClient::removeTag(const std::string &tag) {
  auto it = tags.find(tag);
  if (it != tags.end()) {
    tags.erase(it);
  }
}

bool AdBlockClient::tagExists(const std::string &tag) const {
  return tags.find(tag) != tags.end();
}

// Fills the specified buffer if specified, returns the number of characters
// written or needed
int serializeFilters(char *buffer, size_t bufferSizeAvail,
                     Filter *f, int numFilters) {
  int bufferSize = 0;
  for (int i = 0; i < numFilters; i++) {
    bufferSize += f->Serialize(buffer ? (buffer + bufferSize) : nullptr);
    f++;
  }
  return bufferSize;
}

// Returns a newly allocated buffer, caller must manually delete[] the buffer
char *AdBlockClient::serialize(int *totalSize, bool ignoreHtmlFilters) const {
  *totalSize = 0;
  int adjustedNumHtmlFilters = ignoreHtmlFilters ? 0 : numHtmlFilters;

  uint32_t hostAnchoredHashSetSize = 0;
  char *hostAnchoredHashSetBuffer = nullptr;
  if (hostAnchoredHashSet) {
    hostAnchoredHashSetBuffer =
        hostAnchoredHashSet->SerializeOut(&hostAnchoredHashSetSize);
  }

  uint32_t hostAnchoredExceptionHashSetSize = 0;
  char *hostAnchoredExceptionHashSetBuffer = nullptr;
  if (hostAnchoredExceptionHashSet) {
    hostAnchoredExceptionHashSetBuffer =
        hostAnchoredExceptionHashSet->SerializeOut(
            &hostAnchoredExceptionHashSetSize);
  }

  uint32_t noFingerprintDomainHashSetSize = 0;
  char *noFingerprintDomainHashSetBuffer = nullptr;
  if (noFingerprintDomainHashSet) {
    noFingerprintDomainHashSetBuffer =
        noFingerprintDomainHashSet->SerializeOut(&noFingerprintDomainHashSetSize);
  }

  uint32_t noFingerprintAntiDomainHashSetSize = 0;
  char *noFingerprintAntiDomainHashSetBuffer = nullptr;
  if (noFingerprintAntiDomainHashSet) {
    noFingerprintAntiDomainHashSetBuffer =
        noFingerprintAntiDomainHashSet->SerializeOut(
            &noFingerprintAntiDomainHashSetSize);
  }

  uint32_t noFingerprintDomainExceptionHashSetSize = 0;
  char *noFingerprintDomainExceptionHashSetBuffer = nullptr;
  if (noFingerprintDomainExceptionHashSet) {
    noFingerprintDomainExceptionHashSetBuffer =
        noFingerprintDomainExceptionHashSet->SerializeOut(
            &noFingerprintDomainExceptionHashSetSize);
  }

  uint32_t noFingerprintAntiDomainExceptionHashSetSize = 0;
  char *noFingerprintAntiDomainExceptionHashSetBuffer = nullptr;
  if (noFingerprintAntiDomainExceptionHashSet) {
    noFingerprintAntiDomainExceptionHashSetBuffer =
        noFingerprintAntiDomainExceptionHashSet->SerializeOut(
            &noFingerprintAntiDomainExceptionHashSetSize);
  }

  uint32_t elementHidingHashMapSize = 0;
  char *elementHidingHashMapBuffer = nullptr;
  if (elementHidingSelectorHashMap) {
    elementHidingHashMapBuffer =
        elementHidingSelectorHashMap->SerializeOut(&elementHidingHashMapSize);
  }

  uint32_t elementHidingExceptionHashMapSize = 0;
  char *elementHidingExceptionHashMapBuffer = nullptr;
  if (elementHidingExceptionSelectorHashMap) {
    elementHidingExceptionHashMapBuffer =
        elementHidingExceptionSelectorHashMap->SerializeOut(&elementHidingExceptionHashMapSize);
  }

  uint32_t genericElementHidingSelectorsSize = 0;
  if (genericElementHidingSelectors) {
    genericElementHidingSelectorsSize = genericElementHidingSelectors->Serialize(nullptr);
  }

  uint32_t extendedCssHashMapSize = 0;
  char *extendedCssHashMapBuffer = nullptr;
  if (extendedCssMap) {
    extendedCssHashMapBuffer =
        extendedCssMap->SerializeOut(&extendedCssHashMapSize);
  }

  uint32_t cssRulesHashMapSize = 0;
  char *cssRulesHashMapBuffer = nullptr;
  if (cssRulesMap) {
    cssRulesHashMapBuffer =
        cssRulesMap->SerializeOut(&cssRulesHashMapSize);
  }

  uint32_t scriptletHashMapSize = 0;
  char *scriptletHashMapBuffer = nullptr;
  if (scriptletMap) {
    scriptletHashMapBuffer =
        scriptletMap->SerializeOut(&scriptletHashMapSize);
  }

  // Get the number of bytes that we'll need
  char sz[512];
  *totalSize += 1 + snprintf(sz,
                             sizeof(sz),
                             "%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x",
                             numFilters,
                             numExceptionFilters,
                             numCosmeticFilters,
                             adjustedNumHtmlFilters,
                             numScriptletFilters,
                             numNoFingerprintFilters,
                             numNoFingerprintExceptionFilters,
                             numNoFingerprintDomainOnlyFilters,
                             numNoFingerprintAntiDomainOnlyFilters,
                             numNoFingerprintDomainOnlyExceptionFilters,
                             numNoFingerprintAntiDomainOnlyExceptionFilters,
                             numHostAnchoredFilters,
                             numHostAnchoredExceptionFilters,
                             bloomFilter ? bloomFilter->getByteBufferSize() : 0,
                             exceptionBloomFilter
                             ? exceptionBloomFilter->getByteBufferSize() : 0,
                             hostAnchoredHashSetSize,
                             hostAnchoredExceptionHashSetSize,
                             noFingerprintDomainHashSetSize,
                             noFingerprintAntiDomainHashSetSize,
                             noFingerprintDomainExceptionHashSetSize,
                             noFingerprintAntiDomainExceptionHashSetSize,
                             elementHidingHashMapSize,
                             elementHidingExceptionHashMapSize,
                             genericElementHidingSelectorsSize,
                             extendedCssHashMapSize,
                             cssRulesHashMapSize,
                             scriptletHashMapSize);
  *totalSize += serializeFilters(nullptr, 0, filters, numFilters) +
      serializeFilters(nullptr, 0, exceptionFilters, numExceptionFilters) +
      serializeFilters(nullptr, 0, htmlFilters, adjustedNumHtmlFilters) +
      serializeFilters(nullptr, 0,
                       noFingerprintFilters, numNoFingerprintFilters) +
      serializeFilters(nullptr, 0, noFingerprintExceptionFilters,
                       numNoFingerprintExceptionFilters) +
      serializeFilters(nullptr, 0,
                       noFingerprintDomainOnlyFilters,
                       numNoFingerprintDomainOnlyFilters) +
      serializeFilters(nullptr, 0,
                       noFingerprintAntiDomainOnlyFilters,
                       numNoFingerprintAntiDomainOnlyFilters) +
      serializeFilters(nullptr, 0, noFingerprintDomainOnlyExceptionFilters,
                       numNoFingerprintDomainOnlyExceptionFilters) +
      serializeFilters(nullptr, 0, noFingerprintAntiDomainOnlyExceptionFilters,
                       numNoFingerprintAntiDomainOnlyExceptionFilters);

  *totalSize += bloomFilter ? bloomFilter->getByteBufferSize() : 0;
  *totalSize += exceptionBloomFilter
                ? exceptionBloomFilter->getByteBufferSize() : 0;
  *totalSize += hostAnchoredHashSetSize;
  *totalSize += hostAnchoredExceptionHashSetSize;
  *totalSize += noFingerprintDomainHashSetSize;
  *totalSize += noFingerprintAntiDomainHashSetSize;
  *totalSize += noFingerprintDomainExceptionHashSetSize;
  *totalSize += noFingerprintAntiDomainExceptionHashSetSize;
  *totalSize += elementHidingHashMapSize;
  *totalSize += elementHidingExceptionHashMapSize;
  *totalSize += genericElementHidingSelectorsSize;
  *totalSize += extendedCssHashMapSize;
  *totalSize += cssRulesHashMapSize;
  *totalSize += scriptletHashMapSize;

  // Allocate it
  int pos = 0;
  char *buffer = new char[*totalSize];
  memset(buffer, 0, *totalSize);

  // And start copying stuff in
  snprintf(buffer, *totalSize, "%s", sz);
  pos += static_cast<int>(strlen(sz)) + 1;
  pos += serializeFilters(buffer + pos, *totalSize - pos, filters, numFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos,
                          exceptionFilters, numExceptionFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos, htmlFilters,
                          adjustedNumHtmlFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos, noFingerprintFilters,
                          numNoFingerprintFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos,
                          noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos,
                          noFingerprintDomainOnlyFilters,
                          numNoFingerprintDomainOnlyFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos,
                          noFingerprintAntiDomainOnlyFilters,
                          numNoFingerprintAntiDomainOnlyFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos,
                          noFingerprintDomainOnlyExceptionFilters,
                          numNoFingerprintDomainOnlyExceptionFilters);
  pos += serializeFilters(buffer + pos, *totalSize - pos,
                          noFingerprintAntiDomainOnlyExceptionFilters,
                          numNoFingerprintAntiDomainOnlyExceptionFilters);

  if (bloomFilter) {
    memcpy(buffer + pos, bloomFilter->getBuffer(),
           bloomFilter->getByteBufferSize());
    pos += bloomFilter->getByteBufferSize();
  }
  if (exceptionBloomFilter) {
    memcpy(buffer + pos, exceptionBloomFilter->getBuffer(),
           exceptionBloomFilter->getByteBufferSize());
    pos += exceptionBloomFilter->getByteBufferSize();
  }
  if (hostAnchoredHashSet) {
    memcpy(buffer + pos, hostAnchoredHashSetBuffer, hostAnchoredHashSetSize);
    pos += hostAnchoredHashSetSize;
    delete[] hostAnchoredHashSetBuffer;
  }
  if (hostAnchoredExceptionHashSet) {
    memcpy(buffer + pos, hostAnchoredExceptionHashSetBuffer,
           hostAnchoredExceptionHashSetSize);
    pos += hostAnchoredExceptionHashSetSize;
    delete[] hostAnchoredExceptionHashSetBuffer;
  }
  if (noFingerprintDomainHashSet) {
    memcpy(buffer + pos, noFingerprintDomainHashSetBuffer,
           noFingerprintDomainHashSetSize);
    pos += noFingerprintDomainHashSetSize;
    delete[] noFingerprintDomainHashSetBuffer;
  }
  if (noFingerprintAntiDomainHashSet) {
    memcpy(buffer + pos, noFingerprintAntiDomainHashSetBuffer,
           noFingerprintAntiDomainHashSetSize);
    pos += noFingerprintAntiDomainHashSetSize;
    delete[] noFingerprintAntiDomainHashSetBuffer;
  }
  if (noFingerprintDomainExceptionHashSet) {
    memcpy(buffer + pos, noFingerprintDomainExceptionHashSetBuffer,
           noFingerprintDomainExceptionHashSetSize);
    pos += noFingerprintDomainExceptionHashSetSize;
    delete[] noFingerprintDomainExceptionHashSetBuffer;
  }
  if (noFingerprintAntiDomainExceptionHashSet) {
    memcpy(buffer + pos, noFingerprintAntiDomainExceptionHashSetBuffer,
           noFingerprintAntiDomainExceptionHashSetSize);
    pos += noFingerprintAntiDomainExceptionHashSetSize;
    delete[] noFingerprintAntiDomainExceptionHashSetBuffer;
  }
  if (elementHidingSelectorHashMap) {
    memcpy(buffer + pos, elementHidingHashMapBuffer,
           elementHidingHashMapSize);
    pos += elementHidingHashMapSize;
    delete[] elementHidingHashMapBuffer;
  }
  if (elementHidingExceptionSelectorHashMap) {
    memcpy(buffer + pos, elementHidingExceptionHashMapBuffer,
           elementHidingExceptionHashMapSize);
    pos += elementHidingExceptionHashMapSize;
    delete[] elementHidingExceptionHashMapBuffer;
  }
  if (genericElementHidingSelectors) {
    genericElementHidingSelectors->Serialize(buffer + pos);
    pos += genericElementHidingSelectorsSize;
  }
  if (extendedCssMap) {
    memcpy(buffer + pos, extendedCssHashMapBuffer, extendedCssHashMapSize);
    pos += extendedCssHashMapSize;
    delete[] extendedCssHashMapBuffer;
  }
  if (cssRulesMap) {
    memcpy(buffer + pos, cssRulesHashMapBuffer, cssRulesHashMapSize);
    pos += cssRulesHashMapSize;
    delete[] cssRulesHashMapBuffer;
  }
  if (scriptletMap) {
    memcpy(buffer + pos, scriptletHashMapBuffer, scriptletHashMapSize);
    pos += scriptletHashMapSize;
    delete[] scriptletHashMapBuffer;
  }

  return buffer;
}

// Fills the specified buffer if specified, returns the number of characters
// written or needed
int deserializeFilters(char *buffer, Filter *f, int numFilters) {
  int pos = 0;
  for (int i = 0; i < numFilters; i++) {
    pos += f->Deserialize(buffer + pos, 1024 * 16);
    f++;
  }
  return pos;
}

bool AdBlockClient::deserialize(char *buffer) {
  clear();
  deserializedBuffer = buffer;
  int bloomFilterSize = 0, exceptionBloomFilterSize = 0,
      hostAnchoredHashSetSize = 0, hostAnchoredExceptionHashSetSize = 0,
      noFingerprintDomainHashSetSize = 0,
      noFingerprintAntiDomainHashSetSize = 0,
      noFingerprintDomainExceptionHashSetSize = 0,
      noFingerprintAntiDomainExceptionHashSetSize = 0,
      elementHidingHashMapSize = 0,
      elementHidingExceptionHashMapSize = 0,
      genericElementHidingSelectorsSize = 0,
      extendedCssHashMapSize = 0,
      cssRulesHashMapSize = 0,
      scriptletHashMapSize = 0;
  int pos = 0;
  sscanf(buffer + pos,
         "%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x",
         &numFilters,
         &numExceptionFilters, &numCosmeticFilters, &numHtmlFilters, &numScriptletFilters,
         &numNoFingerprintFilters, &numNoFingerprintExceptionFilters,
         &numNoFingerprintDomainOnlyFilters,
         &numNoFingerprintAntiDomainOnlyFilters,
         &numNoFingerprintDomainOnlyExceptionFilters,
         &numNoFingerprintAntiDomainOnlyExceptionFilters,
         &numHostAnchoredFilters, &numHostAnchoredExceptionFilters,
         &bloomFilterSize, &exceptionBloomFilterSize,
         &hostAnchoredHashSetSize, &hostAnchoredExceptionHashSetSize,
         &noFingerprintDomainHashSetSize,
         &noFingerprintAntiDomainHashSetSize,
         &noFingerprintDomainExceptionHashSetSize,
         &noFingerprintAntiDomainExceptionHashSetSize,
         &elementHidingHashMapSize, &elementHidingExceptionHashMapSize,
         &genericElementHidingSelectorsSize, &extendedCssHashMapSize,
         &cssRulesHashMapSize, &scriptletHashMapSize);
  pos += static_cast<int>(strlen(buffer + pos)) + 1;

  filters = new Filter[numFilters];
  exceptionFilters = new Filter[numExceptionFilters];
  htmlFilters = new Filter[numHtmlFilters];
  noFingerprintFilters = new Filter[numNoFingerprintFilters];
  noFingerprintExceptionFilters = new Filter[numNoFingerprintExceptionFilters];
  noFingerprintDomainOnlyFilters =
      new Filter[numNoFingerprintDomainOnlyFilters];
  noFingerprintAntiDomainOnlyFilters =
      new Filter[numNoFingerprintAntiDomainOnlyFilters];
  noFingerprintDomainOnlyExceptionFilters =
      new Filter[numNoFingerprintDomainOnlyExceptionFilters];
  noFingerprintAntiDomainOnlyExceptionFilters =
      new Filter[numNoFingerprintAntiDomainOnlyExceptionFilters];

  pos += deserializeFilters(buffer + pos, filters, numFilters);
  pos += deserializeFilters(buffer + pos,
                            exceptionFilters, numExceptionFilters);
  pos += deserializeFilters(buffer + pos,
                            htmlFilters, numHtmlFilters);
  pos += deserializeFilters(buffer + pos,
                            noFingerprintFilters, numNoFingerprintFilters);
  pos += deserializeFilters(buffer + pos,
                            noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);

  pos += deserializeFilters(buffer + pos,
                            noFingerprintDomainOnlyFilters, numNoFingerprintDomainOnlyFilters);
  pos += deserializeFilters(buffer + pos,
                            noFingerprintAntiDomainOnlyFilters,
                            numNoFingerprintAntiDomainOnlyFilters);
  pos += deserializeFilters(buffer + pos,
                            noFingerprintDomainOnlyExceptionFilters,
                            numNoFingerprintDomainOnlyExceptionFilters);
  pos += deserializeFilters(buffer + pos,
                            noFingerprintAntiDomainOnlyExceptionFilters,
                            numNoFingerprintAntiDomainOnlyExceptionFilters);

  initBloomFilter(&bloomFilter, buffer + pos, bloomFilterSize);
  pos += bloomFilterSize;
  initBloomFilter(&exceptionBloomFilter,
                  buffer + pos, exceptionBloomFilterSize);
  pos += exceptionBloomFilterSize;
  if (!initHashSet(&hostAnchoredHashSet,
                   buffer + pos, hostAnchoredHashSetSize)) {
    return false;
  }
  pos += hostAnchoredHashSetSize;
  if (!initHashSet(&hostAnchoredExceptionHashSet,
                   buffer + pos, hostAnchoredExceptionHashSetSize)) {
    return false;
  }
  pos += hostAnchoredExceptionHashSetSize;

  if (!initHashSet(&noFingerprintDomainHashSet,
                   buffer + pos, noFingerprintDomainHashSetSize)) {
    return false;
  }
  pos += noFingerprintDomainHashSetSize;

  if (!initHashSet(&noFingerprintAntiDomainHashSet,
                   buffer + pos, noFingerprintAntiDomainHashSetSize)) {
    return false;
  }
  pos += noFingerprintAntiDomainHashSetSize;

  if (!initHashSet(&noFingerprintDomainExceptionHashSet,
                   buffer + pos, noFingerprintDomainExceptionHashSetSize)) {
    return false;
  }
  pos += noFingerprintDomainExceptionHashSetSize;

  if (!initHashSet(&noFingerprintAntiDomainExceptionHashSet,
                   buffer + pos, noFingerprintAntiDomainExceptionHashSetSize)) {
    return false;
  }
  pos += noFingerprintAntiDomainExceptionHashSetSize;

  if (!initHashMap(&elementHidingSelectorHashMap,
                   buffer + pos, elementHidingHashMapSize)) {
    return false;
  }
  pos += elementHidingHashMapSize;

  if (!initHashMap(&elementHidingExceptionSelectorHashMap,
                   buffer + pos, elementHidingExceptionHashMapSize)) {
    return false;
  }
  pos += elementHidingExceptionHashMapSize;

  delete genericElementHidingSelectors;
  genericElementHidingSelectors = new CosmeticFilter();
  genericElementHidingSelectors->Deserialize(buffer + pos, genericElementHidingSelectorsSize);
  pos += genericElementHidingSelectorsSize;

  if (!initHashMap(&extendedCssMap, buffer + pos, extendedCssHashMapSize)) {
    return false;
  }
  pos += extendedCssHashMapSize;

  if (!initHashMap(&cssRulesMap, buffer + pos, cssRulesHashMapSize)) {
    return false;
  }
  pos += cssRulesHashMapSize;

  if (!initHashMap(&scriptletMap, buffer + pos, scriptletHashMapSize)) {
    return false;
  }
  pos += scriptletHashMapSize;

  return true;
}

void AdBlockClient::enableBadFingerprintDetection() {
  if (badFingerprintsHashSet) {
    return;
  }

  badFingerprintsHashSet = new BadFingerprintsHashSet();
  for (auto &badFingerprint : badFingerprints) {
    badFingerprintsHashSet->Add(BadFingerprint(badFingerprint));
  }
}

uint64_t HashFn2Byte::operator()(const char *input, int len,
                                 unsigned char lastCharCode, uint64_t lastHash) {
  return (((uint64_t) input[1]) << 8) | input[0];
}

uint64_t HashFn2Byte::operator()(const char *input, int len) {
  return (((uint64_t) input[1]) << 8) | input[0];
}
