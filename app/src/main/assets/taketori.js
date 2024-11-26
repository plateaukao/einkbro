/* Taketori - Make Text Vertical 
 * Copyright 2010-2015 CMONOS Co. Ltd. (http://cmonos.jp)
 *
 * Version: 1.4.4
 * Lisence: MIT Lisence
 * Last-Modified: 2015-11-08
 */


var TaketoriDblClickAlert = {	// should be unicode entity for Opera.
	'ja-jp' : "\uFEFF\u7E26\u66F8\u304D\u5316\u3057\u305F\u3044\u90E8\u5206\u3092\u30C0\u30D6\u30EB\u30AF\u30EA\u30C3\u30AF\u3057\u3066\u304F\u3060\u3055\u3044\u3002",
	'zh-tw' : "\u9ede\u5169\u4e0b\u4ee5\u5207\u63db\u5230\u76f4\u6392\u986f\u793a\u3002",
	'zh-cn' : "Double click to make text vertical.",
	'ko-kr' : "Double click to make text vertical."
};
var TaketoriDefaultLang = (document.getElementsByTagName('html')[0] && document.getElementsByTagName('html')[0].lang) ? 
			(document.getElementsByTagName('html')[0].lang.search(/tw/i) != -1) ? "zh-tw"
//		:	(document.getElementsByTagName('html')[0].lang.search(/(zh|cn)/i) != -1) ? "zh-cn"
//		:	(document.getElementsByTagName('html')[0].lang.search(/(ko|kr)/i) != -1) ? "ko-kr"
		:	"ja-jp"
	:	"ja-jp";
//var TaketoriDefaultLang = "ja-jp"; // or .. simply set your language.

var TaketoriTool = function () {};
TaketoriTool.prototype = {

	element : function(element) {
		if (arguments.length > 1) element = Array.prototype.slice.call(arguments);
		this.elements = new Array();
		return this.appendElement(element);
	},

	list : function () { return (this.elements) ? this.elements : [] },

	appendElement : function(element) {
		if (arguments.length > 1) element = Array.prototype.slice.call(arguments);
		if (!element) return this;
		if (!this.elements) this.elements = new Array();
		if (element.constructor == Array) {
			for (var i=0; i<element.length; i++) {
				this.appendElement(element[i]);
			}
		} else if (typeof element == 'object') {
			if (!this.isIncluded(element)) this.elements[this.elements.length] = element;
		} else if (typeof element == "string") {
			if (element.match(/^(\w+)\.([\w\-]+)$/)) {
				var tag = RegExp.$1.toLowerCase();
				var cName = RegExp.$2;
				var elements = new Array();
				if (typeof document.getElementsByClassName == "function") {
					elements = document.getElementsByClassName(cName);
					for (var i=0; i<elements.length; i++) {
						if (elements[i].tagName.toLowerCase() == tag && !this.isIncluded(elements[i])) this.elements[this.elements.length] = elements[i];
					}
				}
				if (typeof document.getElementsByClassName != "function" || elements.length == 0) {
					elements = document.getElementsByTagName(tag);
					var regexp = new RegExp("(^|\\s)"+cName+"(?![\\w\\-])");
					for (var i=0; i<elements.length; i++) {
						if (elements[i].className.search(regexp) != -1 && !this.isIncluded(elements[i])) this.elements[this.elements.length] = elements[i];
					} 
				}
			} else {
				element = document.getElementById(element.replace('#',''));
				if (element && !this.isIncluded(element)) this.elements[this.elements.length] = element;
			}
		}
		return this;
	},

	isIncluded : function(value) {
		for (var i=0; i < this.elements.length; i++) {
			if (this.elements[i] == value) return true;
		}
		return false;
	},

	removeClassName : function(cName) {
		return this.replaceClassName(cName,'','ig');
	},

	replaceClassName : function(cName,newName,flag) {
		if (this.elements && cName != null && cName != "") {
			var regexp = new RegExp("(^|\\s)"+cName+"(?![\\w\\-])",flag);
			if (newName == null) newName = '';
			for (var i=0; i < this.elements.length; i++) {
				if (this.elements[i].className != null) this.elements[i].className = this.elements[i].className.replace(regexp,newName);
			}
		}
		return this;
	},

	addClassName : function(cName) {
		if (this.elements && cName != null && cName != "") {
			var regexp = new RegExp("(^|\\s)"+cName+"(?![\\w\\-])");
			for (var i=0; i < this.elements.length; i++) {
				if (this.elements[i].className == null || this.elements[i].className == "") {
					this.elements[i].className = cName;
				} else if (this.elements[i].className.search(regexp) == -1) {
					this.elements[i].className += ' ' + cName;
				}
			}
		}
		return this;
	},

	addEventListener : function(type,func,capture) {
		if (capture == null) capture = false;
		if (this.elements) {
			for (var i=0; i < this.elements.length; i++) {
				if (typeof this.elements[i].addEventListener =='function') {
					this.elements[i].addEventListener(type, func, capture);
				} else if (typeof this.elements[i].attachEvent == 'object') {
					this.elements[i].attachEvent("on" + type, func);
				}
			}
		}
		return func;
	},

	removeEventListener : function(type,func,capture) {
		if (this.elements) {
			for (var i=0; i < this.elements.length; i++) {
				if (typeof this.elements[i].removeEventListener == 'function') {
					this.elements[i].removeEventListener(type,func,capture);
				} else if (this.elements[i].detachEvent) {
					this.elements[i].detachEvent("on" + type, func);
				}
			}
		}
	},

	stopPropagation : function(e) {
		if (e.stopPropagation) {
			e.stopPropagation();
		} else {
			event.cancelBubble = true;
		}
	},

	preventDefault : function (e) {
		if (e.preventDefault) {
			e.preventDefault();
		} else {
			event.returnValue = false;
		}
	}
};

