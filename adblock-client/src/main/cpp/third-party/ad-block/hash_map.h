//
// Created by Edsuns on 2021/1/14.
//

#ifndef HASHMAP_H
#define HASHMAP_H

template<class T>
class HashSet;

template<class K, class V>
class MapNode {
public:
    MapNode(const K &key, V *value = nullptr) : _key(new K(key)), _value(value) {}

    MapNode() : _key(nullptr), _value(nullptr) {}

    ~MapNode() {
        delete _key;
        delete _value;
    }

    K *getKey() const {
        return _key;
    }

    V *getValue() const {
        return _value;
    }

    uint64_t GetHash() const {
        return _key->GetHash();
    }

    bool operator==(const MapNode<K, V> &keyValue) const {
        return *_key == *keyValue._key;
    }

    bool operator!=(const MapNode<K, V> &keyValue) const {
        return !(*_key == *keyValue._key);
    }

    void Update(const MapNode<K, V> &keyValue) {
        // do nothing
    }

    uint32_t Serialize(char *buffer) {
        int totalSize = 0;
        char printBuffer[128];
        int keySize = _key->Serialize(nullptr);
        int valueSize = _value->Serialize(nullptr);

        int printLen = 1 + snprintf(printBuffer, sizeof(printBuffer), "%x,%x", keySize, valueSize);
        if (buffer) {
            memcpy(buffer, printBuffer, printLen);
        }
        totalSize += printLen;

        if (buffer) {
            _key->Serialize(buffer + totalSize);
        }
        totalSize += keySize;

        if (buffer) {
            _value->Serialize(buffer + totalSize);
        }
        totalSize += valueSize;

        return totalSize;
    }

    uint32_t Deserialize(char *buffer, uint32_t buffer_size) {
        int pos = 0;
        int keySize = 0;
        int valueSize = 0;
        sscanf(buffer, "%x,%x", &keySize, &valueSize);
        pos += static_cast<uint32_t>(strlen(buffer)) + 1;

        delete _key;
        delete _value;
        _key = new K();
        _value = new V();

        _key->Deserialize(buffer + pos, buffer_size);
        pos += keySize;

        _value->Deserialize(buffer + pos, buffer_size);
        pos += valueSize;

        return pos;
    }

private:
    // key-value pair
    K *_key;
    V *_value;
};

template<class K, class V>
class HashMap : public HashSet<MapNode<K, V>> {
public:
    HashMap(uint32_t bucket_count) : HashSet<MapNode<K, V>>(bucket_count, false) {}

    // some types can't be converted to a address, so here returns the pointer directly
    V *get(const K &key) {
        MapNode<K, V> *node = this->Find(MapNode<K, V>(key));
        if (node) {
            return node->getValue();
        }
        return nullptr;
    }

    bool put(const K &key, V *value) {
        uint64_t hash = key.GetHash();
        HashItem<MapNode<K, V>> *hash_item = this->buckets_[hash % this->bucket_count_];
        if (!hash_item) {
            hash_item = new HashItem<MapNode<K, V>>();
            hash_item->hash_item_storage_ = new MapNode<K, V>(key, value);
            this->buckets_[hash % this->bucket_count_] = hash_item;
            this->size_++;
            return true;
        }

        while (true) {
            if (*hash_item->hash_item_storage_->getKey() == key) {
                // update the node
                delete hash_item->hash_item_storage_;
                hash_item->hash_item_storage_ = new MapNode<K, V>(key, value);
                return false;
            }
            if (!hash_item->next_) {
                HashItem<MapNode<K, V>> *created_hash_item = new HashItem<MapNode<K, V>>();
                created_hash_item->hash_item_storage_ = new MapNode<K, V>(key, value);
                hash_item->next_ = created_hash_item;
                break;
            }
            hash_item = hash_item->next_;
        }

        this->size_++;
        return true;
    }

    bool remove(const K &key) {
        return this->Remove(MapNode<K, V>(key));
    }

private:
    // forbid
    bool Add(const MapNode<K, V> &item_to_add, bool update_if_exists = true) {
        return false;
    }
};

#endif //HASHMAP_H
