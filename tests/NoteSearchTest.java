package com.gt30pro.launcher;
public class NoteSearchTest {
    public static void main(String[] args) {
        if(!NoteSearch.matches("Judul Penting","Belanja susu","Kerja","PENTING")) throw new AssertionError("title");
        if(!NoteSearch.matches("Judul","Belanja susu","Kerja"," SUSU ")) throw new AssertionError("body");
        if(!NoteSearch.matches("Judul","Isi","Kerja","kerja")) throw new AssertionError("tag");
        if(!NoteSearch.matches("Judul","Isi","Kerja","  ")) throw new AssertionError("empty");
        if(NoteSearch.matches("Judul","Isi","Kerja","lain")) throw new AssertionError("no match");
        System.out.println("Notes instant search by title, body and tag OK");
    }
}
