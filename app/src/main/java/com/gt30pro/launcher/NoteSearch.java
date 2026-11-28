package com.gt30pro.launcher;
import java.util.Locale;
final class NoteSearch {
    static boolean matches(String title,String body,String tag,String query) {
        String q=query.trim().toLowerCase(Locale.ROOT);
        return q.isEmpty() || title.toLowerCase(Locale.ROOT).contains(q) || body.toLowerCase(Locale.ROOT).contains(q) || tag.toLowerCase(Locale.ROOT).contains(q);
    }
    private NoteSearch() { }
}
