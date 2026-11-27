package com.gt30pro.launcher;
public class NoteTextTest {
    public static void main(String[] args) {
        if(NoteText.valid("", "") || NoteText.valid(" \n", " ") || NoteText.valid(null,"x")) throw new AssertionError("empty");
        if(!NoteText.valid("judul", "") || !NoteText.valid("", "isi")) throw new AssertionError("title or body");
        if(!NoteText.displayTitle("  ").equals("Tanpa judul") || !NoteText.displayTitle("  abc ").equals("abc")) throw new AssertionError("display");
        StringBuilder t=new StringBuilder(),b=new StringBuilder();
        for(int i=0;i<120;i++) t.append('x'); for(int i=0;i<20000;i++) b.append('y');
        if(!NoteText.valid(t.toString(),b.toString())) throw new AssertionError("limits inclusive");
        if(NoteText.valid(t+"x","") || NoteText.valid("",b+"y")) throw new AssertionError("limits exceeded");
        System.out.println("Note validation, title fallback and length limits OK");
    }
}
