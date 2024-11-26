/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#include "./no_fingerprint_domain.h"

#include <stdio.h>
#include <string.h>

#include "../hashset-cpp/hashFn.h"

static HashFn h(19);

NoFingerprintDomain::NoFingerprintDomain() :
        borrowed_data(false),
        data(nullptr),
        dataLen(-1) {
}

NoFingerprintDomain::NoFingerprintDomain(const NoFingerprintDomain &other) {
  borrowed_data = other.borrowed_data;
  dataLen = other.dataLen;
  if (other.dataLen == -1 && other.data) {
    dataLen = static_cast<int>(strlen(other.data));
  }

  if (other.borrowed_data) {
    data = other.data;
  } else {
    if (other.data) {
      data = new char[dataLen + 1];
      memcpy(data, other.data, dataLen + 1);
    } else {
      data = nullptr;
    }
  }
}

NoFingerprintDomain::NoFingerprintDomain(const char *data, int dataLen) :
        borrowed_data(true), data(const_cast<char *>(data)),
        dataLen(dataLen) {
}

NoFingerprintDomain::~NoFingerprintDomain() {
  if (borrowed_data) {
    return;
  }
  if (data) {
    delete[] data;
  }
}

uint64_t NoFingerprintDomain::hash() const {
  if (!data) {
    return 0;
  }
  return h(data, dataLen);
}

uint32_t NoFingerprintDomain::Serialize(char *buffer) {
  uint32_t totalSize = 0;
  char sz[64];
  uint32_t dataLenSize = 1 + snprintf(sz, sizeof(sz),
                                      "%x", dataLen);
  if (buffer) {
    memcpy(buffer + totalSize, sz, dataLenSize);
  }
  totalSize += dataLenSize;
  if (buffer) {
    memcpy(buffer + totalSize, data, dataLen);
  }
  totalSize += dataLen;

  totalSize += 1;

  return totalSize;
}

uint32_t NoFingerprintDomain::Deserialize(char *buffer, uint32_t bufferSize) {
  dataLen = 0;
  sscanf(buffer, "%x", &dataLen);
  uint32_t consumed = static_cast<uint32_t>(strlen(buffer)) + 1;
  if (consumed + dataLen >= bufferSize) {
    return 0;
  }
  data = buffer + consumed;
  consumed += dataLen;
  borrowed_data = true;
  // Since we serialize a \0 character, we need to skip past it.
  consumed++;
  return consumed;
}

bool NoFingerprintDomain::operator==(const NoFingerprintDomain &rhs) const {
  if (dataLen != rhs.dataLen) {
    return false;
  }
  if (dataLen == 0) {
    return true;
  }
  return !memcmp(data, rhs.data, dataLen);
}
