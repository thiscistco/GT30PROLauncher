package com.gt30pro.launcher;

/** Shared validation and display logic; empty titles are allowed when a body is present. */
final class NoteText {
    static final int TITLE_LIMIT=120, BODY_LIMIT=20000;
    static boolean valid(String title,String body) {
        return title!=null && body!=null && title.length()<=TITLE_LIMIT && body.length()<=BODY_LIMIT &&
                (!title.trim().isEmpty() || !body.trim().isEmpty());
    }
    static String displayTitle(String title) { return title.trim().isEmpty() ? "Tanpa judul" : title.trim(); }
    private NoteText() { }
}
