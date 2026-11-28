package com.gt30pro.launcher;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import org.json.JSONException;
import java.util.ArrayList;

final class NoteFormatEditor {
    static void apply(Spannable text,String json) throws JSONException {
        for(NoteFormats.Run r:NoteFormats.read(json,text.length())) {
            Object span=r.type.equals("underline")?new UnderlineSpan():new StyleSpan(r.type.equals("bold")?Typeface.BOLD:Typeface.ITALIC);
            text.setSpan(span,r.start,r.end,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    static String capture(Spannable text) throws JSONException {
        ArrayList<NoteFormats.Run> runs=new ArrayList<>();
        for(StyleSpan span:text.getSpans(0,text.length(),StyleSpan.class)) {
            int start=text.getSpanStart(span),end=text.getSpanEnd(span); if(start>=end) continue;
            if((span.getStyle() & Typeface.BOLD)!=0) runs.add(new NoteFormats.Run(start,end,"bold"));
            if((span.getStyle() & Typeface.ITALIC)!=0) runs.add(new NoteFormats.Run(start,end,"italic"));
        }
        for(UnderlineSpan span:text.getSpans(0,text.length(),UnderlineSpan.class)) {
            int start=text.getSpanStart(span),end=text.getSpanEnd(span); if(start<end) runs.add(new NoteFormats.Run(start,end,"underline"));
        }
        return NoteFormats.write(runs,text.length());
    }
    /** Toggle just the selected characters without removing formatting outside the selection. */
    static boolean toggle(Editable text,int start,int end,String type) {
        if(start<0 || end<=start || end>text.length()) return false;
        boolean[] marked=new boolean[text.length()];
        if(type.equals("underline")) {
            for(UnderlineSpan span:text.getSpans(0,text.length(),UnderlineSpan.class)) { int a=text.getSpanStart(span),b=text.getSpanEnd(span); for(int i=Math.max(0,a);i<b && i<marked.length;i++) marked[i]=true; text.removeSpan(span); }
        } else {
            int flag=type.equals("bold")?Typeface.BOLD:Typeface.ITALIC;
            for(StyleSpan span:text.getSpans(0,text.length(),StyleSpan.class)) {
                int style=span.getStyle(); if((style & flag)==0) continue;
                int a=text.getSpanStart(span),b=text.getSpanEnd(span); for(int i=Math.max(0,a);i<b && i<marked.length;i++) marked[i]=true; text.removeSpan(span);
                int other=style & ~flag; if(other!=0 && a>=0 && a<b) text.setSpan(new StyleSpan(other),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        boolean all=true; for(int i=start;i<end;i++) if(!marked[i]) { all=false; break; }
        for(int i=start;i<end;i++) marked[i]=!all;
        for(int i=0;i<marked.length;) {
            if(!marked[i]) { i++; continue; } int a=i; while(i<marked.length && marked[i]) i++;
            Object span=type.equals("underline")?new UnderlineSpan():new StyleSpan(type.equals("bold")?Typeface.BOLD:Typeface.ITALIC);
            text.setSpan(span,a,i,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return true;
    }
    private NoteFormatEditor() {}
}
