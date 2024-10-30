/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#ifndef CONTEXT_DOMAIN_H_
#define CONTEXT_DOMAIN_H_

#include <string.h>
#include "./base.h"

// This class must operate off of borrowed memory
// Serialization and deserialization is not supported intentionally.
class ContextDomain {
public:
    uint64_t GetHash() const;

    ~ContextDomain() {
    }

    ContextDomain(const char *start, int len) {
        start_ = start;
        len_ = len;
    }

    ContextDomain(const ContextDomain &rhs) {
        start_ = rhs.start_;
        len_ = rhs.len_;
    }

    ContextDomain() : start_(nullptr), len_(0) {
    }

    bool operator==(const ContextDomain &rhs) const {
        if (!start_ || !rhs.start_) {
            return false;
        }
        if (len_ != rhs.len_) {
            return false;
        }
        return !memcmp(start_, rhs.start_, len_);
    }

    bool operator!=(const ContextDomain &rhs) const {
        return !(*this == rhs);
    }

    void Update(const ContextDomain &other) {
    }

    uint32_t Serialize(char *buffer) {
        return 0;
    }

    uint32_t Deserialize(char *buffer, uint32_t buffer_size) {
        return 0;
    }

private:
    const char *start_;
    int len_;
};

#endif  // CONTEXT_DOMAIN_H_
