package com.fidesmo.mifaretutorial;

/**
 * Data structure for a rudimentary session control: each sessionID will map to this
 * set of variables that keep the session's state
 */
public class SessionState {
    private long counter;
    private boolean firstTime;

    public SessionState() {
        counter = 0;
        firstTime = false;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public boolean isFirstTime() {
        return firstTime;
    }

    public void setFirstTime(boolean firstTime) {
        this.firstTime = firstTime;
    }
}
