package org.fxpart.common.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by metairie on 04-Sep-15.
 */
public class TimerThreadFactory implements ThreadFactory{
    static final AtomicInteger poolNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "Timer-Autosuggest-" + poolNumber.getAndIncrement() + "-thread");
    }
}