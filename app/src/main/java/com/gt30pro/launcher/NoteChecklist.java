package com.gt30pro.launcher;
import java.util.ArrayList;
/** A checklist line begins with ☐ or ☑; unrelated text remains unchanged. */
final class NoteChecklist {
    static final class Item { final int line; final String text; final boolean checked; Item(int line,String text,boolean checked){this.line=line;this.text=text;this.checked=checked;} }
    static ArrayList<Item> items(String body) {
        String[] lines=body.split("\n",-1); ArrayList<Item> out=new ArrayList<>();
        for(int i=0;i<lines.length;i++) if(lines[i].startsWith("☐ ") || lines[i].startsWith("☑ ")) out.add(new Item(i,lines[i].substring(2),lines[i].startsWith("☑ ")));
        return out;
    }
    static String toggle(String body,int line,boolean checked) {
        String[] lines=body.split("\n",-1); if(line<0 || line>=lines.length || !(lines[line].startsWith("☐ ") || lines[line].startsWith("☑ "))) return body;
        lines[line]=(checked?"☑ ":"☐ ")+lines[line].substring(2); StringBuilder out=new StringBuilder();
        for(int i=0;i<lines.length;i++){if(i>0)out.append('\n');out.append(lines[i]);} return out.toString();
    }
    private NoteChecklist() { }
}
