package org.fxpart.common.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by metairie on 13-Jul-15.
 */
public class SearchThreadFactory implements ThreadFactory {
    static final AtomicInteger poolNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "Search-Autosuggest-" + poolNumber.getAndIncrement() + "-thread");
        //thread.setDaemon(true);
        return thread;
    }
}