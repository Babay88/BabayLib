package ru.babay.lib.transport;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 26.04.13
 * Time: 3:15
 */
public enum TTL {
    Day, TwoDays, Week;

    public long getDurationMs() {
        switch (this) {
            case Day:
                return 24 * 60 * 60 * 1000;
            case TwoDays:
                return 2 * 24 * 60 * 60 * 1000;
            case Week:
                return 7 * 24 * 60 * 60 * 1000;

            default:
                throw new IllegalStateException();
        }
    }
}
