function convertToVerticalStyle(node) {
    if (node.nodeType === Node.TEXT_NODE) {
        let text = node.nodeValue;
        const regex = /(\d{1,4}\.?|[a-zA-Z]{1,50}\.?)/g;
        let match;
        let lastIndex = 0;
        const fragment = document.createDocumentFragment();

        while ((match = regex.exec(text)) !== null) {
            // exclude english words longer than 2 characters, e.g. "Hello" but keep a. b. c.
            if (match[0].length > 1 && isLetter(match[0][0]) && isLetter(match[0][1])) { continue }

            // digits longer than 4 characters are excluded, e.g. 12345, keep 2024 similar year numbers
            if (match[0].length > 2) {
                if (lastIndex < match.index) {
                    fragment.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
                }

                // Create span element for the matched part
                const span = document.createElement('span');
                span.className = 'verticalSingleChr';
                span.textContent = match[0];
                fragment.appendChild(span);

                lastIndex = regex.lastIndex;
                continue;
            }
            // Create text node for the part before the match
            if (lastIndex < match.index) {
                fragment.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
            }

            // Create span element for the matched part
            const span = document.createElement('span');
            span.className = 'vertical';
            span.textContent = match[0];
            fragment.appendChild(span);

            lastIndex = regex.lastIndex;
        }

        // Append remaining text if any
        if (lastIndex < text.length) {
            fragment.appendChild(document.createTextNode(text.slice(lastIndex)));
        }

        // Replace the original text node with the fragment
        node.parentNode.replaceChild(fragment, node);

    } else {
        node.childNodes.forEach(child => convertToVerticalStyle(child));
    }
}

function isLetter(char) {
    return /[a-zA-Z]/.test(char); // Returns true if the character is a letter
}

convertToVerticalStyle(document.body);