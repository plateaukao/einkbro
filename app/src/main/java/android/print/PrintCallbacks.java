package android.print;

/**
 * Bridge to drive {@link PrintDocumentAdapter} directly without {@link PrintManager}.
 * The framework's LayoutResultCallback / WriteResultCallback have package-private
 * constructors, which prevents subclassing from outside android.print. These
 * thin subclasses re-expose them with public constructors so callers (e.g. saving
 * a WebView as PDF without relying on the system print spooler) can override the
 * relevant callback methods.
 */
public final class PrintCallbacks {
    private PrintCallbacks() {}

    public static abstract class LayoutCallback extends PrintDocumentAdapter.LayoutResultCallback {
        public LayoutCallback() {}
    }

    public static abstract class WriteCallback extends PrintDocumentAdapter.WriteResultCallback {
        public WriteCallback() {}
    }
}
