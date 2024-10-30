/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

// You should probably not be using this.  This is only useful in environments
// without std lib and having specific serialization and memory requirements.
// Instead consider using `hash_set` which is a more generic implementation
// with templates.

#ifndef HASH_SET_H_
#define HASH_SET_H_

#include <stdint.h>
#include <math.h>
#include <string.h>
#include <stdio.h>
#include <vector>

#include "./base.h"
#include "./hash_item.h"

template<class T>
class HashSet {
public:
    typedef uint64_t (*HashSetFnPtr)(const T *hash_item);

    /**
     * @param bucket_count The number of buckets to create for the hash set
     * @param multi_set Allow multiple items with the same hash to be added.
     */
    explicit HashSet(uint32_t bucket_count, bool multi_set)
            : multi_set_(multi_set) {
        Init(bucket_count);
    }

    ~HashSet() {
        Cleanup();
    }

    /**
     * Adds the specified data if it doesn't exist
     * A copy of the data will be created, so the memory used to do the add
     * doesn't need to stick around.
     *
     * @param item_to_add The node to add
     * @param update_if_exists true if the item should be updated if it is already there
     *   false if the add should fail if it is already there. If multi_set is true and
     *   update_if_exists is false than the same hash'ed item will be added again.
     * @return true if the data was added
     */
    bool Add(const T &item_to_add, bool update_if_exists = true) {
        if (!check_buckets()) return false;
        uint64_t hash = item_to_add.GetHash();
        HashItem<T> *hash_item = buckets_[hash % bucket_count_];
        if (!hash_item) {
            hash_item = new HashItem<T>();
            hash_item->hash_item_storage_ = new T(item_to_add);
            buckets_[hash % bucket_count_] = hash_item;
            size_++;
            return true;
        }

        while (true) {
            if (*hash_item->hash_item_storage_ == item_to_add) {
                if (update_if_exists) {
                    hash_item->hash_item_storage_->Update(item_to_add);
                    return false;
                } else if (!multi_set_) {
                    return false;
                }
            }
            if (!hash_item->next_) {
                HashItem<T> *created_hash_item = new HashItem<T>();
                created_hash_item->hash_item_storage_ = new T(item_to_add);
                hash_item->next_ = created_hash_item;
                break;
            }
            hash_item = hash_item->next_;
        }

        size_++;
        return true;
    }

    /**
     * Determines if the specified data exists in the set or not`
     * @param data_to_check The data to check
     * @return true if the data found
     */
    bool Exists(const T &data_to_check) {
        if (!check_buckets()) return false;
        uint64_t hash = data_to_check.GetHash();
        HashItem<T> *hash_item = buckets_[hash % bucket_count_];
        if (!hash_item) {
            return false;
        }

        while (hash_item) {
            if (*hash_item->hash_item_storage_ == data_to_check) {
                return true;
            }
            hash_item = hash_item->next_;
        }

        return false;
    }

    /**
     * Determines if the specified data exists in the set or not`
     * @param data_to_check The data to check
     * @return true if the data found
     */
    size_t GetMatchingCount(const T &data_to_check) {
        if (!check_buckets()) return 0;
        size_t count = 0;
        uint64_t hash = data_to_check.GetHash();
        HashItem<T> *hash_item = buckets_[hash % bucket_count_];
        if (!hash_item) {
            return count;
        }

        while (hash_item) {
            if (*hash_item->hash_item_storage_ == data_to_check) {
                count++;
            }
            hash_item = hash_item->next_;
        }

        return count;
    }

    /**
     * Finds the specific data in the hash set.
     * This is useful because sometimes it contains more context
     * than the object used for the lookup.
     * @param data_to_check The data to check
     * @return The data stored in the hash set or nullptr if none is found.
     */
    T *Find(const T &data_to_check) {
        if (!check_buckets()) return nullptr;
        uint64_t hash = data_to_check.GetHash();
        HashItem<T> *hash_item = buckets_[hash % bucket_count_];
        if (!hash_item) {
            return nullptr;
        }

        while (hash_item) {
            if (*hash_item->hash_item_storage_ == data_to_check) {
                return hash_item->hash_item_storage_;
            }
            hash_item = hash_item->next_;
        }

        return nullptr;
    }

    /**
     * Finds the specific data in the hash set.
     * This is useful because sometimes it contains more context
     * than the object used for the lookup.
     * @param data_to_check The data to check
     * @return The data stored in the hash set or nullptr if none is found.
     */
    void FindAll(const T &data_to_check, std::vector<T *> *result) {
        if (!check_buckets()) return;
        uint64_t hash = data_to_check.GetHash();
        HashItem<T> *hash_item = buckets_[hash % bucket_count_];
        if (!hash_item) {
            return;
        }

        while (hash_item) {
            if (*hash_item->hash_item_storage_ == data_to_check) {
                result->push_back(hash_item->hash_item_storage_);
            }
            hash_item = hash_item->next_;
        }
    }

    /**
     * Removes the specific data in the hash set.
     * @param data_to_check The data to remove
     * @return true if an item matching the data was removed
     */
    bool Remove(const T &data_to_check) {
        if (!check_buckets()) return false;
        uint64_t hash = data_to_check.GetHash();
        HashItem<T> *hash_item = buckets_[hash % bucket_count_];
        if (!hash_item) {
            return false;
        }

        HashItem<T> *last_item = nullptr;
        while (hash_item) {
            if (*hash_item->hash_item_storage_ == data_to_check) {
                if (last_item) {
                    last_item->next_ = hash_item->next_;
                    delete hash_item;
                } else {
                    buckets_[hash % bucket_count_] = hash_item->next_;
                    delete hash_item;
                }
                size_--;
                return true;
            }

            last_item = hash_item;
            hash_item = hash_item->next_;
        }

        return false;
    }


