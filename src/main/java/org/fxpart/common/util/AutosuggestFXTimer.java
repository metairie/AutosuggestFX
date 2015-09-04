package org.fxpart.common.util;

import java.util.Timer;

/**
 * singleton of Timer
 */
public class AutosuggestFXTimer extends Timer {
    private AutosuggestFXTimer() {
    }

    private static AutosuggestFXTimer singleton = null;

    public static AutosuggestFXTimer getInstance() {
        if (singleton != null) {
            singleton.cancel();
            singleton.purge();
            singleton = null;
        }
        singleton = new AutosuggestFXTimer();
        return singleton;
    }
}