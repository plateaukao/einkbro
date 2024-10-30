/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#include "./context_domain.h"
#include "../hashset-cpp/hashFn.h"

static HashFn context_domain_hash(19);

uint64_t ContextDomain::GetHash() const {
    return context_domain_hash(start_, len_);
}