    /**
     * Obtains the number of items in the Hash Set
     */
    uint32_t GetSize() {
        return size_;
    }

    /**
     * Serializes the parsed data and bloom filter data into a single buffer.
     * @param size The size is returned in the out parameter if it's needed to
     * write to a file.
     * @return The returned buffer should be deleted by the caller.
     */
    char *SerializeOut(uint32_t *size) {
        *size = 0;
        *size += Serialize(nullptr);
        char *buffer = new char[*size];
        memset(buffer, 0, *size);
        Serialize(buffer);
        return buffer;
    }

    uint32_t Serialize(char *buffer) {
        uint32_t total_size = 0;
        char sz[512];
        total_size += 1 + snprintf(sz, sizeof(sz), "%x,%x",
                                   bucket_count_, multi_set_ ? 1 : 0);
        if (buffer) {
            memcpy(buffer, sz, total_size);
        }
        for (uint32_t i = 0; i < bucket_count_; i++) {
            HashItem<T> *hash_item = buckets_[i];
            while (hash_item) {
                if (buffer) {
                    total_size +=
                            hash_item->hash_item_storage_->Serialize(buffer + total_size);
                } else {
                    total_size += hash_item->hash_item_storage_->Serialize(nullptr);
                }
                hash_item = hash_item->next_;
            }
            if (buffer) {
                buffer[total_size] = '\0';
            }
            // Second null terminator to show next bucket
            total_size++;
        }
        return total_size;
    }

    /**
     * Deserializes the buffer.
     * Memory passed in will be used by this instance directly without copying
     * it in.
     * @param buffer The serialized data to deserialize
     * @param buffer_size the size of the buffer to deserialize
     * @return true if the operation was successful
     */
    uint32_t Deserialize(char *buffer, uint32_t buffer_size) {
        Cleanup();
        uint32_t pos = 0;
        if (!HasNewlineBefore(buffer, buffer_size)) {
            return 0;
        }

        uint32_t multi_set = 0;
        sscanf(buffer + pos, "%x,%x", &bucket_count_, &multi_set);
        pos += static_cast<uint32_t>(strlen(buffer + pos)) + 1;
        multi_set_ = multi_set != 0;
        if (bucket_count_ == 0) {
            return pos;// at this moment, pos == buffer_size
        }
        buckets_ = new HashItem<T> *[bucket_count_];
        memset(buckets_, 0, sizeof(HashItem<T> *) * bucket_count_);
        if (pos >= buffer_size) {
            return 0;
        }
        for (uint32_t i = 0; i < bucket_count_; i++) {
            HashItem<T> *last_hash_item = nullptr;
            while (*(buffer + pos) != '\0') {
                if (pos >= buffer_size) {
                    return 0;
                }

                HashItem<T> *hash_item = new HashItem<T>();
                hash_item->hash_item_storage_ = new T();
                uint32_t deserialize_size =
                        hash_item->hash_item_storage_->Deserialize(buffer + pos,
                                                                   buffer_size - pos);
                pos += deserialize_size;
                if (pos >= buffer_size || deserialize_size == 0) {
                    return 0;
                }

                size_++;

                if (last_hash_item) {
                    last_hash_item->next_ = hash_item;
                } else {
                    buckets_[i] = hash_item;
                }
                last_hash_item = hash_item;
            }
            pos++;
        }
        return pos;
    }

    /**
     * Clears the HashSet back to the original dimensions but
     * with no data.
     */
    void Clear() {
        auto old_bucket_count = bucket_count_;
        Cleanup();
        Init(old_bucket_count);
    }

    /**
     * Returns true if the hash_set is a multi_set
     */
    bool IsMultiSet() const {
        return multi_set_;
    }

private:
    bool HasNewlineBefore(char *buffer, uint32_t buffer_size) {
        char *p = buffer;
        for (uint32_t i = 0; i < buffer_size; ++i) {
            if (*p == '\0')
                return true;
            p++;
        }
        return false;
    }

    void Init(uint32_t num_buckets) {
        bucket_count_ = num_buckets;
        buckets_ = nullptr;
        size_ = 0;
        if (bucket_count_ != 0) {
            buckets_ = new HashItem<T> *[bucket_count_];
            memset(buckets_, 0, sizeof(HashItem<T> *) * bucket_count_);
        }
    }

    void Cleanup() {
        if (buckets_) {
            for (uint32_t i = 0; i < bucket_count_; i++) {
                HashItem<T> *hash_item = buckets_[i];
                while (hash_item) {
                    HashItem<T> *temp_hash_item = hash_item;
                    hash_item = hash_item->next_;
                    delete temp_hash_item;
                }
            }
            delete[] buckets_;
            buckets_ = nullptr;
            bucket_count_ = 0;
            size_ = 0;
        }
    }

protected:
    bool check_buckets() {
        return buckets_ && bucket_count_ > 0;
    }

protected:
    bool multi_set_;
    uint32_t bucket_count_;
    HashItem<T> **buckets_;
    uint32_t size_;
};

#endif  // HASH_SET_H_
