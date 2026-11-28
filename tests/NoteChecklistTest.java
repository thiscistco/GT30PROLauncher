package com.gt30pro.launcher;
import java.util.ArrayList;
public class NoteChecklistTest {
    public static void main(String[] args) {
        String body="Teks biasa\n☐ Susu\n☑ Teh\n\n";
        ArrayList<NoteChecklist.Item> items=NoteChecklist.items(body);
        if(items.size()!=2 || items.get(0).line!=1 || items.get(0).checked || !items.get(1).checked) throw new AssertionError("parse");
        String changed=NoteChecklist.toggle(body,1,true);
        if(!changed.equals("Teks biasa\n☑ Susu\n☑ Teh\n\n")) throw new AssertionError("check, preserve line breaks");
        if(!NoteChecklist.toggle(changed,2,false).equals("Teks biasa\n☑ Susu\n☐ Teh\n\n")) throw new AssertionError("uncheck");
        if(!NoteChecklist.toggle(body,0,true).equals(body) || !NoteChecklist.toggle(body,100,true).equals(body)) throw new AssertionError("ignore invalid");
        System.out.println("Checklist parse, toggle and preserve text OK");
    }
}
