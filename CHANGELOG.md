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
