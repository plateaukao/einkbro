package de.baumann.browser.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.RecordUnit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class AdBlock {
    private static final String FILE = "hosts.txt";
    private static final Set<String> hosts = new HashSet<>();
    private static final List<String> whitelist = new ArrayList<>();
    @SuppressLint("ConstantLocale")
    private static final Locale locale = Locale.getDefault();

    private static void loadHosts(final Context context) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AssetManager manager = context.getAssets();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(manager.open(FILE)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        hosts.add(line.toLowerCase(locale));
                    }
                } catch (IOException i) {
                    Log.w("browser", "Error loading hosts", i);
                }
            }
        });
        thread.start();
    }

    private synchronized static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        whitelist.clear();
        whitelist.addAll(action.listDomains(RecordUnit.TABLE_WHITELIST));
        action.close();
    }

    private static String getDomain(String url) throws URISyntaxException {
        url = url.toLowerCase(locale);

        int index = url.indexOf('/', 8); // -> http://(7) and https://(8)
        if (index != -1) {
            url = url.substring(0, index);
        }

        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            return url;
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private final Context context;

    public AdBlock(Context context) {
        this.context = context;

        if (hosts.isEmpty()) {
            loadHosts(context);
        }
        loadDomains(context);
    }

    public boolean isWhite(String url) {
        for (String domain : whitelist) {
            if (url != null && url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    boolean isAd(String url) {
        String domain;
        try {
            domain = getDomain(url);
        } catch (URISyntaxException u) {
            return false;
        }
        return hosts.contains(domain.toLowerCase(locale));
    }

    public synchronized void addDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomain(domain, RecordUnit.TABLE_WHITELIST);
        action.close();
        whitelist.add(domain);
    }

    public synchronized void removeDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomain(domain, RecordUnit.TABLE_WHITELIST);
        action.close();
        whitelist.remove(domain);
    }

    public synchronized void clearDomains() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearDomains();
        action.close();
        whitelist.clear();
    }
}
