localStorage.setItem('bannerLastClosed', new Date());
localStorage.setItem('xpromo-consolidation', new Date());

var posts = [].filter.call(document.getElementsByTagName('article'), el => (
   (el.getElementsByTagName('a')[0].getAttribute('rel') != null && el.getElementsByTagName('a')[0].getAttribute('rel').indexOf('sponsored') >= 0)));

while(posts.length > 0) { posts.pop().style.display = "none"; }

var qcleanObserver = new window.MutationObserver(function(mutation, observer){
var posts = [].filter.call(document.getElementsByTagName('article'), el => (
   (el.getElementsByTagName('a')[0].getAttribute('rel') != null && el.getElementsByTagName('a')[0].getAttribute('rel').indexOf('sponsored') >= 0)));

while(posts.length > 0) { posts.pop().style.display = "none"; }
});

qcleanObserver.observe(document, { subtree: true, childList: true });
