/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#ifndef PROTOCOL_H_
#define PROTOCOL_H_

// Checks whether the URL can be blocked by ABP, based on its protocol.
//
// We only apply ABP rules against certain protocols (http, https, ws, wss).
// This function checks to see if the given url is of one of these protocol.
// For the purposes of this function, blob indicators are ignored (e.g.
// "blob:http://" is treated the same as "http://").
bool isBlockableProtocol(const char *url, int urlLen);

#endif  // PROTOCOL_H_
