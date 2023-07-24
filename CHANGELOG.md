### 10.5.0
* Reader mode font configuration (not stable yet)
* Support reading subtitle format srt file.
* Support Android monochrome App icon

### 10.4.0
## Feature
* separate reader mode font setting from normal browsing (beta)
* translate web content only when content is visible in "translate by paragraph" mode
## Fix
* crash in inputting url address
* import bookmarks failure
* page info divide by zero
* bookmark manager crash
* gesture setting is gone

### 10.3.0
* Support paragraph-by-paragraph translation
* Add popular translation method to toolbar actions

### 10.2.0
* Add Translate feature in text selection dialog.
* Fix when long click on text field, the paste function is not available.

|Figure 1| Figure 2|
|--|--|
|![image](https://user-images.githubusercontent.com/4084738/232300452-46b1a867-30b8-4006-9491-66fd180865de.png)|![image](https://user-images.githubusercontent.com/4084738/232300518-9a051d63-01c0-4e59-9a16-6c954851a285.png)|

### 10.1.0
## Feature
1. Integrate ChatGPT feature (If API key is setup in Settings > ChatGPT integration, after text selection, it will show a `ChatGPT` icon)
2. New menu style after text selection (can be turned off in Settings > UI)
3.  If Pocket app is installed, its UI will be used.
4. Add Indonesian language support (thanks to amsitlab)
5. New UI for touch area dialog

|Feature 1, 2| Feature 1|
|-|-|
| ![image](https://user-images.githubusercontent.com/4084738/232273619-70c61d22-b1a2-4dcf-ad0e-bd5969d161a5.png) | ![image](https://user-images.githubusercontent.com/4084738/232273671-9bc73340-35b2-4799-880c-66cb9b2cd772.png) |
|Feature 5|Feature 1|
|![image](https://user-images.githubusercontent.com/4084738/232273758-c88e4f65-6f2e-4da8-b733-4c2e4978f651.png)|![image](https://user-images.githubusercontent.com/4084738/232274383-2c59bffd-89ab-40a2-ba91-2b5209451bc5.png)|

## Fix 
* Enter/back fullscreen mode won't cause screen too big.
* Remove beta feature: image adjustment

### 10.0.0
* Feature: Add Pocket support. Now you can add web url to pocket from menu.
* UI: fold menu items to make it not so frightening. Users can expand it if they use some functions more often.
* Fix: now the whitelist for adblock domains, and Javascript are working.
* Fix: update page info when scrolling web content.

![image](https://user-images.githubusercontent.com/4084738/231217539-0669eb49-3e29-4968-a15a-bafb17cf7514.png)

![image](https://user-images.githubusercontent.com/4084738/231217595-3f984520-c29d-4d01-af0c-f7984af2bbd9.png)

![image](https://user-images.githubusercontent.com/4084738/231217653-ca65f447-f415-44b2-a62b-6c58863dbfd2.png)

### 9.23.0
* click on tab to scroll to top
* click on tab again to reload

### 9.22.0
* Enhance user agent setting. You could use to configure proper user agent strings so that it can pass cloudflare captcha check.
For example, input following string: `Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.50 Mobile Safari/537.36`
* Add page info feature to toolbar (you can add it in toolbar configuration)

### 9.21.0
* Add client certificate authentication support
* Fine tune black text feature
* Modify manual link
* Add an option to show "no more history" and do not close current tab
* Fix: pagedown overscroll issue
* Fix: if url parameters are without value, it should be kept as is.

### 9.20.0
* Support maestro to run automation scripts.
* Add an option to set all text color in black.
* Add image adjustment in quick setting
* UI: do not scroll screen when using multi-touch gesture.
* UI: fix accidentally trigger input address bar.
* Support bookmark sync feature with Google Drive through system file picker.
* Support saving web content to .mht file. It keeps the original web layout.
* Refactor menu handler, toolbar handler, gesture handler.

### 9.19.0
* Backup now works with file save location feature.
* Manual is available now in Settings > About.
* Fix: new created web size is wrong.
* Fix: save tabs feature now works with lazy loading tabs.
* Enhance font customization dialog.
* Hide tab bar is it's in epub reading mode.

### 9.18.0
* All files are in kotlin language.
* Use adblock hosts from github link.
* Add settings for adblock: auto-update, trigger update, modify adblock content url.

### 9.17.0
* Support playing video in fullscreen automatically when start playing.
* Support picture-in-picture mode if video playing is in fullscreen, and leave EinkBro
* Refactor UI for whitelists
* Fix search engine setting error

### 9.16.0
* re-write whole setting UI with jetpack compose

### 9.15.0
* optimize link context menu position

### 9.14.0
* Add text-to-speech feature (**Read Content** in menu, Figure 1)
* Add text-to-speech action in toolbar configuration
* Long click text-to-speech button on toolbar to adjust voice speed or open voice setting. (Figure 2)

### Figure 1
<img src=https://user-images.githubusercontent.com/4084738/202916340-40d9fea9-9f79-4a78-a646-f1b037e8b10b.png width=400>

### Figure 2
<img src=https://user-images.githubusercontent.com/4084738/202916456-ad25af6d-c673-4dbf-93fa-ac69a6731dad.png width=200>

### 9.13.0
* Enhance context menu UI when long pressing a link

<img src=https://user-images.githubusercontent.com/4084738/202330956-4e0805b2-dbe0-420b-9d94-0c978fa58f2a.png width=400>

### 9.12.0
* UI: show tab count in floating button
* Support devices that does not have microphone or gps

### 9.11.0
* Fix: after opening new tab, the javascript on current tab may not work
* Feature: add a new mode for Floating button, so that it's possible to customize its position
* Feature: support opening Menu as gesture action.

### 9.10.0
* Add + button on Tab bar
* Support editing homepage url in Settings
* Support new tab behavior: focus on url, show recent bookmarks, show homepage
* Fix dark mode issues on some devices

### 9.9.0
* Add a pen icon in front of web title, so that new users know where to touch for editing url.
* Remove font configurstion button from default toolbar and put "add tab" icon instead.
* fix: go to bookmark root folder after any bookmark item is clicked.

![image](https://user-images.githubusercontent.com/4084738/194693275-46a51cb6-63f0-43ef-ba9c-c9f76f8e4fe2.png)

### 9.8.0
* support microphone permission request, when web content need to record audio
* support basic username / password authentication
* add a button to duplicate current tab
* fix download crash issue

### 9.7.0
* support stripping url tracking parameters
* fixed tab bar support. can be turned on in Settings > Behavior/UI
* enhance menu icon ordering

### 9.6.0
* Fix title width
* Enhance text input: make sure text area can be displayed.
https://stackoverflow.com/a/21860837/1265915

### 9.5.0
* enable website zooming
* fix toolbar title color in dark mode
* use drag handle in toolbar configuration
* enhance facebook browsing
* enhance twitter browsing
* add refresh action in gesture configuration
* now it's possible to turn on WebView debug in settings > start controls

### 9.4.0
* combine font size and font type dialog
* add bookmark adding hint when it's empty
* save image when long pressed on images

<img src=https://user-images.githubusercontent.com/4084738/179558766-d06ad7aa-7651-4c8f-8c2f-b0cf39d719d2.png width=300 />

### 9.3.0
* UI: rewrite touch area config dialog
* UI: rewrite search bar
* UI: rewrite menu dialog
* UI: rewrite input url bar
* Setting: add an option to disable auto fill form
* Setting: add an option to auto trim input url

### 9.2.0
* fix dialog shadow
* add SSL exception handling dialog
* enhance incognito mode icon effect
* refactor setting UI, touch area dialog to compose

### 9.1.0
## Apk size for this version is much larger, due to adopting Jetpack Compose

It'll be easier to add new features in the future; however, the size increase sucks.

## Changes
* a lot of code refactoring to adopt Jetpack Compose for existing dialogs
* add copy link in context menu
* add an option to disable loading background tab in Settings / Behavior
* fix: keyboard shortcut for font size change
* now Reader Mode should be available for more websites!
* toolbar is scrollable again!   

### 9.0.0
#### Feature
* Add javascript toggle in Quick Toggle setting screen
* Enhance Quick Toggle setting screen
* Add favicon to Bookmark list
* Enhance Tab Preview bottom bar
* Enhance Bookmark list UI

### 8.36.0
#### Enhancement
* Support Sharing link between two EinkBro App in different devices

#### Issue fix
* Crash when loading a long web page

### Feature
* **Receive data** and **Send link**
!(https://user-images.githubusercontent.com/4084738/169814052-6de1f7c4-7c86-4ce9-8eec-9f4449e1b9f9.png)
Receive data can receive data from another EinkBro or Sharik APP on another device that is also refactored by me https://github.com/plateaukao/sharik

### 8.35.0
#### Enhancement
* Add recently used bookmark items in new tab (can be enabled in Settings)
* Support installing app after downloading apk file

### 8.34.1
#### Issue fix
Fixed bluetooth up/down key page navigation fail issue

#### Feature
* Add navigation back icon on Setting screen

### 8.34.0
#### Issue fix
* Crash when epub file cannot be opened

#### Feat: 
* Add file size to epub list in open save epub dialogs

## something wrong with previous app-release.apk, so I re-compiled it and uploaded again.

### 8.33.0
#### Enhancement
* Long press font size button to configure font type
* Update string translation
* Pause WebView logic when App is not in foreground

### 8.32.0
#### Translation
* Update Chinese translations (Thanks to @xBcai)

#### Enhancement
* Update user-agent handlings to have newer browser version
* Hide dark mode configuration for lower version
* Open last article when opening an epub file that's just saved a new article
* Long press on font size button on toolbar to configure font type

#### Contributors
!(https://avatars.githubusercontent.com/u/66826351?s=64&v=4) xBcai

### 8.31.0
#### Feature
* Support toolbar on top (option in Settings -> Behavior/UI)
* Enable VI key bindings (option in Settings -> Bhavior/UI)

### 8.30.1
#### Enhancement
* Hand made white background active icon
* Two finger gesture co-work with zooming feature

#### Issue fix
* Hide toolbar when reading an epub file

#### Feature
* Add desktop mode button in toolbar

### 8.30.0
#### Feature
* Support non http/https url to be opened by system
* Add quick toggle icon in menu

#### Enhancement
* Enhance bold font icon
* Enhance white background icon

### 8.29.2
#### Issue fixing
* Fix sometimes touch area not working
* Fix crash when epub file can't be opened sometimes

#### Enhancement
* Put bookmark list icon in default toolbar actions
* Now multi-touch gesture can work after configure it in Settings

### 8.29.1
#### Issue fix
* Long press on link, no pupup window is displayed.

### 8.29.0
#### Feature
* Better tab count button UI
* Finetune epub reader TOC list UI

#### Issue fixing

* Fix touch area hint always displayed when changing configuration
* Fix saved tab list does not have up-to-date url

### 8.28.0
#### Feature
* Add local font support in Setting -> Font -> Font Type
* Add **show bookmark** action in gesture
* Fine tune translation dialog logic
* Separate toolbar configuration for portrait and landscape
* Open epub file in EinkBro

### 8.27.1
#### Fix
* Long press click not working, when two finger touch is enabled...

### 8.27.0
#### enhancement
* improve save epub process: now you can select previously saved epub file easily.
* remove unused resources: now EinkBro is even smaller!
* add open epub button in menu: possible to open previously saved epub
* two finger swipe gesture is supported now! Find it in Setting -> Gestures

### 8.26.0
#### issue fix
* tab preview title would be correct even if bold font is set.
* fix memory leak
* fix back button behavior: don't close tab if there's no previous page

#### enhancement
* improve new tab loading performance (preload webview)

### 8.25.0
#### Feature
* Add configuration to hide touch area dot line when keyboard is displayed (access from touch config dialog)
* Add configuration to switch touch area actions (page down to page up, and vice versa)
* Add better translate feature: Google in Place
* Add shortcut to long press App icon, to launch only favorite url (when sometimes saved tabs have problem)
* Add Translate button in menu, for easier access

#### Fix
* fix search keyword parsing issue
* fix bold font feature causes tab preview title becomes blank.


### 8.16.0
#### enhancement
* much faster web loading performance!!
* purge histories older than 2 weeks

#### features
* add folder button in Bookmark edit dialog

#### issue fixes
* fix translation sync button status
* fix incognito icon status

### 8.15.1

#### issue fixes
* fix long press on web content, the dialog is popup too even it's text

### 8.15.0
#### features
* long press back button to show recent history
* add hide button on url address bar
* hide translation control buttons when scrolling
* remember translation orientation
* support Adblock site addition (long press on Ad images)

#### enhancement
* Improve web loading performance by using cache

#### issue fixes
* fix desktop issue

### 8.14.0
- Much improved the web content loading speed by removing saving screenshot feature.

### 8.13.1
- fine tune touch area hint dash line
- make translation buttons transparent so they won't overlap on web content.

### 8.13.0
- support opening a new Activity, so that original Activity can continue play video or audio.
- add google and papago translate feature for url format.
- add scroll sync when scrolling on translation panel (only works in some websites)
- fix layout issue when larger font is used.

### 8.12.1
- remove safeBrowsingEnabled since it's enabled by default.

### 8.12.0

- Add keep awake option in settings
- Add vertical translation option
- Add download file name edit dialog
- Add touch area option: middle on two sides
- Fix preview tab refresh issue

### 8.11.0

- Change tab preview image to list to improve web loading performance
- Support resizable two pane layout for translation
- Support font size change in translation panel
- Show bookmark dialog when saving a new bookmark

### 8.10.0

- Add translation icon in Toolbar configuration
- Long press translation icon to configure which website to use
- Pagination design for text translation
- Toolbar refactoring: when adding too many icons, it can be scrolled horizontally
- Support for new a incognito tab (from tab preview screen)
- UI improvement: add separators in Menu, better touch effect in preference settings, clear button in address bar

### 8.9.1

- Fix full text translation issue

### 8.9.0

- Better rendering performance
- Support web content translation in some Eink devices
- Setting icon in Toolbar configuration is not changeable now

### 8.8.2

- Fix issues:
 - fix saving epub crash
 - fix reopen App crash

- Feature:
 - modify Tab Preview Layout
 - long press tab count icon to turn on incognito mode

### 8.8.0

- support dark mode (if you want to use EinkBro in normal phones/tablets)
- add incognito mode in quick toggle dialog (long press 3 dot icon)
- add rotate screen action in tool bar configuration dialog

### 8.7.6

- Fix crash on Android OS5 when opening toolbar setting dialog
- When sharing text with EinkBro, it will remove prefix before launching url
- Export bookmark file as text file, instead of json extension
- Update strings for zh-rCN

### 8.7.5

- Support creating folder in bookmarks (click on 3 dot when bookmark list is displayed)
- Support backup and restore bookmark data
- Support Android OS 5 (SDK level 21)


### 8.7.4

- Add web navigation forward action. You can add it by configure toolbar customization.
- Add option to save current browsing tabs, so that next time, all the tabs are created automatically (in Setting -> UI, bottom par)
- Fix Onyx device crash issue when spliting screens.

### 8.7.3

- add font increase/decrease action to toolbar actions. Add them from toolbar configuration dialog
- add fullscreen action to toolbar actions.
- now it's possible to disable volume key turning pages. Long press setting button, and click on Volume icon with label "page"
- enhance toolbar configuration dialog layout

### 8.7.2

- enhance dialog layout

### 8.7.1

- fix another saving image issue in epub file

### 8.7.0

- fix saving image issue in epub file

### 8.6.7

- keep fixing images in epub saving
- add version info to About dialog

### 8.6.6

- fix saving to existing epub, sometimes images cannot be saved.
(still, if the image downloading needs authentication, it still can't be saved)

### 8.6.5

- now shortcut really works, and don't need to choose browser
- fix launch app, sometimes extra tab is created

### 8.6.4

- fix launch blan screen issue

### 8.6.3

- fix save history toggleing default value to true
- add description of why each permissions are used in full description in fastlane

### 8.6.2

- fix crash issues

### 8.6.0

- Support saving web page to an epub file, either creating a new one, or adding to an existing one.
- Hide status bar when toolbar is hidden

### 8.5.1

- Use Mozilla readerview for Reader Mode. much better results.

### 8.4.2

- Add page turn reserve height configuration
- Fix issues
- Make font bold and more readable (in Settings -> Behavior/UI)
- Add bold font style to toolbar setting
- Add options to set default printed pdf paper size
- Add font style (serif) support from webfont in Settings -> Beavior/UI



### 8.3.1

- fix crash issue when clicking on some links
- add reader mode (beta)

### 8.3

- Add touch area configuration for page Up/Down (long press on the finger icon on toolbar)
- Improve UI for quick toggle setting dialog

### 8.2

- Now toolbar actions can be customized. Go to Settings -> Behavior/UI -> Icons on toolbar and configure.
- Add touch area hint when turning on page-turn feature by touching on screen. You can also long click on the button to keep area hint always on.
- Add labels to quick toggle settings for easier knowing what these icons are for
- Now it's possible to keep playing audio when EinkBro is in the background. (the media should be played in current tab). This can be turned on in quick toggle settings (long click on three dot button on tool bar)
- Long press on left touch area will scroll the webpage to top. (only work when touch feature turned on)

### 8.1
- Add Chinese translation
- Use website favicon as shortcut Icon
- Add a gesture button on toolbar to allow tap screen right/left side to turn pages

### 8.0
- Support assigning gestures pageUp / pageDown for tool bar and navigation button
- Make navigation button more visible no matter the webpage background color
- Remove button press visual feedback
- Re-layout menu items for easier access in different resolution devices
- Support Android SDK level 19
- Change package id from original one to info.plateaukao.einkbro
- Support Vi key bindings when using physical keyboard
b : open bookmarks
d : remove current tab
gg : go to top
j : page down
k : page up
h : go back
l : go forward
o : focus on url bar
t : new a tab
vi : zoom in font
vo : zoom out font
/ : search in page
G : go to bottom
J : show next tab
K : show previous tab


### 7.2
- Re-organize menu items so that they can be accessed without switching between tabs.
- Add font size configuration in first layer setting.
- Refactor overview popup, so that it's no longer BottomSheetDialog, which shows un-necessary show/hide animations.
- Remove gray mask when overview pop up. (This is not necessary in E-ink device)
- Remove wv from useragent string, so that Google Login works.
- Modify floating action button design, so that it only outlines the button without blocking underlying content.
- Add Multi-Window support
- When adding a new tab, by default, keyboard will popup, so that users can start input the search keyword, or input the url directly.
- Heavily refactoring BrowserActivity; adopt Kotlin for better coding. Reduce BrowserActivity codes from 2500 lines to 1900 lines.

### 7.1
- support using volume key for pageUp/pageDown.

### v7.0
- Add pageUp / pageDown / Back button in bottom function bar
- Add Desktop mode feature
- Make all icons in high contrast colors
- Add WebView count in bottom function bar
