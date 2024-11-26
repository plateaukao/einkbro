/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#ifndef NO_FINGERPRINT_DOMAIN_H_
#define NO_FINGERPRINT_DOMAIN_H_

#include "./base.h"

class NoFingerprintDomain {
public:
    NoFingerprintDomain();

    NoFingerprintDomain(const NoFingerprintDomain &other);

    NoFingerprintDomain(const char *data, int dataLen);

    ~NoFingerprintDomain();

    uint64_t hash() const;

    uint64_t GetHash() const {
        return hash();
    }

    uint32_t Serialize(char *buffer);

    uint32_t Deserialize(char *buffer, uint32_t bufferSize);

    // Nothing needs to be updated when being added multiple times
    void Update(const NoFingerprintDomain &) {}

    bool operator==(const NoFingerprintDomain &rhs) const;

private:
    // Holds true if the data should not free memory because for example it
    // was loaded from a large buffer somewhere else via the serialize and
    // deserialize functions.
    bool borrowed_data;
    char *data;
    int dataLen;
};

#endif  // NO_FINGERPRINT_DOMAIN_H_
