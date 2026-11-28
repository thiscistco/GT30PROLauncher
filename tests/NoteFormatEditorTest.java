package com.gt30pro.launcher;
import android.text.Spannable;
import android.text.Editable;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import java.util.ArrayList;
class FakeText implements Editable {
    final String value; final ArrayList<Object> spans=new ArrayList<>(); final ArrayList<Integer> starts=new ArrayList<>(),ends=new ArrayList<>();
    FakeText(String s){value=s;}
    public int length(){return value.length();} public char charAt(int i){return value.charAt(i);} public CharSequence subSequence(int a,int b){return value.subSequence(a,b);}
    public <T> T[] getSpans(int a,int b,Class<T> type){ArrayList<T> result=new ArrayList<>(); for(Object s:spans)if(type.isInstance(s))result.add(type.cast(s)); @SuppressWarnings("unchecked") T[] arr=(T[])java.lang.reflect.Array.newInstance(type,result.size()); return result.toArray(arr);}
    public int getSpanStart(Object s){int i=spans.indexOf(s);return i<0?-1:starts.get(i);} public int getSpanEnd(Object s){int i=spans.indexOf(s);return i<0?-1:ends.get(i);}
    public void setSpan(Object s,int a,int b,int flags){spans.add(s);starts.add(a);ends.add(b);} public void removeSpan(Object s){int i=spans.indexOf(s);if(i>=0){spans.remove(i);starts.remove(i);ends.remove(i);}}
}
public class NoteFormatEditorTest {
    public static void main(String[] args) throws Exception {
        FakeText t=new FakeText("abcdef");
        if(!NoteFormatEditor.toggle(t,1,5,"bold")) throw new AssertionError();
        NoteFormatEditor.toggle(t,2,4,"italic"); NoteFormatEditor.toggle(t,2,3,"underline");
        String json=NoteFormatEditor.capture(t); FakeText copy=new FakeText("abcdef"); NoteFormatEditor.apply(copy,json);
        if(!NoteFormatEditor.capture(copy).equals(json)) throw new AssertionError("roundtrip");
        NoteFormatEditor.toggle(copy,2,4,"bold");
        ArrayList<NoteFormats.Run> runs=NoteFormats.read(NoteFormatEditor.capture(copy),6);
        boolean left=false,right=false,italic=false,underline=false;
        for(NoteFormats.Run r:runs){if(r.type.equals("bold") && r.start==1 && r.end==2)left=true; if(r.type.equals("bold")&&r.start==4&&r.end==5)right=true; if(r.type.equals("italic")&&r.start==2&&r.end==4)italic=true; if(r.type.equals("underline")&&r.start==2&&r.end==3)underline=true;}
        if(!left||!right||!italic||!underline) throw new AssertionError("partial toggle must preserve surrounding styles");
        if(NoteFormatEditor.toggle(copy,2,2,"bold"))throw new AssertionError("empty selection");
        System.out.println("Visual formatting selection, overlap and roundtrip OK");
    }
}
