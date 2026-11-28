package com.gt30pro.launcher;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import org.json.JSONException;
public class NoteBackupTest {
    private static void reject(String json) throws Exception { try { NoteBackup.decode(json); throw new AssertionError("accepted: "+json); } catch(JSONException expected) {} }
    public static void main(String[] args) throws Exception {
        ArrayList<NoteStore.Note> original=new ArrayList<>();
        original.add(new NoteStore.Note(99,"猫","☑ Teh\n",123,1,2,1,"Kerja",4));
        original.add(new NoteStore.Note(100,"","",42,0,0,0,"",0));
        byte[] bytes=NoteBackup.encode(original); ArrayList<NoteStore.Note> copy=NoteBackup.decode(NoteBackup.read(new ByteArrayInputStream(bytes)));
        if(copy.size()!=2 || copy.get(0).id!=0 || !copy.get(0).body.equals("☑ Teh\n") || copy.get(0).state!=2 || copy.get(0).previousState!=1 || copy.get(0).color!=4 || !copy.get(0).tag.equals("Kerja")) throw new AssertionError("roundtrip");
        String good=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        reject(good+" garbage"); reject(good.replace("gt30-notes","other")); reject(good.replace("\"version\":1","\"version\":2"));
        reject(good.replace("\"pinned\":1","\"pinned\":2")); reject(good.replace("\"body\":\"☑ Teh\\n\"","\"body\":123"));
        try { NoteBackup.read(new ByteArrayInputStream(new byte[]{(byte)0xc3,0x28})); throw new AssertionError("invalid UTF-8"); } catch(java.io.IOException expected) {}
        try { NoteBackup.read(new ByteArrayInputStream(new byte[NoteBackup.MAX_BYTES+1])); throw new AssertionError("size limit"); } catch(java.io.IOException expected) {}
        System.out.println("Note backups: round trip, metadata, invalid data and size limits OK");
    }
}
