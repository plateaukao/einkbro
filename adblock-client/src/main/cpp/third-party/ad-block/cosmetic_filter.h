/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#ifndef COSMETIC_FILTER_H_
#define COSMETIC_FILTER_H_

#include <math.h>
#include <string>
#include "../hashset-cpp/hash_set.h"
#include "hash_map.h"
#include "no_fingerprint_domain.h"

#include "linked_list.h"

class CosmeticFilter {
public:
    uint64_t hash() const;

    uint64_t GetHash() const {
        return hash();
    }

    ~CosmeticFilter() {
        if (data) {
            delete[] data;
        }
    }

    explicit CosmeticFilter(const char *data) {
        size_t len = strlen(data) + 1;
        this->data = new char[len];
        snprintf(this->data, len, "%s", data);
    }

    CosmeticFilter(const CosmeticFilter &rhs) {
        data = new char[strlen(rhs.data) + 1];
        memcpy(data, rhs.data, strlen(rhs.data) + 1);
    }

    CosmeticFilter() : data(nullptr) {
    }

    bool operator==(const CosmeticFilter &rhs) const {
        return !strcmp(data, rhs.data);
    }

    bool operator!=(const CosmeticFilter &rhs) const {
        return !(*this == rhs);
    }

    // Nothing needs to be updated for multiple adds
    void Update(const CosmeticFilter &) {}

    uint32_t Serialize(char *buffer) {
        if (buffer) {
            memcpy(buffer, data, strlen(data) + 1);
        }
        return static_cast<uint32_t>(strlen(data)) + 1;
    }

    uint32_t Deserialize(char *buffer, uint32_t bufferSize) {
        int len = static_cast<int>(strlen(buffer));
        data = new char[len + 1];
        memcpy(data, buffer, len + 1);
        return len + 1;
    }

    char *data;
};

class CosmeticFilterHashSet : public HashSet<CosmeticFilter> {
public:
    CosmeticFilterHashSet(uint32_t bucket_count = 1000)
            : HashSet<CosmeticFilter>(bucket_count, false) {}

    char *toStylesheet() {
        uint32_t len;
        return toStylesheet(&len);
    }

    char *toStylesheet(uint32_t *len) {
        *len = fillStylesheetBuffer(nullptr);
        char *buffer = new char[*len + 1];
        memset(buffer, 0, *len + 1);
        fillStylesheetBuffer(buffer);
        return buffer;
    }

    LinkedList<std::string> *toStringList() {
        LinkedList<std::string> *list = new LinkedList<std::string>();
        for (uint32_t bucketIndex = 0; bucketIndex < bucket_count_; bucketIndex++) {
            HashItem<CosmeticFilter> *hashItem = buckets_[bucketIndex];
            while (hashItem) {
                CosmeticFilter *cosmeticFilter = hashItem->hash_item_storage_;
                list->push_back(std::string(cosmeticFilter->data));
                hashItem = hashItem->next_;
            }
        }
        return list;
    }

private:
    uint32_t fillStylesheetBuffer(char *buffer) {
        uint32_t len = 0;
        for (uint32_t bucketIndex = 0; bucketIndex < bucket_count_; bucketIndex++) {
            HashItem<CosmeticFilter> *hashItem = buckets_[bucketIndex];
            while (hashItem) {
                if (len > 0) {
                    if (buffer) {
                        memcpy(buffer + len, ", ", 2);
                    }
                    len += 2;
                }
                CosmeticFilter *cosmeticFilter = hashItem->hash_item_storage_;
                int cosmeticFilterLen = static_cast<int>(strlen(cosmeticFilter->data));
                if (buffer) {
                    memcpy(buffer + len, cosmeticFilter->data, cosmeticFilterLen);
                }
                len += cosmeticFilterLen;
                hashItem = hashItem->next_;
            }
        }
        return len;
    }
};

class CosmeticFilterHashMap : public HashMap<NoFingerprintDomain, CosmeticFilterHashSet> {
public:
    CosmeticFilterHashMap(uint32_t bucket_count)
            : HashMap<NoFingerprintDomain, CosmeticFilterHashSet>(bucket_count) {}

    void putCosmeticFilter(const NoFingerprintDomain &key, const CosmeticFilter &value) {
        if (CosmeticFilterHashSet *existing_hash_set = get(key)) {
            existing_hash_set->Add(value);
        } else {
            CosmeticFilterHashSet *created_hash_set = new CosmeticFilterHashSet(30);
            created_hash_set->Add(value);
            put(key, created_hash_set);
        }
    }

    void toElementHidingSelectorMap(HashMap<NoFingerprintDomain, CosmeticFilter> *selectorMap) {
        for (uint32_t bucketIndex = 0; bucketIndex < bucket_count_; bucketIndex++) {
            HashItem<MapNode<NoFingerprintDomain, CosmeticFilterHashSet>> *hashItem = buckets_[bucketIndex];
            while (hashItem) {
                MapNode<NoFingerprintDomain, CosmeticFilterHashSet> *node = hashItem->hash_item_storage_;
                char *selector = node->getValue()->toStylesheet();
                selectorMap->put(*node->getKey(), new CosmeticFilter(selector));
                delete[] selector;
                hashItem = hashItem->next_;
            }
        }
    }
};

#endif  // COSMETIC_FILTER_H_
