/* Copyright (c) 2015 Brian R. Bondy. Distributed under the MPL2 license.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Modified by Edsuns@qq.com.
 * Origin: https://github.com/brave/ad-block
 */

#include <ctype.h>
#include "./protocol.h"

enum ProtocolParseState {
    ProtocolParseStateStart,
    ProtocolParseStateReadingBlob,
    ProtocolParseStatePostBlob,
    ProtocolParseStateReadingProtoWebSocket,
    ProtocolParseStateReadingProtoHTTP,
    ProtocolParseStatePostProto,
    ProtocolParseStateReadingSeperator,
};

/**
 * Checks to see if a URL is "blockable".
 *
 * Blockable URLs are ones that use one of the following protocols (any of
 * which can be prefixed by "blob:")
 *  - http
 *  - https
 *  - ws
 *  - wss
 */
bool isBlockableProtocol(const char *url, int urlLen) {
    // First check to see if this is a blob URL.  If the URL is very short,
    // then trivially it isn't of the above protocols.
    if (urlLen <= 5) {
        return false;
    }

    const char *curChar = url;
    int totalCharsRead = 0;
    int numCharsReadInState;
    char lowerChar;
    ProtocolParseState parseState = ProtocolParseStateStart;

    // The below loop encodes a state machine.  Free transitions between states
    // are continues.  States that consume input "break" so that the can
    // share the common incrementing statements at the bottom of the loop.
    //
    // Its not quite as optimized as possible (some state transitions could
    // be collapsed) but its written in this _slightly_ more verbose way
    // to make it easier to grok.
    while (true) {
        switch (parseState) {
            case ProtocolParseStateStart:
                if (tolower(*curChar) == 'b') {
                    parseState = ProtocolParseStateReadingBlob;
                    continue;
                }
                // Intentional fall through
                [[fallthrough]];
            case ProtocolParseStatePostBlob:
                lowerChar = tolower(*curChar);
                if (lowerChar == 'w') {
                    parseState = ProtocolParseStateReadingProtoWebSocket;
                    continue;
                }
                if (lowerChar == 'h') {
                    parseState = ProtocolParseStateReadingProtoHTTP;
                    continue;
                }
                // If we're in ProtocolParseStateStart and didn't see "blob:",
                // "ws" or "http", or in ProtocolParseStatePostBlob
                // and don't see "ws" or "http" starting, then the URL doesn't match
                // any protocol we're interested in.
                return false;

            case ProtocolParseStateReadingBlob:
                if (tolower(*curChar) == 'b' &&
                    tolower(*(curChar + 1)) == 'l' &&
                    tolower(*(curChar + 2)) == 'o' &&
                    tolower(*(curChar + 3)) == 'b' &&
                    tolower(*(curChar + 4)) == ':') {
                    parseState = ProtocolParseStatePostBlob;
                    numCharsReadInState = 5;
                    break;
                }
                // Unexpected character read when consuming "blob:"
                return false;

            case ProtocolParseStateReadingProtoHTTP:
                if (tolower(*curChar) == 'h' &&
                    tolower(*(curChar + 1)) == 't' &&
                    tolower(*(curChar + 2)) == 't' &&
                    tolower(*(curChar + 3)) == 'p') {
                    parseState = ProtocolParseStatePostProto;
                    numCharsReadInState = 4;
                    break;
                }
                // Unexpected character read when consuming "http"
                return false;

            case ProtocolParseStateReadingProtoWebSocket:
                if (tolower(*curChar) == 'w' &&
                    tolower(*(curChar + 1)) == 's') {
                    parseState = ProtocolParseStatePostProto;
                    numCharsReadInState = 2;
                    break;
                }
                // Unexpected character read when consuming "ws"
                return false;

            case ProtocolParseStatePostProto:
                if (tolower(*curChar) == 's') {
                    parseState = ProtocolParseStateReadingSeperator;
                    numCharsReadInState = 1;
                    break;
                }
                [[fallthrough]];
                // Intentional fall through
            case ProtocolParseStateReadingSeperator:
                if (*curChar == ':' &&
                    (*(curChar + 1)) == '/' &&
                    (*(curChar + 2)) == '/') {
                    return true;
                }
                // Unexpected character read when consuming "://"
                return false;
        }

        // If we've read the entire URL and we haven't been able to determine
        // the protocol, then its trivially not a blockable protocol.
        totalCharsRead += numCharsReadInState;
        if (totalCharsRead >= urlLen) {
            return false;
        }
        curChar += numCharsReadInState;
    }
}

