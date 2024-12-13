function toChineseNumeral(num) {
  const numerals = ['○', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
  return num.toString().split('').map(digit => numerals[parseInt(digit)]).join('');
}

function toChineseNumeralForDate(num) {
  const numerals = ['○', '一', '二', '三', '四', '五', '六', '七', '八', '九'];

  if (num < 10) {
    return numerals[num];
  } else if (num < 20) {
    return '十' + (num === 10 ? '' : numerals[num - 10]);
  } else {
    const tens = Math.floor(num / 10);
    const units = num % 10;
    return (tens === 1 ? '十' : numerals[tens] + '十') + (units === 0 ? '' : numerals[units]);
  }
}

function convertDateToChinese(dateString) {
  return convertDayToChinese(convertMonthToChinese(convertYearToChinese(dateString)));
}

function convertDayToChinese(dateString) {
  const datePattern = /\s*(\d{1,2})\s*日/g;
  return dateString.replace(datePattern, (match, day) => {
    const chineseDay = toChineseNumeralForDate(parseInt(day));
    return `${chineseDay}日`;
  });
}

function convertMonthToChinese(dateString) {
  const datePattern = /\s*(\d{1,2})\s*月/g;
  return dateString.replace(datePattern, (match, month) => {
    const chineseMonth = toChineseNumeralForDate(parseInt(month));
    return `${chineseMonth}月`;
  });
}

function convertYearToChinese(dateString) {
  const datePattern = /(\d{1,4})\s*年/g;
  return dateString.replace(datePattern, (match, year) => {
    console.log(year);
    const chineseYear = year.split('').map(digit => toChineseNumeral(digit)).join('');
    console.log('chineseYear', chineseYear);
    return `${chineseYear}年`;
  });
}

function traverseAndConvertDates(node) {
  if (node.nodeType === Node.TEXT_NODE) {
    node.textContent = convertDateToChinese(node.textContent);
  } else {
    node.childNodes.forEach(traverseAndConvertDates);
  }
}

traverseAndConvertDates(document.body);

function convertToVerticalStyle(node) {
    if (node.nodeType === Node.TEXT_NODE) {
        let text = convertToFullWidth(node.nodeValue);
        // 1.1 | 1. | 5G | 2024 | a. | 這是 ABC 的測試
        const regex = /(\b\d\.\d\b|\d{1}\.(?=\s|\b)|\b\d[a-zA-Z]{1}|\d{1,4}|\b[a-zA-Z]{1}\.(?!\s+[a-zA-Z])|(?<![a-zA-Z]+\s+)\b[a-zA-Z]{1,4}(?![a-zA-Z])(?!\s+[a-zA-Z]))/g;
        let match;
        let lastIndex = 0;
        const fragment = document.createDocumentFragment();

        while ((match = regex.exec(text)) !== null) {
            let matchedText = match[0];
            // exclude english words longer than 2 characters, e.g. "Hello" but keep a. b. c.
            if (matchedText.length > 4 && isLetter(matchedText[0]) && isLetter(matchedText[1])) { continue }
            if (matchedText.length == 3 && isAllCapitalLetter(matchedText)) { continue }

            // digits longer than 3 characters , e.g. 12345, keep 2024 similar year numbers
            if (match[0].length >= 3) {
                //console.log("matchedText.length >= 3");
                //console.log(matchedText);
                if (matchedText[1] != '.') {
                    if (lastIndex < match.index) {
                        fragment.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
                    }

                    // Create span element for the matched part
                    const span = document.createElement('span');
                    span.className = 'verticalSingleChr';
                    span.textContent = matchedText;
                    fragment.appendChild(span);

                    lastIndex = regex.lastIndex;
                    continue;
                }
            }
            // Create text node for the part before the match
            if (lastIndex < match.index) {
                fragment.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
            }

            // prevent duplicate vertical text
            const parentNode = node.parentNode;
            const isAlreadyWrapped = parentNode && parentNode.classList && parentNode.classList.contains('vertical');

            if (!isAlreadyWrapped) {
                // Create span element for the matched part
                const span = document.createElement('span');
                span.className = 'vertical';
                span.textContent = matchedText;
                fragment.appendChild(span);
            } else {
                fragment.appendChild(document.createTextNode(match[0]));
            }

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

function convertToFullWidth(str) {
  return str.replace(/%/g, '％').replace(/\,/g, '，').replace(/\?/g, '？').replace(/\!/g, '！').replace(/:/g, '：').replace(/\//g, '／');
}

function isAllCapitalLetter(str) {
    for (let i = 0; i < str.length; i++) {
        if (!isLetter(str[i])) {
            return false;
        }
    }
    return str === str.toUpperCase();
}

function isLetter(char) {
    return /[a-zA-Z]/.test(char); // Returns true if the character is a letter
}

function convertListLabelToVertical(node) {
    const listItems = node.querySelectorAll('li');
    listItems.forEach(listItem => {
        const label = listItem.querySelector('span.vertical');
        if (label) {
            label.classList.add('listLabel');
        }
    });
}

convertToVerticalStyle(document.body);

// Helper function to get the list based on the list style type
function convertListLabel(node) {
   function getList(listType) {
       const lists = {
           "upper-alpha": ["\u30A2","\u30A4","\u30A6","\u30A8","\u30AA","\u30AB","\u30AD","\u30AF","\u30B1","\u30B3","\u30B5","\u30B7","\u30B9","\u30BB","\u30BD","\u30BF","\u30C1","\u30C4","\u30C6","\u30C8","\u30CA","\u30CB","\u30CC","\u30CD","\u30CE","\u30CF","\u30D2","\u30D5","\u30D8","\u30DB","\u30DE","\u30DF","\u30E0","\u30E1","\u30E2","\u30E4","\u30E6","\u30E8","\u30E9","\u30EA","\u30EB","\u30EC","\u30ED","\u30EF","\u30F0","\u30F1","\u30F2","\u30F3"],
           "hiragana": ["\u3042","\u3044","\u3046","\u3048","\u304A","\u304B","\u304D","\u304F","\u3051","\u3053","\u3055","\u3057","\u3059","\u305B","\u305D","\u305F","\u3061","\u3064","\u3066","\u3068","\u306A","\u306B","\u306C","\u306D","\u306E","\u306F","\u3072","\u3075","\u3078","\u307B","\u307E","\u307F","\u3080","\u3081","\u3082","\u3084","\u3086","\u3088","\u3089","\u308A","\u308B","\u308C","\u308D","\u308F","\u3090","\u3091","\u3092","\u3093"],
           "katakana-iroha": ["\u30A4","\u30ED","\u30CF","\u30CB","\u30DB","\u30D8","\u30C8","\u30C1","\u30EA","\u30CC","\u30EB","\u30F2","\u30EF","\u30AB","\u30E8","\u30BF","\u30EC","\u30BD","\u30C4","\u30CD","\u30CA","\u30E9","\u30E0","\u30A6","\u30F0","\u30CE","\u30AA","\u30AF","\u30E4","\u30DE","\u30B1","\u30D5","\u30B3","\u30A8","\u30C6","\u30A2","\u30B5","\u30AD","\u30E6","\u30E1","\u30DF","\u30B7","\u30F1","\u30D2","\u30E2","\u30BB","\u30B9"],
           "hiragana-iroha": ["\u3044","\u308D","\u306F","\u306B","\u307B","\u3078","\u3068","\u3061","\u308A","\u306C","\u308B","\u3092","\u308F","\u304B","\u3088","\u305F","\u308C","\u305D","\u3064","\u306D","\u306A","\u3089","\u3080","\u3046","\u3090","\u306E","\u304A","\u304F","\u3084","\u307E","\u3051","\u3075","\u3053","\u3048","\u3066","\u3042","\u3055","\u304D","\u3086","\u3081","\u307F","\u3057","\u3091","\u3072","\u3082","\u305B","\u3059"],
           "upper-roman": ["I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX"],
           "lower-roman": ["i","ii","iii","iv","v","vi","vii","viii","ix","x","xi","xii","xiii","xiv","xv","xvi","xvii","xviii","xix","xx"],
           "decimal": ["1","2","3","4","5","6","7","8","9","10"],
           "cjk-ideographic": ["\u4E00","\u4E8C","\u4E09","\u56DB","\u4E94","\u516D","\u4E03","\u516B","\u4E5D","\u5341"],
           "\u58F1": ["\u58F1","\u5F10","\u53C2","\u56DB","\u4F0D","\u516D","\u4E03","\u516B","\u4E5D","\u62FE"]
       };

       if (listType == "A" || listType == "\u30A2" || listType == "upper-latin" || listType == "katakana") {
              return lists["upper-alpha"];
       } else if (listType == "a" || listType == "\u3042" || listType == "lower-latin" || listType == "hiragana") {
              return lists["hiragana"];
		} else if (listType == "\u30A4" || listType == "katakana-iroha") {
		     return lists["katakana-iroha"];
		} else if (listType == "\u3044" || listType == "hiragana-iroha") {
		      return lists["hiragana-iroha"];
		} else if (listType == "I" || listType == "upper-roman") {
		      return lists["upper-roman"];
		} else if (listType == "i" || listType == "lower-roman") {
		      return lists["lower-roman"];
		} else if (listType == "none" || listType == "1" || listType == "decimal" || listType == "\u4E00" || listType == "cjk-ideographic" || listType == "\u58F1") {
        	  return (listType == "\u58F1") ? lists["\u58F1"] : lists["cjk-ideographic"];
	    } else {
	         return [];
       }
   }

   // Function to convert a single label based on the given list style type and index
   function convertLabel(listType, index) {
       let list = getList(listType);
       if (list.length === 0) {
           return "";
       }
       if (listType == "none" || listType == "1" || listType == "decimal") {
           return `${index+1}.`;
       } else if (listType == "\u4E00" || listType == "cjk-ideographic" || listType == "\u58F1") {
           return index <= 10 ? `(${list[index]})` : `(${index}).`;
       }
       return `(${list[index]})`;
   }

   // Main function logic
   const olLists = node.querySelectorAll("ol");

   olLists.forEach((ol) => {
       const listStyleType = window.getComputedStyle(ol).listStyleType;
       //console.log(listStyleType);
       const listItems = ol.querySelectorAll("li");
       listItems.forEach((li, index) => {
           //console.log(li.textContent);
           const marker = convertLabel(listStyleType, index);
           li.setAttribute("data-marker", marker);
       });
   });
}

// Run the function on the document body
convertListLabel(document.body);

function changeOlToOlCjk(node) {
    node.querySelectorAll("ol").forEach((ol) => { ol.classList.add("cjk"); });
}

changeOlToOlCjk(document.body);