var Taketori = function () {};
Taketori.prototype = {
	isMSIE : ((document.documentMode) ? document.documentMode : (navigator.appVersion.search(/MSIE/) != -1) ? 5 : 0),
	isWritingModeReady : ((
							navigator.appVersion.search(/MSIE/) != -1 || document.documentMode
						 || typeof (document.createElement('div')).style.MozWritingMode != 'undefined'
						 || (typeof (document.createElement('div')).style.webkitWritingMode != 'undefined' && (!window.devicePixelRatio || window.devicePixelRatio < 2 || navigator.userAgent.search(/Chrome\/([0-9]+)/) == -1 || parseInt(RegExp.$1) > 30))
						 || typeof (document.createElement('div')).style.OWritingMode != 'undefined'
						 ) ? true : false),
	isMultiColumnReady : ((
							typeof (document.createElement('div')).style.MozColumnWidth != 'undefined'
						 || typeof (document.createElement('div')).style.webkitColumnWidth != 'undefined'
						 || (typeof (document.createElement('div')).style.webkitWritingMode != 'undefined' && typeof (document.createElement('div')).style.webkitColumnWidth != 'undefined')
						 || typeof (document.createElement('div')).style.OColumnWidth != 'undefined'
						 || typeof (document.createElement('div')).style.msColumnWidth != 'undefined'
						 || typeof (document.createElement('div')).style.columnWidth != 'undefined'
						 ) ? true : false),
	isLegacy : ((navigator.appVersion.search(/MSIE/) != -1
			 || typeof (document.createElement('div')).style.MozTransform != 'undefined'
			 || typeof (document.createElement('div')).style.webkitTransform != 'undefined'
			 || typeof (document.createElement('div')).style.OTransform != 'undefined'
			 || typeof (document.createElement('div')).style.transform != 'undefined') ? false : true),
	isTextEmphasisReady : ((
							typeof (document.createElement('div')).style.MozTextEmphasisStyle != 'undefined'
						 || typeof (document.createElement('div')).style.webkitTextEmphasisStyle != 'undefined'
						 || typeof (document.createElement('div')).style.OTextEmphasisStyle != 'undefined'
						 || typeof (document.createElement('div')).style.msTextEmphasisStyle != 'undefined'
						 || typeof (document.createElement('div')).style.textEmphasisStyle != 'undefined'
						 ) ? true : false),
	supportTouch : ((((!window.navigator.msPointerEnabled && !window.navigator.pointerEnabled) || navigator.userAgent.search(/iphone|ipad|android/i) != -1) && ('createTouch' in document || 'ontouchstart' in document)) ? true : false),

	document : (new TaketoriTool()),

	importCSS : function() {
		if (this.cssImported) return true;
		this.cssImported = false;
		var links = document.getElementsByTagName('link');
		for (var i=0; i<links.length; i++) {
			if (links[i].href != null && links[i].href.search(/taketori\.css$/) != -1) {
				this.cssImported = true;
			}
		}
		if (this.cssImported) return true;
		if (this.config.cssPath == null || this.config.cssPath == 'auto') {
			var scripts = document.getElementsByTagName('script');
			for (var i=0; i<scripts.length; i++) {
				if (scripts[i].src != null && scripts[i].src.search(/taketori\.js$/) != -1) {
					this.config.cssPath = scripts[i].src.replace('taketori.js','');
					break;
				}
			}
		}
		var header = document.getElementsByTagName('head')[0];
		if (header) {
			var link;
			link = document.createElement('link');
			link.setAttribute('rel','stylesheet');
			link.setAttribute('type','text/css');
			link.setAttribute('href',this.config.cssPath + 'taketori.css');
			header.appendChild(link);
		} else {
			document.write('<link rel="stylesheet" type="text/css" href="' + this.config.cssPath + 'taketori.css" />');
		}
		this.cssImported = true;
	},

	setTemporaryCSS : function(cssText) {
		if (!this.temporaryStyle) {
			this.temporaryStyle = document.createElement('style');
			this.temporaryStyle.setAttribute('type','text/css');
			document.getElementsByTagName('head')[0].appendChild(this.temporaryStyle);
		}
		this.temporaryStyle.appendChild(document.createTextNode(cssText));
	},

	removeTemporaryCSS : function() {
		if (this.temporaryStyle) {
			document.getElementsByTagName('head')[0].removeChild(this.temporaryStyle);
			this.temporaryStyle = null;
		}
	},

	init : function() {
		if (!this.windowWidth) {
			var taketori = this;
			this.document.element(window).addEventListener('resize', function () {
				if (taketori.resizeTimer) clearTimeout(taketori.resizeTimer);
				taketori.resizeTimer = setTimeout(function () { taketori.refresh(); },500);
			});
		}
		this.windowWidth = ((self.innerWidth) ? self.innerWidth : (document.compatMode != null && document.compatMode != "BackCompat") ? self.document.documentElement.clientWidth : self.document.body.clientWidth);
		this.windowHeight = ((self.innerHeight) ? self.innerHeight : (document.compatMode != null && document.compatMode != "BackCompat") ? self.document.documentElement.clientHeight : self.document.body.clientHeight);
		if (!this.isMSIE && this.rubyDisabled == null) {
			if (!!window.opera) {
				this.rubyDisabled = true;
			} else {
				var ruby = document.createElement('ruby');
				var rubyDisplay = this.getStyle(ruby).display;
				this.rubyDisabled = (rubyDisplay == 'block') ? true : false;
			}
		}
	},

	set : function (hash) {
		if (this.isLegacy) return this;
		if (!this.config) this.config = {};
		if (hash != null) this.merge(this.config,hash);
		if (this.config.lang) this.config.lang = this.config.lang.toLowerCase();
		return this;
	},

	merge : function (original,hash) {
		if (!original) original = {};
		if (hash != null) {
			for (var key in hash) {
				if (hash[key] != null) {
					if (hash[key].constructor == Array) {
						this.merge(original[key],hash[key]);
					} else if (typeof hash[key] == "string" || typeof hash[key] == "number" || typeof hash[key] == "boolean") {
						original[key] = hash[key];
					}
				}
			}
		}
		return original;
	},

	element : function (element) {
		if (this.isLegacy) return this;
		if (arguments.length > 1) element = Array.prototype.slice.call(arguments);
		if (!this.config.elements) this.config.elements = new Array();
		if (element && element.constructor == Array) {
			for (var i=0; i<element.length; i++) {
				this.config.elements[this.config.elements.length] = element[i];
			}
		} else if (element) {
			this.config.elements[this.config.elements.length] = element;
		}
		return this;
	},

	reset : function (hash) {
		this.config = null;
		this.set(hash);
	},

	japaneseReadableElements : function (minScore) {
		var elements = new Array();
		var target = new Array();
		var temp = document.getElementsByTagName('p');
		for (var i=0; i<temp.length; i++) {
			target[target.length] = temp[i];
		}
		temp = document.getElementsByTagName('br');
		for (var i=0; i<temp.length; i++) {
			target[target.length] = temp[i];
		}
		for (var i=0; i<target.length; i++) {
			var p = this.lookUpBlockElements(target[i],elements);
			if (p != null && !this.isCountedElement(elements,p)) {
				var text = p.innerHTML;
				var score = 0;
				text.replace(/\u3002/g,function () { score++ } ).replace(/\u3001/g,function () { score+=0.5 } );
				elements[elements.length] = {
					element : p,
					readabilityScore : score
				};
			}
		}
		if (elements.length > 0) {
			elements = elements.sort(function (a,b) { return b.readabilityScore - a.readabilityScore });
			var readableElements = new Array();
			for (var i=0; i<elements.length; i++) {
				if (!this.isVerticalTextElement(elements[i].element) && (!minScore || minScore < elements[i].readabilityScore)) {
					readableElements[readableElements.length] = elements[i].element;
				}
			}
			return readableElements;
		} else {
			return [document.body];
		}
	},

	isVerticalTextElement : function (element) {
		var style = this.getStyle(element);
		return ((navigator.appVersion.search(/MSIE/) != -1 && typeof style.writingMode == 'string' && (style.writingMode.toLowerCase() == 'tb-rl' || style.writingMode.toLowerCase() == 'vertical-rl'))
				 || (typeof style.MozWritingMode == 'string' && style.MozWritingMode.toLowerCase() == 'vertical-rl')
				 || (typeof style.webkitWritingMode == 'string' && style.webkitWritingMode.toLowerCase() == 'vertical-rl')
				 || (typeof style.OWritingMode == 'string' && style.OWritingMode.toLowerCase() == 'vertical-rl')
				 || (typeof style.writingMode == 'string' && style.writingMode.toLowerCase() == 'vertical-rl')
		) ? true : false;
	},

	isCountedElement : function (elements,element) {
		for (var i=0; i<elements.length; i++) {
			if (elements[i].element == element) {
				return true;
			}
		}
		return false;
	},

	lookUpBlockElements : function (p) {
		var blockElement = null;
		while (p) {
			var tagName = p.tagName.toLowerCase();
			if (tagName == 'div' || tagName == 'td' || tagName == 'th' || tagName == 'dd' || tagName == 'section' || tagName == 'article' || tagName == 'body') {
				if (p.taketori || !blockElement) {
					blockElement = p;
				}
				if (tagName == 'body') break;
			}
			p = p.parentNode;
		}
		return blockElement;
	},


	isIncludedIn : function(array,value) {
		if (array && array.constructor == Array) {
			for (var i=0; i < array.length; i++) {
				if (array[i] == value) return true;
			}
		}
		return false;
	},

	escapeHTML : function (str) {
		var div = document.createElement('div');
		var text = document.createTextNode(str);
		div.appendChild(text);
		return div.innerHTML;
	},

	unescapeHTML : function (str) {
		return (str == null) ? '' : str.replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"').replace(/&amp;/g,'&');
	},

	getCookie : function(name) {
		var cookies = new Array();
		var cookiePairs = document.cookie.split(/;\s*/);
		for (var i=0; i < cookiePairs.length; i++) {
			if (cookiePairs[i] != "") {
				var eq = cookiePairs[i].indexOf("=");
				if (eq >= 0) {
					var key = cookiePairs[i].substring(0, eq);
					var value = cookiePairs[i].substring(eq+1, cookiePairs[i].length);
					cookies[key] = value;
				}
			}
		}
		return (name != null && cookies[name] != null) ? cookies[name] : "";
	},

	setCookie : function(name,value,expires) {
		if (!expires) {
			if (this.config.cookieExpires) {
				expires = this.config.cookieExpires;
			} else {
				expires = new Date();
				expires.setTime(expires.getTime() + 86400000000);
			}
		}
		document.cookie = name + '=' + value + '; expires=' + expires.toGMTString() + ';' + ((this.config.cookieDomain != null && this.config.cookieDomain != "") ? ' domain=' + this.config.cookieDomain + ';' : '') + ((this.config.cookiePath != null && this.config.cookiePath != "") ? ' path=' + this.config.cookiePath : '');
	},

	deleteCookie : function(name) {
		this.setCookie(name, '', new Date(70, 0, 1, 0, 0, 1));
	},

	setOnLoad : function(func) {
		if (this.isLegacy) return this;
		if (typeof window.addEventListener == 'function') {
			window.addEventListener('load', func, false);
		} else if (typeof window.attachEvent == 'object') {
			window.attachEvent('onload', ((!this.isMultiColumnReady) ? function () { setTimeout(func,600) } : func));
		}
		return this;
	},

	getStyle : function(element) {
		if (element.currentStyle) {
			return element.currentStyle;
		} else {
			var style = document.defaultView.getComputedStyle(element, null);
			var div = document.createElement('div');
			if (style.length) {
				for (var i=0; i<style.length; i++) {
					div.style.setProperty(style[i],style.getPropertyValue(style[i]),'');
				}
				for (var i=0; i<element.style.length; i++) {
					div.style.setProperty(element.style[i],element.style.getPropertyValue(element.style[i]),'');
				}
			} else {
				div.style.cssText = style.cssText + element.style.cssText;
			}
			return div.style;
		}
	},

	counterClockwiseRotatedOuterHTML : function(element,style,cssTextOnly) {
		var temp;
		var isBlock = (style.display != 'inline' || (style.cssFloat && style.cssFloat != 'none') || (style.styleFloat && style.styleFloat != 'none')) ? true : false;
		var swapWH = false;
		if (!style) style = this.getStyle(element);
		if (cssTextOnly) {
			temp = document.createElement('div');
		} else {
			temp = element.cloneNode(true);
		}
		if (this.isWritingModeReady && cssTextOnly) {
			if (style.width && style.width.search(/\d+\.?\d*(px|em)/i) != -1 && parseInt(style.width) > 0 && style.height && style.height.search(/\d+\.?\d*(px|em)/i) != -1 && parseInt(style.height) > 0) {
				if (element.currentStyle || (style.cssFloat && style.cssFloat != 'none') || (style.display == 'inline-block')) { 
					swapWH = true;
				} else {
					temp.style.width = 'auto';
					temp.style.height = 'auto';
				}
			} else if ((style.width && style.width.search(/\d+\.?\d*%/) != -1) || (style.height && style.height.search(/\d+\.?\d*%/) != -1)) {
				temp.style.width = (style.height && style.height.search(/\d+\.?\d*%/) != -1) ? style.height : 'auto';
				temp.style.height = (style.width && style.width.search(/\d+\.?\d*%/) != -1) ? style.width : 'auto';
			} else {
				if (style.width == '0px') temp.style.width = 'auto';
				if (style.height == '0px') temp.style.height = 'auto';
			}
		}
		if (!this.isWritingModeReady || swapWH || (this.isWritingModeReady && !cssTextOnly)) {
			var mw = 0;
			var mh = 0;
			var noRotate = false;
			if (this.isWritingModeReady) {
				mw = this.getMarginSize([style.paddingLeft,style.paddingRight,style.borderLeftWidth,style.borderRightWidth]);
				mh = this.getMarginSize([style.paddingTop,style.paddingBottom,style.borderTopWidth,style.borderBottomWidth]);
				noRotate = true;
			} else {
				mw = this.getMarginSize([style.paddingTop,style.paddingBottom,style.borderTopWidth,style.borderBottomWidth]);
				mh = this.getMarginSize([style.paddingLeft,style.paddingRight,style.borderLeftWidth,style.borderRightWidth]);
			}
			var w = (!isNaN(parseInt(style.width)) && parseInt(style.width) > 0) ? parseInt(style.width) : (element.width) ? element.width : (isBlock && element.offsetWidth) ? element.offsetWidth - ((noRotate) ? mw : mh) : null;
			var h = (!isNaN(parseInt(style.height)) && parseInt(style.height) > 0) ? parseInt(style.height) : (element.height) ? element.height : (isBlock && element.offsetHeight) ? element.offsetHeight - ((noRotate) ? mh : mw) : null;
			var resized = false;
			if (!w || !h) {
				var clone = (cssTextOnly) ? element.cloneNode(true) : temp;
				this.makeClipboard(this.process.target,true);
				this.clipboard.appendChild(clone);
				w = (clone.offsetWidth) ? clone.offsetWidth - ((noRotate) ? mw : mh) : clone.clientWidth;
				h = (clone.offsetHeight) ? clone.offsetHeight - ((noRotate) ? mh : mw) : clone.clientHeight;
				this.clipboard.removeChild(clone);
			}
			if (!isNaN(w) && !isNaN(h)) {
				w += mw;
				h += mh;
				if (h > this.process.config.height) {
					w = parseInt(w * this.process.config.height / h);
					h = this.process.config.height;
					resized = true;
				}
				if (w > this.process.config.width) {
					h = parseInt(h * this.process.config.width / w);
					w = this.process.config.width;
					resized = true;
				}
				if (resized || swapWH) {
					var cw = w - mw;
					var ch = h - mh;
					if (swapWH) {
						if (element.currentStyle || (style.display == 'inline-block')) { 
							temp.width = ch;
							temp.style.width = ch + 'px';
						} else {
							temp.style.width = 'auto';
						}
						temp.height = cw;
						temp.style.height = cw + 'px';
					} else {
						temp.width = cw;
						temp.height = ch;
						temp.style.width = cw + 'px';
						temp.style.height = ch + 'px';
						if (resized && isBlock) temp.style.overflow = 'auto';
					}
				}
			}
		}
		temp.style.borderTopWidth = style.borderLeftWidth || 0;
		temp.style.borderRightWidth = style.borderTopWidth || 0;
		temp.style.borderBottomWidth = style.borderRightWidth || 0;
		temp.style.borderLeftWidth = style.borderBottomWidth || 0;
		temp.style.borderTopColor = style.borderLeftColor || '';
		temp.style.borderRightColor = style.borderTopColor || '';
		temp.style.borderBottomColor = style.borderRightColor || '';
		temp.style.borderLeftColor = style.borderBottomColor || '';
		temp.style.borderTopStyle = style.borderLeftStyle || 'none';
		temp.style.borderRightStyle = style.borderTopStyle || 'none';
		temp.style.borderBottomStyle = style.borderRightStyle || 'none';
		temp.style.borderLeftStyle = style.borderBottomStyle || 'none';
		if (isBlock || !cssTextOnly || (this.isWritingModeReady && (!this.isMSIE || this.isMSIE < 8))) {
			temp.style.paddingTop = style.paddingLeft || 0;
			temp.style.paddingRight = style.paddingTop || 0;
			temp.style.paddingBottom = style.paddingRight || 0;
			temp.style.paddingLeft = style.paddingBottom || 0;
		}
		if (this.isWritingModeReady) {
			if (isBlock && cssTextOnly) {
				temp.style.overflowX = (style.overflowY) ? style.overflowY : 'visible';
				temp.style.overflowY = (style.overflowX) ? style.overflowX : 'visible';
			}
			if (style.backgroundPositionX || style.backgroundPositionY) {
				temp.style.backgroundPositionX = (style.backgroundPositionY) ? (style.backgroundPositionY == 'top') ? 'left' : (style.backgroundPositionY == 'bottom') ? 'right' : style.backgroundPositionY : '50%';
				temp.style.backgroundPositionY = (style.backgroundPositionX) ? (style.backgroundPositionX == 'left') ? 'top' : (style.backgroundPositionX == 'right') ? 'bottom' : style.backgroundPositionX : '50%';
			} else if (style.backgroundPosition && style.backgroundPosition.search(/(\S+)\s+(\S)/) != -1) {
				temp.style.backgroundPosition = RegExp.$2 + ' ' + RegExp.$1 + ';';
			}
			if (isBlock || !cssTextOnly || !this.isMSIE || this.isMSIE < 8) {
				temp.style.marginTop = style.marginLeft || 0;
				temp.style.marginRight = style.marginTop || 0;
				temp.style.marginBottom = style.marginRight || 0;
				temp.style.marginLeft = style.marginBottom || 0;
			}
		} else {
			temp.style.marginTop = parseInt(((style.marginTop && style.marginTop != 'auto') ? parseInt(style.marginTop) : 0) + ((w - h)/2)) + 'px';
			temp.style.marginRight = parseInt(((style.marginRight && style.marginRight != 'auto') ? parseInt(style.marginRight) : 0) + ((h - w)/2)) + 'px';
			temp.style.marginBottom = parseInt(((style.marginBottom && style.marginBottom != 'auto') ? parseInt(style.marginBottom) : 0) + ((w - h)/2)) + 'px';
			temp.style.marginLeft = parseInt(((style.marginLeft && style.marginLeft != 'auto') ? parseInt(style.marginLeft) : 0) + ((h - w)/2)) + 'px';
			if (style.backgroundPosition && style.backgroundPosition.search(/(\S+)\s+(\S)/) != -1) {
				temp.style.backgroundPosition = RegExp.$2 + ' ' + RegExp.$1 + ';';
			}
		}
		return (cssTextOnly) ? temp.style.cssText + ';' + element.style.cssText : this.outerHTML(temp);
	},

	outerHTML : function(element) {
		if (!element) return '';
		var div = document.createElement('div');
		div.appendChild(element.cloneNode(true));
		return div.innerHTML;
	},

	rotatedBlockAdditionalCSSText : function(w,h,g,o) {
		if (g == null) g = 0;
		if (this.isWritingModeReady) {
			return 'width:' + h + 'px;'
				 + 'height:' + w + 'px;'
				 + ((g) ? ((this.isMSIE && this.isMSIE == 9) ? (!o) ? 'padding-right:' : 'margin-right:' : (!o) ? 'padding-bottom:' : 'margin-bottom:') + g + 'px;' : '')
				 + ((!o) ? 'position:relative;overflow:auto;overflow-x:auto;overflow-y:hidden;' : '');
		} else {
			return 'width:' + w + 'px;'
				 + 'height:' + h + 'px;'
				 + 'margin-top:-' + h + 'px;'
				 + 'margin-right:' + (h - w - g) + 'px;'
				 + 'margin-bottom:' + (w + g) + 'px;'
				 + 'margin-left:0;'
				 + ((!o) ? 'padding-right:'+g+'px;overflow:auto;overflow-x:visible;overflow-y:auto;' : '');
		}
	},

	toVertical : function(wait,currentConfig) {
		if (this.isLegacy) return this;
		var taketori = this;
		if (wait == null || wait) {
			this.importCSS();
			this.setOnLoad(function () { taketori.toVertical(false,currentConfig) });
		} else if (!this.cssImported) {
			this.importCSS();
			setTimeout(function () { taketori.toVertical(false,currentConfig) },120);
		} else {
			var setDblClickEvent = false;
			var setOnly = false;
			if (!this.targetElements) {
				if (!this.config.elements || this.config.elements[0] == '=auto') {
					this.targetElements = this.japaneseReadableElements((this.config.minScore == null) ? 3 : this.config.minScore);
					this.config.togglable = false;
					setDblClickEvent = true;
				} else if (this.config.elements[0] == '=dblclick') {
					setOnly = setDblClickEvent = true;
				} else {
					var targets = this.document.element(this.config.elements).list();
					this.targetElements = new Array();
					for (var i=0; i<targets.length; i++) {
						if (!this.isVerticalTextElement(targets[i])) {
							this.targetElements[this.targetElements.length] = targets[i];
						}
					}
				}
			}
			var event_handler = (this.supportTouch) ? 'touchstart' : 'dblclick';
			if (setDblClickEvent) {
				if (!this.toggleEventAttached) {
					this.document.element(document.body).addEventListener(event_handler,function(e) {
						e = e || event;
						if (taketori.supportTouch && e.touches) {
							var clicked = (taketori.dblClickTimer) ? true : false;
							e = e.touches[0];
							if (clicked) {
								clearTimeout(taketori.dblClickTimer);
								delete taketori.dblClickTimer;
								if (Math.abs(taketori.touchX - e.pageX) > 50 || Math.abs(taketori.touchY - e.pageY) > 50) clicked = false;
							}
							taketori.touchX = e.pageX;
							taketori.touchY = e.pageY;
							if (!clicked) {
								taketori.dblClickTimer = setTimeout(function () {
									if (taketori.dblClickTimer) delete taketori.dblClickTimer;
									if (taketori.touchX) delete taketori.touchX;
									if (taketori.touchY) delete taketori.touchY;
								},500);
								return;
							}
						}
						taketori.toggle(taketori.lookUpBlockElements(e.target || e.srcElement));
						taketori.document.stopPropagation(e);
						taketori.document.preventDefault(e);
					});
					this.toggleEventAttached = true;
				}
				if (setOnly) {
					var user_lang = navigator.browserLanguage || navigator.language || navigator.userLanguage;
					user_lang = (user_lang.search(/tw/i) != -1) ? "zh-tw" : 
								(user_lang.search(/(zh|cn)/i) != -1) ? "zh-cn" : 
								(user_lang.search(/(ko|kr)/i) != -1) ? "ko-kr" : "ja-jp";
					alert(TaketoriDblClickAlert[user_lang]);
					return this;
				}
			}
			if (this.ttbDisabled == null) this.ttbDisabled = (this.getCookie('TTB_DISABLED') == 'true') ? true : false;
			if (this.config.togglable) {
				if (!this.config.cookieDomain) this.config.cookieDomain = document.domain;
				if (!this.config.cookiePath) this.config.cookiePath = '/';
				for(var i=0; i<this.targetElements.length; i++) {
					var element = this.targetElements[i];
					if (!element.taketori) element.taketori = {};
					if (!element.taketori.toggleEventAttached) {
						this.document.element(element).addEventListener(event_handler,function(e) {
							e = e || event;
							if (taketori.supportTouch && e.touches) {
								var clicked = (element.taketori.dblClickTimer) ? true : false;
								var is_not_tap = (e.touches.length > 1) ? true : false;
								e = e.touches[0];
								if (clicked) {
									clearTimeout(element.taketori.dblClickTimer);
									delete element.taketori.dblClickTimer;
									if (is_not_tap || Math.abs(element.taketori.touchX - e.pageX) > 50 || Math.abs(element.taketori.touchY - e.pageY) > 50) clicked = false;
								}
								element.taketori.touchX = e.pageX;
								element.taketori.touchY = e.pageY;
								if (!clicked) {
									if (!is_not_tap) element.taketori.dblClickTimer = setTimeout(function () {
										if (element.taketori.dblClickTimer) delete element.taketori.dblClickTimer;
										if (element.taketori.touchX) delete element.taketori.touchX;
										if (element.taketori.touchY) delete element.taketori.touchY;
									},500);
									return;
								}
							}
							taketori.document.stopPropagation(e);
							taketori.document.preventDefault(e);
							if (taketori.ttbDisabled) {
								taketori.deleteCookie('TTB_DISABLED');
								taketori.ttbDisabled = false;
							} else {
								taketori.setCookie('TTB_DISABLED','true');
								taketori.ttbDisabled = true;
							}
							taketori.toVertical(false);
						});
						element.taketori.toggleEventAttached = true;
					}
					this.toggle(element,this.ttbDisabled,true);
				}
			} else if (!this.ttbDisabled) {
				for(var i=0; i<this.targetElements.length; i++) {
					this.make(this.targetElements[i]);
				}
			}
		}
		return this;
	},

	configClone : function (config) {
		if (!config) return {};
		var clone = {};
		var configNames = ['width','height','fontFamily','maxHeight','multiColumnEnabled','gap','contentWidth','contentHeight','onbreak','classNameImported','lang'];
		for(var i=0; i<configNames.length; i++) {
			if (config[configNames[i]] != null) clone[configNames[i]] = config[configNames[i]];
		}
		return clone;
	},

	toggleAll : function () {
		if (this.isLegacy) return this;
		if (!this.config.cookieDomain) this.config.cookieDomain = document.domain;
		if (!this.config.cookiePath) this.config.cookiePath = '/';
		if (this.ttbDisabled) {
			this.deleteCookie('TTB_DISABLED');
			this.ttbDisabled = false;
		} else {
			this.setCookie('TTB_DISABLED','true');
			this.ttbDisabled = true;
		}
		if (this.targetElements) {
			for(var i=0; i<this.targetElements.length; i++) {
				var element = this.targetElements[i];
				this.toggle(element,this.ttbDisabled,true);
			}
		}
		return this;
	},

	clearAll : function () {
		if (this.isLegacy) return this;
		if (!this.config.cookieDomain) this.config.cookieDomain = document.domain;
		if (!this.config.cookiePath) this.config.cookiePath = '/';
		if (!this.ttbDisabled) {
			this.setCookie('TTB_DISABLED','true');
			this.ttbDisabled = true;
		}
		if (this.targetElements) {
			for(var i=0; i<this.targetElements.length; i++) {
				var element = this.targetElements[i];
				this.toggle(element,true,true,true);
			}
		}
		return this;
	},

	appendTarget : function (element) {
		if (this.isVerticalTextElement(element)) return;
		if (!this.targetElements) {
			this.targetElements = new Array(element);
		} else if (!this.isIncludedIn(this.targetElements,element)) {
			this.targetElements[this.targetElements.length] = element;
		}
	},

	removeTarget : function (element) {
		if (this.targetElements) {
			var temp = new Array();
			for(var i=0; i<this.targetElements.length; i++) {
				if (this.targetElements[i] != element) temp[temp.length] = this.targetElements[i];
			}
			this.targetElements = temp;
		}
	},

	makeClipboard : function (element,required) {
		if (!required && !this.process.isBreakable) return;
		if (this.clipboard && this.clipboard.parentNode != element) this.removeClipboard();
		if (!this.clipboard) {
			this.clipboard = document.createElement('div');
			this.clipboard.style.position = 'absolute';
			element.appendChild(this.clipboard);
			var boardSize = (this.windowWidth > this.windowHeight) ? this.windowWidth : this.windowHeight;
			this.clipboard.style.top = '-' + (boardSize + 100) + 'px';
			this.clipboard.style.left = '-' + (boardSize + 100) + 'px';
			this.clipboard.style.width = boardSize + 'px';
			this.clipboard.style.height = boardSize + 'px';
			this.clipboard.style.overflow = 'auto';
		}
	},

	removeClipboard : function () {
		if (this.temporaryContent) this.temporaryContent.parentNode.removeChild(this.temporaryContent);
		if (this.clipboard) this.clipboard.parentNode.removeChild(this.clipboard);
		this.clipboard = null;
		this.temporaryContent = null;
	},

	toPx : function (value,fontSize,isLineHeight,parentSize) {
		return (!value) ? 0 : (isLineHeight) ? ((value.search(/(\d+\.\d+)/) != -1) ? parseInt(RegExp.$1 * fontSize) : parseInt(1.5 * fontSize)) : (value.search(/\d+\.?\d*em/i) != -1) ? parseInt(parseFloat(value) * fontSize) : (value.search(/\d+\.?\d*%/) != -1) ? parseInt(parseFloat(value) * ((parentSize) ? parentSize : fontSize) / 100) : (value.search(/\d/) != -1) ? parseInt(value) : value;
	},

	getFontSize : function (fontSize) {
		if (!isNaN(fontSize) || fontSize.search(/\d+px/) != -1) {
			return parseInt(fontSize);
		} else {
			var bodyFontSize = (this.getStyle(document.body)).fontSize;
			bodyFontSize = (bodyFontSize.search(/\d+\.?\d*(em|%)/) != -1) ? this.toPx(bodyFontSize,16) : parseInt(bodyFontSize);
			fontSize = this.toPx(fontSize,bodyFontSize);
			return (fontSize > 0) ? fontSize : 16; 
		}
	},

	setCurrentConfig : function (element,currentConfig) {
		if (!currentConfig) currentConfig = this.configClone(this.config);
		if (!currentConfig.classNameImported) {
			var className = element.className;
			if (className) {
				var configNames = ['width','height','fontFamily','maxHeight','multiColumnEnabled','gap','contentWidth','contentHeight','lang'];
				for(var i=0; i<configNames.length; i++) {
					var regexp = new RegExp("(^|\\s)taketori-"+configNames[i]+"-([\\w\\.\\-\\%]+)(?![\\w\\-])",'i');
					if (className.search(regexp) != -1) currentConfig[configNames[i]] = RegExp.$2;
				}
			}
			currentConfig.classNameImported = true;
		}
		currentConfig.lang = (currentConfig.lang) ? currentConfig.lang.toLowerCase() : TaketoriDefaultLang;
		currentConfig.ja = null;
		currentConfig.zh = null;
		if (currentConfig.lang == 'ja-jp') {
			currentConfig.ja = true;
		} else if (currentConfig.lang.search(/^zh-/) != -1) {
			currentConfig.zh = true;
		}
		this.init();
		var contentStyle = this.getStyle(element);
		var fontSize = this.getFontSize(contentStyle.fontSize);
		this.process = {
			target : element,
			done : false,
			isMultiColumnEnabled : false,
			noCJK : 0,
			latin : 0,
			width: 0,
			columnCount : 0,
			openTagsHTML : '',
			listStart : [],
			listStyleType : [],
			currentStyle : {},
			openTags : [],
			closeTags : [],
			columnHTML : '',
			content : '',
			config : {}
		};
		if ((typeof currentConfig.multiColumnEnabled == "boolean" && currentConfig.multiColumnEnabled) || (this.isMultiColumnReady && currentConfig.multiColumnEnabled && currentConfig.multiColumnEnabled == 'auto')) {
			this.process.config.multiColumnEnabled = true;
		}
		if (this.isMultiColumnReady && this.process.config.multiColumnEnabled) this.process.isMultiColumnEnabled = true;
		if ((!this.isMultiColumnReady && this.process.config.multiColumnEnabled) || (currentConfig.contentHeight && currentConfig.contentWidth)) this.process.isBreakable = true;
		var w = this.getElementWidth(element,contentStyle);
		if (currentConfig.contentWidth) this.process.config.contentWidth = this.toPx(currentConfig.contentWidth,fontSize,false,w);
		if (currentConfig.contentHeight) this.process.config.contentHeight = this.toPx(currentConfig.contentHeight,fontSize,false,w);
		this.process.config.width = (currentConfig.width) ? this.toPx(currentConfig.width,fontSize,false,w) : (this.process.config.contentWidth != null) ? this.process.config.contentWidth : w;
		this.process.config.gap = (currentConfig.gap != null) ? this.toPx(currentConfig.gap,fontSize) : fontSize * 2;
		if (currentConfig.height == null) {
			if (!currentConfig.maxHeight) currentConfig.maxHeight = '36em';
			var maxHeight = this.toPx(currentConfig.maxHeight,fontSize);
			var windowSize = (this.process.config.contentHeight) ? this.process.config.contentHeight : this.windowHeight;
			if (this.process.config.multiColumnEnabled) {
				var r = Math.ceil(windowSize / (maxHeight + this.process.config.gap));
				this.process.config.height = parseInt((windowSize-40) / r) - this.process.config.gap;
				if (this.process.config.contentHeight) this.process.config.columnCount = r;
			} else {
				windowSize = windowSize - this.process.config.gap - 18;
				this.process.config.height = (windowSize > maxHeight) ? maxHeight : windowSize;
			}
		} else if (this.process.config.contentHeight && this.process.config.multiColumnEnabled) {
			var h = this.toPx(currentConfig.height,fontSize);
			this.process.config.columnCount = Math.ceil(this.process.config.contentHeight / (h+this.process.config.gap));
			this.process.config.height = parseInt((this.process.config.contentHeight + this.process.config.gap) / this.process.config.columnCount) - this.process.config.gap;
		} else if (currentConfig.height && currentConfig.height == 'width') {
			this.process.config.height = this.process.config.width;
		} else if (!this.process.config.multiColumnEnabled) {
			this.process.config.height = this.process.config.contentHeight || this.toPx(currentConfig.height,fontSize);
		} else {
			this.process.config.height = this.toPx(currentConfig.height,fontSize);
		}
		this.process.currentConfig = currentConfig;
		return this;
	},

	getElementWidth : function (element,style) {
		return ((element.offsetWidth) ? element.offsetWidth : element.scrollWidth) - this.getMarginSize([style.paddingLeft,style.paddingRight,style.borderLeftWidth,style.borderRightWidth]);
	},

	getMarginSize : function (v) {
		var n = 0;
		for (var i=0; i<v.length; i++) {
			if (v[i] && !isNaN(parseInt(v[i]))) n += parseInt(v[i]);
		}
		return n;
	},

	getWidth : function (content) {
		var width = 0;
		this.setTaketoriClassName(this.clipboard);
		if (!this.temporaryContent) {
			this.temporaryContent = document.createElement('div');
			this.setTemporaryContentStyle();
			this.clipboard.appendChild(this.temporaryContent);
		}
		this.temporaryContent.innerHTML = content;
		this.process.width = this.getTemporaryContentWidth();
		this.clipboard.className = '';
		return this.process.width;
	},

	setTemporaryContentStyle : function () {
		this.temporaryContent.className = 'taketori-col';
		if (this.isWritingModeReady) {
			this.temporaryContent.style.height = this.process.config.height + 'px';
			this.temporaryContent.style.width = '16px';
		} else {
			this.temporaryContent.style.overflow = 'auto';
			this.temporaryContent.style.width = this.process.config.height + 'px';
		}
	},

	getTemporaryContentWidth : function () {
		return (this.isWritingModeReady) ? (this.temporaryContent.scrollWidth || this.temporaryContent.clientWidth || this.temporaryContent.offsetWidth) : (this.temporaryContent.scrollHeight ||this.temporaryContent.clientHeight ||this.temporaryContent.offsetHeight);
	},

	hasToBreak : function (content) {
		if (!this.temporaryContent) {
			this.temporaryContent = document.createElement('div');
			this.clipboard.appendChild(this.temporaryContent);
		}
		var height = 0;
		var width = 0;
		var hasToBreak = false;
		this.temporaryContent.className = 'taketori-col';
		this.temporaryContent.style.overflow = 'auto';
		this.setTaketoriClassName(this.clipboard);
		if (this.process.config.columnCount && this.process.config.columnCount > 1) {
			var columnTotalHeight = this.process.config.width * this.process.config.columnCount;
			if (this.process.width + 50 < columnTotalHeight) {
				this.setTemporaryContentStyle();
				this.temporaryContent.innerHTML = content;
				this.process.width = this.getTemporaryContentWidth();
			}
			if (this.process.width + 50 >= columnTotalHeight) {
				height = this.getMultiColumnWidth(content);
				hasToBreak = (height > this.process.config.contentHeight) ? true : false;
			}
		} else if (this.isWritingModeReady) {
			this.temporaryContent.style.height = this.process.config.contentHeight + 'px';
			this.temporaryContent.innerHTML = content;
			this.process.width = this.temporaryContent.scrollWidth || this.temporaryContent.clientWidth || this.temporaryContent.offsetWidth;
			hasToBreak = (this.process.width > this.process.config.contentWidth) ? true : false;
		} else {
			this.temporaryContent.style.width = this.process.config.contentHeight + 'px';
			this.temporaryContent.innerHTML = content;
			this.process.width = this.temporaryContent.scrollHeight || this.temporaryContent.clientHeight || this.temporaryContent.offsetHeight;
			hasToBreak = (this.process.width > this.process.config.contentWidth) ? true : false;
		}
		this.clipboard.className = '';
		return hasToBreak;
	},

	getMultiColumnWidth : function (content) {
		if (!this.temporaryContent) {
			this.temporaryContent = document.createElement('div');
			this.clipboard.appendChild(this.temporaryContent);
		}
		var width = 0;
		this.setTaketoriClassName(this.clipboard);
		this.setMultiColumnStyle(this.temporaryContent);
		this.temporaryContent.innerHTML = content;
		width = (this.isWritingModeReady) ? this.getTotalHeight(this.temporaryContent) : this.getTotalWidth(this.temporaryContent);
		this.clipboard.className = '';
		return width;
	},

	setMultiColumnStyle : function (element) {
		element.className = 'taketori-col taketori-multi-column';
		element.style.overflow = 'auto';
		if (this.isWritingModeReady) {
			element.style.width = this.process.config.width + 'px';
			element.style.height = this.process.config.height + 'px';
		} else {
			element.style.width = this.process.config.height + 'px';
			element.style.height = this.process.config.width + 'px';
		}
		element.style.MozColumnWidth = this.process.config.height + 'px';
		element.style.webkitColumnWidth = this.process.config.height + 'px';
		element.style.OColumnWidth = this.process.config.height + 'px';
		element.style.msColumnWidth = this.process.config.height + 'px';
		element.style.columnWidth = this.process.config.height + 'px';
		element.style.MozColumnGap = this.process.config.gap + 'px';
		element.style.webkitColumnGap = this.process.config.gap + 'px';
		element.style.OColumnGap = this.process.config.gap + 'px';
		element.style.msColumnGap = this.process.config.gap + 'px';
		element.style.columnGap = this.process.config.gap + 'px';
		element.style.MozColumnFill = 'auto';
		element.style.webkitColumnFill = 'auto';
		element.style.OColumnFill = 'auto';
		element.style.msColumnFill = 'auto';
		element.style.columnFill = 'auto';
	},

	setMultiColumnWidth : function (element) {
		if (this.isWritingModeReady) {
			var h = this.getTotalHeight(element);
			element.style.height = h + 'px';
			element.style.overflow = 'auto';
			this.fixMargin(element,this.process.config.width,h);
			setTimeout( function () { element.style.height = element.scrollHeight + 'px'; element.style.overflow = 'visible'; }, 120);
		} else {
			var w = this.getTotalWidth(element);
			element.style.width = w + 'px';
			element.style.overflow = 'auto';
			this.fixMargin(element,w,this.process.config.width);
			setTimeout( function () { element.style.width = element.scrollWidth + 'px'; element.style.overflow = 'visible'; }, 120);
		}
	},

	getTotalWidth : function (element) {
		var w = element.scrollWidth || element.clientWidth || element.offsetWidth;
		return Math.ceil(w / (this.process.config.height + this.process.config.gap)) * (this.process.config.height + this.process.config.gap) - this.process.config.gap;
	},

	getTotalHeight : function (element) {
		var h = element.scrollHeight || element.clientHeight || element.offsetHeight;
		return Math.ceil(h / (this.process.config.height + this.process.config.gap)) * (this.process.config.height + this.process.config.gap) - this.process.config.gap;
	},

	setTaketoriClassName : function (element) {
		var className = (this.isWritingModeReady) ? 'taketori-writingmode-ttb' : 'taketori-ttb';
		if (this.rubyDisabled) className += ' taketori-ruby-disabled';
		if (!this.isTextEmphasisReady) className += ' taketori-text-emphasis-disabled';
		className += (
			(this.isWritingModeReady && navigator.userAgent.search(/WebKit/i) != -1 && navigator.userAgent.search(/Windows/i) != -1) ? ' taketori-atsign' : 
			(!this.process.currentConfig.fontFamily) ? ' taketori-serif' : 
			(this.process.currentConfig.fontFamily == 'sans-serif') ? ' taketori-sans-serif' : 
			(this.process.currentConfig.fontFamily == 'cursive') ? ' taketori-cursive' : 
			(this.process.currentConfig.fontFamily == 'kai') ? ' taketori-kai' : 
			' taketori-serif'
		) + '-' + this.process.currentConfig.lang;
		className += ' taketori-lang-' + this.process.currentConfig.lang;
		element.className += ((element.className) ? ' ' : '') + className;
	},

	removeTaketoriClassName : function (element) {
		this.document.element(element).removeClassName('taketori-ttb').removeClassName('taketori-writingmode-ttb').removeClassName('taketori-ruby-disabled').removeClassName('taketori-(lang|serif|sans-serif|cursive|kai)[\\w\\-]*');
	},

	make : function(element,configReady) {
		this.document.element(element).addClassName('taketori-in-progress');
		var taketori = this;
		setTimeout( function () {
			if (!configReady) taketori.setCurrentConfig(element);
			taketori.makeClipboard(element);
			taketori.parse(element,true);
			taketori.complement();
			taketori.removeClipboard();
			taketori.document.element(element).removeClassName('taketori-in-progress');
		},120);
	},

	isSkipClass : function (element) {
		if (this.config.skipClass && element.className) {
			for (var i=0; i < this.config.skipClass.length; i++) {
				var regexp = new RegExp("(^|\\s)"+this.config.skipClass[i]+"(?![\\w\\-])");
				if (this.elements.className.search(regexp) != -1) {
					return true;
				}
			}
		}
		return false;
	},

	isSkipId : function (element) {
		return (this.config.skipId && element.id && this.isIncludedIn(this.config.skipId,thisNode.id)) ? true : false;
	},

	parse : function(thisNode,isRoot) {
		if (thisNode == this.clipboard) return;
		if (thisNode.taketori && thisNode.taketori.ttb) this.toggle(thisNode,true,true);
		var tag = thisNode.nodeName.toLowerCase();
		var openTag = '';
		var closeTag = '';
		var attrText = '';
		var cssText = '';
		var className = '';
		var text = '';
		var isList = false;
		var nodeType = thisNode.nodeType;
		var lineMarginHeight = null;
		var clearLineMarginHeight = false;
		var hasChildNodes = thisNode.hasChildNodes && thisNode.hasChildNodes();
		switch (nodeType) {
			case 1:
				var nodeStyle = this.getStyle(thisNode);
				if (this.process.isBreakable) {
					var fontSize = this.getFontSize(nodeStyle.fontSize);
					this.process.lineHeight = this.toPx(nodeStyle.lineHeight,fontSize,true);
					if (!this.process.lineLength) this.process.lineLength = this.process.config.height;
					if (!isRoot && nodeStyle.display == 'block') {
						var parentLineLength = this.process.lineLength;
						this.process.lineLength -= this.getMarginSize([nodeStyle.marginTop,nodeStyle.marginBottom,nodeStyle.paddingTop,nodeStyle.paddingBottom,nodeStyle.borderTopWidth,nodeStyle.borderBottomWidth]);
						if (this.process.lineLength < fontSize) this.process.lineLength = fontSize;
					}
					this.process.roughFormula = fontSize * this.process.lineHeight / this.process.lineLength;
				}
				this.process.letterSpacing = this.letterSpacing(nodeStyle) || 0;
				if (!isRoot) {
					if (this.isIncludedIn(['img','object','embed','video','audio','textarea'],tag)) {
						this.appendHTML(this.counterClockwiseRotatedOuterHTML(thisNode,nodeStyle));
						return;
					} else if (this.isSkipClass(thisNode) || this.isSkipId(thisNode)) {
						this.appendHTML(this.counterClockwiseRotatedOuterHTML(thisNode,nodeStyle));
						return;
					} else if (this.isIncludedIn(['link','meta','base','script','style','map'],tag)) {
						this.process.columnHTML += this.outerHTML(thisNode);
						return;
					} else if (tag == '!') {
						return;
					} else if (this.isIncludedIn(['br','input','select','option'],tag) || (this.isMSIE && this.isMSIE < 8 && tag == 'table')) {
						this.appendHTML(this.outerHTML(thisNode));
						return;
					}
					var attributes = thisNode.attributes;
					var setLtr = false;
					var setKenten = false;
					for (var i=0;i<attributes.length;i++) {
						var attr = attributes[i];
						var attributeName = attr.nodeName.toLowerCase();
						var attributeValue = (typeof attr.nodeValue == "string" || typeof attr.nodeValue == "number") ? this.escapeHTML(attr.nodeValue) : (attributeName == "style" && thisNode.style.cssText != null) ? thisNode.style.cssText.replace(/[\w\-]+:/g,function(s){return s.toLowerCase()}) : (attributeName == "class" && thisNode.className != null) ? thisNode.className : null;
						if (!attr.specified || !attributeValue || this.isIncludedIn(this.stripAttributes,attributeName)) {
							continue;
						}
						switch (attributeName) {
							case 'style':
								cssText = attributeValue;
								attributeValue = "";
							break;
							case 'class':
								className += ((className) ? ' ' : '') + attributeValue;
								if (attributeValue.search(/(^|\s)ltr(?![\w\-])/) != -1) {
									if (!this.process.ltr) setLtr = true;
									this.process.ltr = true;
								}
								attributeValue = "";
							break;
							case 'dir':
								if (attributeValue.search(/(^|\s)ltr(?![\w\-])/) != -1) {
									if (!this.process.ltr) setLtr = true;
									this.process.ltr = true;
								}
								className += ((className) ? ' ' : '') + attributeValue;
							break;
						}
						if (attributeValue != null && attributeValue != "") attrText += ' '+attributeName+'="'+attributeValue+'"';
					}
					if (setLtr) {
						cssText += this.counterClockwiseRotatedOuterHTML(thisNode,nodeStyle,true)
					} else if (this.isWritingModeReady && !this.isIncludedIn(['table','caption','thead','tbody','tr','td','th','tfoot'],tag)) {
						cssText += this.counterClockwiseRotatedOuterHTML(thisNode,nodeStyle,true)
					}
					if (!this.process.ltr && (!this.isMSIE || this.isMSIE > 7)) {
						if (tag == 'li' && this.process.listStyleType && this.process.listStyleType[this.process.listStyleType.length-1] != '') {
							attrText += ' data-marker="'+this.getListMarkerText()+'"';
						}
						if (tag == 'ul' || tag == 'ol') {
							isList = true;
							this.process.listStart[this.process.listStart.length] = 0;
							var listStyleType = nodeStyle.listStyleType.toLowerCase();
							if (this.isIncludedIn(['none','disc','circle','square'],listStyleType)) {
								this.process.listStyleType[this.process.listStyleType.length] = '';
							} else {
								this.process.listStyleType[this.process.listStyleType.length] = listStyleType;
								className += ((className) ? ' ' : '') + 'cjk';
							}
						}
						if (this.isWritingModeReady) {
							if (tag == 'strong') {
								if (!this.isTextEmphasisReady) {
									if (!this.process.kenten) setKenten = true;
									this.process.kenten = true;
								}
								className += ((className) ? ' ' : '') + 'bo-ten';
							}
						}
						if (!this.isWritingModeReady && nodeStyle.fontStyle && nodeStyle.fontStyle.toLowerCase() == 'italic' && hasChildNodes) {
							className += ((className) ? ' ' : '') + 'italic';
						}
						if (nodeStyle.textDecoration && hasChildNodes) {
							var textDecoration = nodeStyle.textDecoration.toLowerCase();
							if (textDecoration != 'none') {
								if (textDecoration.search(/underline/i) != -1 && textDecoration.search(/overline/i) != -1) {
									className += ((className) ? ' ' : '') + 'bothline';
									lineMarginHeight = this.lineMarginHeight(nodeStyle);
								} else if (textDecoration.search(/underline/i) != -1) {
									className += ((className) ? ' ' : '') + 'underline';
									lineMarginHeight = this.lineMarginHeight(nodeStyle);
								} else if (textDecoration.search(/overline/i) != -1) {
									className += ((className) ? ' ' : '') + 'overline';
									lineMarginHeight = this.lineMarginHeight(nodeStyle);
								}
								if (!this.isWritingModeReady && lineMarginHeight) {
									if (!this.process.lineMarginHeight) clearLineMarginHeight = true;
									this.process.lineMarginHeight = lineMarginHeight;
								}
							}
						}
					}
					if (className) attrText += ' class="'+className+'"';
					if (cssText) attrText += ' style="'+cssText+'"';
					if (!isRoot) openTag += '<'+tag+attrText;
				}
				if (!hasChildNodes) {
					if (!isRoot) {
						openTag += '></'+tag+'>';
						this.process.columnHTML += openTag;
					}
				} else {
					if (!isRoot) {
						this.process.columnHTML += openTag + '>';
						if (isList) openTag += ' start=""';
						openTag += '>';
						closeTag = '</'+tag+'>';
						this.process.openTags[this.process.openTags.length] = openTag;
						this.process.closeTags[this.process.closeTags.length] = tag;
					}
					for (var i=0; i<thisNode.childNodes.length; i++) {
						this.parse(thisNode.childNodes[i]);
					}
					if (!isRoot) {
						this.process.columnHTML += closeTag;
						this.process.openTags.pop();
						this.process.closeTags.pop();
						if (setLtr) this.process.ltr = null;
						if (setKenten) this.process.kenten = null;
						if (clearLineMarginHeight) this.process.lineMarginHeight = null;
						if (isList) {
							this.process.listStart.pop();
							this.process.listStyleType.pop();
						}
					}
				}
				if (parentLineLength) this.process.lineLength = parentLineLength;
				break;
			case 3:
				text = this.escapeHTML(thisNode.nodeValue);
				text = text.replace(/\uFF0F\uFF3C/g,"\u3033\u3035").replace(/\uFF3C\uFF0F/g,"\u3033\u3035").replace(/\uFF0F\u0022\uFF3C/g,"\u3034\u3035").replace(/\uFF3C\u0022\uFF0F/g,"\u3034\u3035");//kunojiten
				text = text.replace(/\uFF61/g,"\u3002").replace(/\uFF62/g,"\u300C").replace(/\uFF63/g,"\u300D").replace(/\uFF64/g,"\u3001");//hankaku
				text = text.replace(/(^|[^\u0000-\u10FF])\s*([0-9\.\,\+\-]{3,}\s*[A-Za-z%]{0,2}|[0-9]\s*[A-Za-z%]{0,2}|[A-Z]+|[a-zA-Z]{1,2})(?=\s*[^\u0000-\u10FF]|$)/g,function (a,p,w) {return p + w.replace(/./g, function (c) {return String.fromCharCode(c.charCodeAt(0) + 65248)})});//hankaku->zenkaku
				text = text.replace(/([^\u0000-\u10FF]\s*[0-9]{2}|^\s*[0-9]{2})\s*([a-zA-Z]{1,2})(?=\s*[^\u0000-\u10FF]|$)/g,function (a,p,w) {return p + w.replace(/./g, function (c) {return String.fromCharCode(c.charCodeAt(0) + 65248)})});//hankaku->zenkaku
				var taketori = this;
				var count = 0;
				text.replace(/&#?\w+;|\s+|./g,function (w) {

					//CJK
					if (w.search(/^[\u1100-\u11FF\u2030-\u217F\u2460-\u24FF\u2600-\u261B\u2620-\u277F\u2E80-\u2FDF\u2FF0-\u4DBF\u4E00-\u9FFF\uA960-\uA97F\uAC00-\uD7AF\uD7B0-\uD7FF\uF900-\uFAFF\uFE30-\uFE4F\uFF00\uFF01\uFF03-\uFF06\uFF08-\uFF0C\uFF0E-\uFF1B\uFF1F-\uFF3D\uFF40-\uFF5B\uFF5D-\uFFEF]$/) != -1) {
						taketori.setCJK();
						if (taketori.process.currentConfig.zh && taketori.isWritingModeReady && !taketori.process.ltr && (w == "\uFF1A" || w == "\uFF1B")) {
							w = '<span class="tcy"' + ((taketori.process.letterSpacing) ? 'style="margin-' + ((taketori.isWritingModeReady) ? 'bottom:' : 'right:') + taketori.process.letterSpacing + 'px;"' : '') + '>' + w + '</span>';
						} else if ((!taketori.isWritingModeReady || taketori.process.kenten) && !taketori.process.ltr) {
							w = taketori.kinsokuShori('<span class="' + taketori.getCJKClassName(w) + '" style="' + ((taketori.process.lineMarginHeight) ? 'margin-top:' + taketori.process.lineMarginHeight + 'px;margin-bottom:' + taketori.process.lineMarginHeight + 'px;"' : '') + ((taketori.process.letterSpacing) ? 'margin-right:' + taketori.process.letterSpacing + 'px;' : '') + '">' + w + '</span>');
						}
						count++;

					//space
					} else if (w.search(/^\s+$/) != -1) {
						if (count > 0) count+=0.5;
						
					//NoCJK
					} else {
						if (w.search(/^[0-9a-zA-Z]$/) != -1) {
							count+=0.5;
							taketori.process.latin++;
						} else {
							count++;
							taketori.process.latin = 0;
						}
						if (taketori.process.noCJK == 0) w = '<span class="nocjk notcy">' + w;
						if (!this.isTextEmphasisReady) taketori.process.noCJK++;
					}
					if (!taketori.process.isBreakable || (count > 1 && taketori.isNoBreak(taketori.process.width + (count * taketori.process.roughFormula) + taketori.process.lineHeight))) {
						taketori.process.columnHTML += w;
					} else {
						count = 1;
						taketori.appendHTML(w);
					}
				});
				this.setCJK();
				break;
		}
	},

	isNoBreak : function (h) {
		if (this.isWritingModeReady || !this.process.isMultiColumnEnabled) {
			return (h < this.process.config.width) ? true : false;
		} else if (this.process.config.contentHeight && this.process.config.contentWidth) {
			if (this.process.config.columnCount && this.process.config.columnCount > 1) {
				return (h + 50 < this.process.config.width * this.process.config.columnCount) ? true : false;
			} else {
				return (h < this.process.config.contentHeight) ? true : false;
			}
		} else {
			return true;
		}
	},

	setCJK : function () {
		if (this.process.noCJK > 0) {
			this.process.columnHTML = ((this.process.noCJK == this.process.latin && this.process.latin <= 2) ? this.process.columnHTML.replace('<span class="nocjk notcy">','<span class="tcy"' + ((this.process.letterSpacing) ? 'style="margin-' + ((this.isWritingModeReady) ? 'bottom:' : 'right:') + this.process.letterSpacing + 'px;"' : '') + '>') : this.process.columnHTML.replace('<span class="nocjk notcy">','<span class="nocjk">')) + '</span>';
		}
		this.process.latin = this.process.noCJK = 0;
	},

	lineMarginHeight : function (nodeStyle) {
		var lineHeight = nodeStyle.lineHeight;
		var fontSize = this.getFontSize(nodeStyle.fontSize);
		if (lineHeight.search(/\d+px/i) != -1) {
			return parseInt((parseInt(lineHeight) - fontSize) / 2);
		} else if (lineHeight.search(/\d+%/) != -1) {
			return parseInt(((parseInt(lineHeight) / 100) * fontSize - fontSize) / 2);
		} else {
			return parseInt((parseFloat(lineHeight) * fontSize - fontSize) / 2);
		}
	},

	letterSpacing : function (nodeStyle) {
		var letterSpacing = nodeStyle.letterSpacing;
		if (letterSpacing.search(/normal/i) != -1) {
			return 0;
		} else if (letterSpacing.search(/\d+px/i) != -1) {
			return parseInt(letterSpacing);
		} else {
			var fontSize = this.getFontSize(nodeStyle.fontSize);
			if (letterSpacing.search(/\d+%/) != -1) {
				return parseInt((parseInt(letterSpacing) / 100) * fontSize);
			} else {
				return parseInt(parseFloat(letterSpacing) * fontSize);
			}
		}
	},

	getListMarkerText : function () {
		var n = ++this.process.listStart[this.process.listStart.length-1];
		var listType = this.process.listStyleType[this.process.listStyleType.length-1];
		var list;
		if (listType == "A" || listType == "\u30A2" || listType == "upper-latin" || listType == "katakana") {
			list = ["\u30A2","\u30A2","\u30A4","\u30A6","\u30A8","\u30AA","\u30AB","\u30AD","\u30AF","\u30B1","\u30B3","\u30B5","\u30B7","\u30B9","\u30BB","\u30BD","\u30BF","\u30C1","\u30C4","\u30C6","\u30C8","\u30CA","\u30CB","\u30CC","\u30CD","\u30CE","\u30CF","\u30D2","\u30D5","\u30D8","\u30DB","\u30DE","\u30DF","\u30E0","\u30E1","\u30E2","\u30E4","\u30E6","\u30E8","\u30E9","\u30EA","\u30EB","\u30EC","\u30ED","\u30EF","\u30F0","\u30F1","\u30F2","\u30F3"];
			return "(" + list[n] + ")";
		} else if (listType == "a" || listType == "\u3042" || listType == "lower-latin" || listType == "hiragana") {
			list = ["\u3042","\u3042","\u3044","\u3046","\u3048","\u304A","\u304B","\u304D","\u304F","\u3051","\u3053","\u3055","\u3057","\u3059","\u305B","\u305D","\u305F","\u3061","\u3064","\u3066","\u3068","\u306A","\u306B","\u306C","\u306D","\u306E","\u306F","\u3072","\u3075","\u3078","\u307B","\u307E","\u307F","\u3080","\u3081","\u3082","\u3084","\u3086","\u3088","\u3089","\u308A","\u308B","\u308C","\u308D","\u308F","\u3090","\u3091","\u3092","\u3093"];
			return "(" + list[n] + ")";
		} else if (listType == "\u30A4" || listType == "katakana-iroha") {
			list = ["\u30A4","\u30A4","\u30ED","\u30CF","\u30CB","\u30DB","\u30D8","\u30C8","\u30C1","\u30EA","\u30CC","\u30EB","\u30F2","\u30EF","\u30AB","\u30E8","\u30BF","\u30EC","\u30BD","\u30C4","\u30CD","\u30CA","\u30E9","\u30E0","\u30A6","\u30F0","\u30CE","\u30AA","\u30AF","\u30E4","\u30DE","\u30B1","\u30D5","\u30B3","\u30A8","\u30C6","\u30A2","\u30B5","\u30AD","\u30E6","\u30E1","\u30DF","\u30B7","\u30F1","\u30D2","\u30E2","\u30BB","\u30B9"];
			return "(" + list[n] + ")";
		} else if (listType == "\u3044" || listType == "hiragana-iroha") {
			list = ["\u3044","\u3044","\u308D","\u306F","\u306B","\u307B","\u3078","\u3068","\u3061","\u308A","\u306C","\u308B","\u3092","\u308F","\u304B","\u3088","\u305F","\u308C","\u305D","\u3064","\u306D","\u306A","\u3089","\u3080","\u3046","\u3090","\u306E","\u304A","\u304F","\u3084","\u307E","\u3051","\u3075","\u3053","\u3048","\u3066","\u3042","\u3055","\u304D","\u3086","\u3081","\u307F","\u3057","\u3091","\u3072","\u3082","\u305B","\u3059"];
			return "(" + list[n] + ")";
		} else if (listType == "I" || listType == "upper-roman") {
			list = ["I","I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX"]; 
			return "(" + list[n] + ")";
		} else if (listType == "i" || listType == "lower-roman") {
			list = ["i","i","ii","iii","iv","v","vi","vii","viii","ix","x","xi","xii","xiii","xiv","xv","xvi","xvii","xviii","xix","xx"]; 
			return "(" + list[n] + ")";
		} else if (listType == "1" || listType == "decimal" || listType == "\u4E00" || listType == "cjk-ideographic" || listType == "\u58F1") {
			if (n <= 10) {
				list = (listType == "\u58F1") ? ["\u58F1","\u58F1","\u5F10","\u53C2","\u56DB","\u4F0D","\u516D","\u4E03","\u516B","\u4E5D","\u62FE"] : ["\u4E00","\u4E00","\u4E8C","\u4E09","\u56DB","\u4E94","\u516D","\u4E03","\u516B","\u4E5D","\u5341"]; 
				return '(' + list[n] + ')';
			} else {
				return '(' + n + ')';
			}
		} else {
			return "";
		}
	},

	appendHTML : function (w,isTerminated) {
		var html = '';
		if (w == null) w = '';
		if ((isTerminated || this.process.isBreakable) && this.process.columnHTML != '') {
			if ((this.isWritingModeReady && !this.isMultiColumnReady) || !this.process.isMultiColumnEnabled) {
				if (isTerminated) {
					html = this.columnComplement();
				} else {
					html = this.columnComplement(this.process.columnHTML + w);
				}
				if (isTerminated || this.process.config.width < this.getWidth(html)) {
					if (!isTerminated) html = this.columnComplement();
					this.process.content += '<div class="taketori-col" style="' + this.rotatedBlockAdditionalCSSText(this.process.config.height,this.process.config.width,this.process.config.gap,this.process.config.multiColumnEnabled) + '">' + html + '</div>';
					this.process.columnCount++;
					if (isTerminated || (this.process.config.columnCount && this.process.config.columnCount == this.process.columnCount)) this.containerBreak();
				}
			} else if (this.process.config.contentHeight && this.process.config.contentWidth) {
				if (isTerminated) {
					html = this.columnComplement();
				} else {
					html = this.columnComplement(this.process.columnHTML + w);
				}
				if (isTerminated || this.hasToBreak(html)) {
					if (!isTerminated) html = this.columnComplement();
					if (this.process.config.columnCount && this.process.config.columnCount > 1) {
						this.process.content += '<div class="taketori-col taketori-multi-column" style="-moz-column-width:' + this.process.config.height + 'px;-webkit-column-width:' + this.process.config.height + 'px;column-width:' + this.process.config.height + 'px;-moz-column-count:' + this.process.config.columnCount + ';-webkit-column-count:' + this.process.config.columnCount + ';column-count:' + this.process.config.columnCount + ';-moz-column-gap:' + this.process.config.gap + 'px;-webkit-column-gap:' + this.process.config.gap + 'px;column-gap:' + this.process.config.gap + 'px;' + this.rotatedBlockAdditionalCSSText(this.process.config.contentHeight,this.process.config.contentWidth,0,true) + '">' + html + '</div>';
					} else {
						this.process.content += '<div class="taketori-col" style="' + this.rotatedBlockAdditionalCSSText(this.process.config.contentHeight,this.process.config.contentWidth) + '">' + html + '</div>';
					}
					this.containerBreak();
				}
			} else if (isTerminated) {
				this.process.content = document.createElement('div');
				this.setMultiColumnStyle(this.process.content)
				this.process.content.innerHTML = this.columnComplement()
				this.containerBreak();
				this.setMultiColumnWidth(this.process.target.firstChild)
			}
		}
		this.process.columnHTML += w;
	},

	columnComplement : function (column) {
		var isTrueColumn = false;
		var html = '';
		if (column == null) {
			html = this.process.columnHTML + ((this.process.noCJK > 1) ? '</span>' : '');;
			isTrueColumn = true;
		} else {
			html = column + ((this.process.noCJK) ? '</span>' : '');
		}
		if (isTrueColumn) {
			this.process.columnHTML = '';
			this.process.width = 0;
			if (this.process.noCJK) this.process.noCJK = 1;
			if (this.process.latin) this.process.latin = 1;
		}
		var closeTags = '';
		var openTags = this.process.openTagsHTML;
		var isNewItem = true;
		for (var i=this.process.closeTags.length-1; i>=0; i--) {
			var closeTag = this.process.closeTags[i];
			var regexp = new RegExp("<"+closeTag+"(?!\\w)[^>]*>\\s*$");
			if (html.search(regexp) == -1) {
				closeTags += '</'+closeTag+'>';
			} else if (closeTags == '') {
				html = html.replace(regexp,'');
				if (closeTag == 'li') isNewItem = false;
			}
		}
		if (isTrueColumn) {
			this.process.openTagsHTML = this.process.openTags.join('');
			if (isNewItem) this.process.openTagsHTML = this.process.openTagsHTML.replace(/ data-marker=".*?"/g,'');
			for (var i=0; i<this.process.listStart.length; i++) {
				this.process.openTagsHTML = this.process.openTagsHTML.replace(/ start=""/,'start="'+this.process.listStart[i]+'"');
			}
		}
		return openTags + html + closeTags;
	},

	getCJKClassName : function (w) {
		var className;
		if (w == "\u30FC" || w == "\u301C" || w == "\uFF5E" || w == "\u2026") {
			this.process.kinsoku = true;
			return "cjk cho-on";
		} else if (w == "\u3001" || w == "\uFF0C") {
			this.process.kinsoku = true;
			return (this.process.currentConfig.ja) ? "cjk tou-ten" : "cjk";
		} else if (w == "\u3002" || w == "\uFF0E") {
			this.process.kinsoku = true;
			return (this.process.currentConfig.ja) ? "cjk ku-ten" : "cjk";
		} else if (w.search(/^[\u3041\u3043\u3045\u3047\u3049\u3083\u3085\u3087\u3063\u30A1\u30A3\u30A5\u30A7\u30A9\u30E3\u30E5\u30E7\u30C3]$/) != -1) {
			this.process.kinsoku = true;
			return 'cjk kogaki';
		} else if (w.search(/^[\u3009\u300B\u300D\u300F\u3011\u3015\u3017\u3019\uFF09\uFF5D\uFF60]$/) != -1) {
			this.process.kinsoku = true;
			return 'kakko';
		} else if (w.search(/^[\u3008\u300A\u300C\u300E\u3010\u3014\u3016\u3018\uFF08\uFF5B\uFF5F]$/) != -1) {
			return 'kakko';
		} else if (w.search(/^[\uFF01\uFF1A\uFF1B\uFF1F]$/) != -1) { // use "search" for IE5.5.
			this.process.kinsoku = true;
			return 'cjk';
		} else {
			return 'cjk';
		}
	},

	kinsokuShori : function (w) {
		if (this.process.kinsoku) {
			this.process.columnHTML = this.process.columnHTML.replace(/(<span(?!\w)[^>]*class="cjk"[^>]*>.<\/span>)$/,'');
			if (RegExp.$1) w = '<span class="kinsoku">' + RegExp.$1 + w + '</span>';
			this.process.kinsoku = null;
		}
		return w;
	},

	complement : function () {
		this.appendHTML('',true);
	},

	containerBreak : function () {
		if (typeof this.process.config.onbreak == 'function') {
			this.process.config.onbreak(this);
		} else {
			this._containerBreak();
		}
	},

	_containerBreak : function () {
		if (this.process.target) {
			this.removeClipboard();
			if (!this.process.target.taketori) this.process.target.taketori = {};
			if (!this.process.config.contentWidth || !this.process.config.contentHeight || !this.process.done) {
				var alt = this.process.target.innerHTML;
				this.setTaketoriClassName(this.process.target);
				if (typeof this.process.content == 'object') {
					this.process.target.innerHTML = '';
					this.process.target.appendChild(this.process.content);
				} else {
					this.process.target.innerHTML = this.process.content;
					var columnNode = this.process.target.firstChild;
					var taketori = this;
					var h = this.process.config.width;
					var g = this.process.config.gap;
					setTimeout(function () {
						if (taketori.isWritingModeReady) {
							if (columnNode.clientHeight < columnNode.scrollHeight) {
								columnNode.style.height = columnNode.scrollHeight + 'px';
							}
						} else {
							if (columnNode.clientWidth < columnNode.scrollWidth) {
								var w = columnNode.scrollWidth;
								columnNode.style.width = w + 'px';
								taketori.fixMargin(columnNode,w,h,g,taketori.process.config.multiColumnEnabled);
							}
						}
					},600);
				}
				if (this.process.config.contentWidth && this.process.config.contentHeight) this.process.done = true;
				this.process.content = '';
				this.process.target.taketori.ttb = true;
				this.process.target.taketori.alt = alt;
				this.process.target.taketori.config = this.configClone(this.process.currentConfig);
				this.process.target.taketori.clientWidth = this.process.target.clientWidth;
				this.process.target.taketori.windowHeight = this.windowHeight;
			}
			this.makeClipboard(this.process.target);
		}
	},

	fixMargin : function (rotatedNode,w,h,g,o) {
		if (!g) g = 0;
		if (this.isWritingModeReady) {
			if (g) {
				if (!o) {
					rotatedNode.style.paddingBottom = g + 'px';
				} else {
					rotatedNode.style.marginBottom = g + 'px';
				}
			}
		} else {
			rotatedNode.style.marginTop = '-' + h + 'px';
			rotatedNode.style.marginRight = (h - w - g) + 'px';
			rotatedNode.style.marginBottom = (w + g) + 'px';
			rotatedNode.style.marginLeft = 0;
			if (!o && g) rotatedNode.style.paddingRight = g + 'px';
		}
	},

	refresh : function () {
		if (this.isLegacy) return this;
		this.init();
		for(var i=0; i<this.targetElements.length; i++) {
			var element = this.targetElements[i];
			if (element && element.taketori && element.taketori.alt && element.taketori.ttb && (element.clientWidth != element.taketori.clientWidth || element.taketori.windowHeight != this.windowHeight)) {
				this.setCurrentConfig(element,element.taketori.config);
				if (this.process.isBreakable) {
					this.toggle(element,true,true);
					this.make(element,true);
				} else {
					var columnNode = element.firstChild;
					var w;
					if (columnNode) {
						if (this.process.isMultiColumnEnabled) columnNode.style.overflow = 'auto';
						if (element.clientWidth != element.taketori.clientWidth) {
							element.taketori.clientWidth = element.clientWidth;
							if (this.isWritingModeReady) {
								columnNode.style.width = this.process.config.width + 'px';
							} else {
								columnNode.style.height = this.process.config.width + 'px';
							}
						}
						if (element.taketori.windowHeight != this.windowHeight) {
							if (this.process.isMultiColumnEnabled) {
								w = this.process.config.height;
								columnNode.style.MozColumnWidth = w + 'px';
								columnNode.style.webkitColumnWidth = w + 'px';
								columnNode.style.OColumnWidth = w + 'px';
								columnNode.style.msColumnWidth = w + 'px';
								columnNode.style.columnWidth = w + 'px';
							} else {
								if (this.isWritingModeReady) {
									columnNode.style.height = this.process.config.height + 'px';
									if (columnNode.clientHeight < columnNode.scrollHeight) {
										columnNode.style.height = columnNode.scrollHeight + 'px';
									}
								} else {
									w = this.process.config.height;
									columnNode.style.width = w + 'px';
									if (columnNode.clientWidth < columnNode.scrollWidth) {
										w = columnNode.scrollWidth;
										columnNode.style.width = w + 'px';
									}
								}
							}
							element.taketori.windowHeight = this.windowHeight;
						}
						if (this.process.isMultiColumnEnabled) {
							if (this.isWritingModeReady) {
								columnNode.style.height = this.process.config.height + 'px';
							} else {
								columnNode.style.width = this.process.config.height + 'px';
							}
							this.setMultiColumnWidth(columnNode);
						} else if (!this.isWritingModeReady) {
							this.fixMargin(columnNode,w,this.process.config.width,this.process.config.gap,this.process.config.multiColumnEnabled);
						}
					}
				}
			}
		}
	},

	toggle : function (element,ttbDisabled,keepTargets,cacheDisabled) {
		if (this.isLegacy || this.isVerticalTextElement(element) || (ttbDisabled != null && ((ttbDisabled && (!element.taketori || !element.taketori.ttb)) || ((!ttbDisabled && element.taketori && element.taketori.ttb))))) return this;
		this.init();
		if (element.taketori && element.taketori.alt && (element.taketori.ttb || (element.clientWidth == element.taketori.clientWidth && element.taketori.windowHeight == this.windowHeight))) {
			var alt = element.innerHTML;
			element.innerHTML = element.taketori.alt;
			if (element.taketori.ttb) {
				this.removeTaketoriClassName(element);
			} else {
				this.setTaketoriClassName(element);
				keepTargets = true;
			}
			if (keepTargets) {
				element.taketori.alt = ((cacheDisabled || this.config.cacheDisabled) && element.taketori.ttb) ? null : alt;
				element.taketori.ttb = (element.taketori.ttb) ? false : true;
				element.taketori.clientWidth = element.clientWidth;
				element.taketori.windowHeight = this.windowHeight;
			} else {
				this.removeTarget(element);
				element.taketori = null;
			}
		} else {
			this.appendTarget(element);
			this.make(element,false);
		}
		return this;
	},

	clear : function (element) {
		this.toggle(element,true,true,true);
	}
}
