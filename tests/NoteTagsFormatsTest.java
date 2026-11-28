package com.gt30pro.launcher;
import java.util.ArrayList;
import org.json.JSONException;
public class NoteTagsFormatsTest {
    static void badTags(String s) throws Exception { try { NoteTags.read(s,""); throw new AssertionError("accepted "+s); } catch(JSONException expected) {} }
    static void badFormats(String s) throws Exception { try { NoteFormats.read(s,5); throw new AssertionError("accepted "+s); } catch(JSONException expected) {} }
    public static void main(String[] args) throws Exception {
        if(!NoteTags.read(null,"lama").get(0).equals("lama") || !NoteTags.read(null,"").isEmpty()) throw new AssertionError("legacy");
        ArrayList<String> tags=NoteTags.read("[\"Kerja\",\"Sekolah\"]","");
        if(!NoteTags.display(tags).equals("#Kerja  #Sekolah") || NoteTags.read(NoteTags.write(tags),"").size()!=2) throw new AssertionError("tags");
        badTags("[\"Tag\",\"tag\"]"); badTags("[42]"); badTags("[\" a \" ]");
        ArrayList<NoteFormats.Run> runs=NoteFormats.read("[{\"start\":0,\"end\":2,\"type\":\"bold\"},{\"start\":1,\"end\":5,\"type\":\"underline\"}]",5);
        if(NoteFormats.read(NoteFormats.write(runs,5),5).size()!=2) throw new AssertionError("formats");
        badFormats("[{\"start\":0,\"end\":6,\"type\":\"bold\"}]"); badFormats("[{\"start\":1,\"end\":1,\"type\":\"bold\"}]"); badFormats("[{\"start\":0,\"end\":1,\"type\":\"script\"}]"); badFormats("[{\"start\":0.5,\"end\":1,\"type\":\"bold\"}]");
        System.out.println("Tag migration and text format validation OK");
    }
}
