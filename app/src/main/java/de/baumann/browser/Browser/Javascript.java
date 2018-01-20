package de.baumann.browser.Browser;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.baumann.browser.Database.RecordAction;

public class Javascript {
    private static final String FILE = "javaHosts.txt";
    private static final Set<String> hostsJS = new HashSet<>();
    private static final List<String> whitelistJS = new ArrayList<>();
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
                        hostsJS.add(line.toLowerCase(locale));
                    }
                } catch (IOException i) {
                    Log.w("Browser", "Error loading hosts");
                }
            }
        });
        thread.start();
    }

    private synchronized static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        whitelistJS.clear();
        whitelistJS.addAll(action.listDomainsJS());
        action.close();
    }

    private final Context context;

    public Javascript(Context context) {
        this.context = context;

        if (hostsJS.isEmpty()) {
            loadHosts(context);
        }
        loadDomains(context);
    }

    public boolean isWhite(String url) {
        for (String domain : whitelistJS) {
            if (url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomainJS(domain);
        action.close();
        whitelistJS.add(domain);
    }

    public synchronized void removeDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomainJS(domain);
        action.close();
        whitelistJS.remove(domain);
    }

    public synchronized void clearDomains() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearDomainsJS();
        action.close();
        whitelistJS.clear();
    }
}
