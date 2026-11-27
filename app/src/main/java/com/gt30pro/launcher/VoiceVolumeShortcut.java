package com.gt30pro.launcher;

/** Caller supplies only non-repeating volume-down key events. */
final class VoiceVolumeShortcut {
    private int previous;
    private long time;
    boolean press(int key, long now) {
        if (key==previous && now>=time && now-time<=600) { previous=0; time=0; return true; }
        previous=key; time=now; return false;
    }
}
