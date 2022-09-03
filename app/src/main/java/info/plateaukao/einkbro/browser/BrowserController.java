package info.plateaukao.einkbro.browser;

import android.net.Uri;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

public interface BrowserController {
    void updateProgress(int progress);
    void updateTitle(String title);
    void addNewTab(String url);
    void showAlbum(AlbumController albumController);
    void removeAlbum(AlbumController albumController);
    void showFileChooser(ValueCallback<Uri[]> filePathCallback);
    void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);
    void onLongPress(Message message);
    void hideOverview ();
    void addHistory(String title, String url);
    boolean onHideCustomView();
    boolean handleKeyEvent(KeyEvent event);
    boolean loadInSecondPane(String url);

    void updateTabs();
}
