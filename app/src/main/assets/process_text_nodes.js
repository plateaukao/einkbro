function convertToVerticalStyle(node) {
    if (node.nodeType === Node.TEXT_NODE) {
        let text = node.nodeValue;
        const regex = /(\d{1,4})/g;
        let match;
        let lastIndex = 0;
        const fragment = document.createDocumentFragment();

        while ((match = regex.exec(text)) !== null) {
            if (match[0].length > 2) {
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
convertToVerticalStyle(document.body);