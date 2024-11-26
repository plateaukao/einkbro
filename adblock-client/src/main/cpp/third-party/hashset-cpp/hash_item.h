/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#ifndef HASH_ITEM_H_
#define HASH_ITEM_H_

#include "./base.h"

template<class T>
class HashItem {
public:
    HashItem() : next_(nullptr), hash_item_storage_(nullptr) {
    }

    ~HashItem() {
      if (hash_item_storage_) {
        delete hash_item_storage_;
      }
    }

    HashItem *next_;
    T *hash_item_storage_;
};

#endif  // HASH_ITEM_H_
