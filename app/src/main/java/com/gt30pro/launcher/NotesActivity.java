package com.gt30pro.launcher;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** Local notes: explicit save, confirmation before discarding or deleting. */
public final class NotesActivity extends SwipeBackActivity {
    private AppUi ui; private Typeface font; private NoteStore store;
    private LinearLayout content; private EditText title,body;
    private boolean editing; private long editId;
    private String originalTitle="",originalBody="";
    @Override protected boolean supportsBottomSwipe() { return !editing; }
    private int dp(int n) { return ui.dp(this,n); }
    private TextView text(String value,int size,int color) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(font); return t;
    }
    private Button button(String label,Runnable click) {
        Button b=new Button(this); b.setAllCaps(false); b.setText(label); b.setTypeface(font); b.setTextColor(ui.ink);
        b.setMinHeight(dp(48)); b.setBackground(ui.surface(this,ui.control)); b.setOnClickListener(v -> click.run()); return b;
    }
    private void add(View view) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(4)); content.addView(view,p); }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); ui=new AppUi(this); ui.window(this); font=Typeface.createFromAsset(getAssets(),"fonts/Doto.ttf"); store=new NoteStore(this);
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(ui.canvas);
        page.setOnApplyWindowInsetsListener((v,insets) -> { page.setPadding(dp(20),dp(16)+insets.getSystemWindowInsetTop(),dp(20),dp(16)+insets.getSystemWindowInsetBottom()); return insets; });
        LinearLayout header=ui.header(this,font,"Catatan"); header.getChildAt(0).setOnClickListener(v -> onBackPressed()); page.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,0,0,dp(80)); scroll.addView(content);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(page);
        if(state!=null && state.getBoolean("editing")) {
            editId=state.getLong("id"); originalTitle=state.getString("originalTitle",""); originalBody=state.getString("originalBody",""); editing=true;
            editor(state.getString("title",""),state.getString("body",""));
        } else showList();
    }
    @Override protected void onResume() {
        super.onResume(); AppUi current=new AppUi(this);
        if(ui!=null && (ui.dark!=current.dark || !ui.choice.equals(current.choice))) recreate();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putBoolean("editing",editing);
        if(editing && title!=null && body!=null) {
            out.putLong("id",editId); out.putString("originalTitle",originalTitle); out.putString("originalBody",originalBody);
            out.putString("title",title.getText().toString()); out.putString("body",body.getText().toString());
        }
    }
    @Override protected void onDestroy() { if(store!=null) store.close(); super.onDestroy(); }
    private void message(String msg) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show(); }
    private void hideKeyboard() {
        View focused=getCurrentFocus(); InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(focused!=null && imm!=null) imm.hideSoftInputFromWindow(focused.getWindowToken(),0);
    }
    private void showList() {
        editing=false; title=null; body=null; content.removeAllViews();
        add(text("Tersimpan hanya di perangkat ini. Menghapus aplikasi juga menghapus catatan.",12,ui.muted));
        final ArrayList<NoteStore.Note> notes;
        try { notes=store.list(); } catch(SQLiteException e) { add(text("Catatan tidak dapat dibaca. Data lama tidak dihapus.",14,ui.ink)); return; }
        add(button("+ Catatan baru",() -> openEditor(null)));
        if(notes.isEmpty()) { add(text("Belum ada catatan.",14,ui.muted)); return; }
        for(NoteStore.Note note:notes) {
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(12),dp(10),dp(12),dp(10)); card.setBackground(ui.surface(this,ui.card));
            TextView heading=text(NoteText.displayTitle(note.title),16,ui.ink); heading.setTypeface(font,Typeface.BOLD); heading.setMaxLines(1); heading.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(heading);
            if(!note.body.trim().isEmpty()) { TextView summary=text(note.body,13,ui.muted); summary.setMaxLines(2); summary.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(summary); }
            card.setFocusable(true); card.setContentDescription("Buka catatan " + NoteText.displayTitle(note.title)); card.setOnClickListener(v -> openEditor(note)); add(card);
        }
    }
    private void openEditor(NoteStore.Note note) {
        editId=note==null?0:note.id; originalTitle=note==null?"":note.title; originalBody=note==null?"":note.body;
        editing=true; editor(originalTitle,originalBody);
    }
    private EditText field(String hint,int limit,boolean multiline) {
        EditText e=new EditText(this); e.setHint(hint); e.setTextColor(ui.ink); e.setHintTextColor(ui.muted); e.setTypeface(font); e.setTextSize(16);
        e.setPadding(dp(12),dp(12),dp(12),dp(12)); e.setBackground(ui.surface(this,ui.card)); e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        if(multiline) { e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); e.setGravity(Gravity.TOP|Gravity.START); e.setMinLines(7); }
        else { e.setSingleLine(true); e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); }
        return e;
    }
    private void editor(String initialTitle,String initialBody) {
        content.removeAllViews(); add(text(editId==0?"Catatan baru":"Edit catatan",18,ui.ink));
        title=field("Judul (opsional)",NoteText.TITLE_LIMIT,false); title.setContentDescription("Judul catatan, opsional"); title.setText(initialTitle); add(title);
        body=field("Tulis catatan",NoteText.BODY_LIMIT,true); body.setContentDescription("Isi catatan"); body.setText(initialBody); add(body);
        add(button("Simpan",this::save)); add(button("Batal",this::cancelEditor));
        if(editId!=0) add(button("Hapus catatan",this::confirmDelete));
    }
    private void save() {
        String t=title.getText().toString(), b=body.getText().toString();
        if(!NoteText.valid(t,b)) { message("Isi judul atau catatan sebelum menyimpan."); return; }
        try { if(!store.save(editId,t,b)) { message("Catatan tidak tersimpan atau sudah tidak ada."); return; } }
        catch(SQLiteException e) { message("Tidak dapat menyimpan catatan. Perubahan masih di editor."); return; }
        hideKeyboard(); showList(); message("Catatan disimpan.");
    }
    private void confirmDelete() {
        ui.dialog(this).setTitle("Hapus catatan?").setMessage("Catatan yang dihapus tidak dapat dipulihkan.")
            .setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w) -> {
                try { if(!store.delete(editId)) { message("Catatan tidak ditemukan."); return; } }
                catch(SQLiteException e) { message("Tidak dapat menghapus catatan."); return; }
                hideKeyboard(); showList(); message("Catatan dihapus.");
            }).show();
    }
    private void cancelEditor() {
        if(!originalTitle.equals(title.getText().toString()) || !originalBody.equals(body.getText().toString())) {
            ui.dialog(this).setTitle("Buang perubahan?").setMessage("Perubahan belum disimpan.")
                .setNegativeButton("Lanjutkan mengedit",null).setPositiveButton("Buang",(d,w)->{ hideKeyboard(); showList(); }).show();
        } else { hideKeyboard(); showList(); }
    }
    @Override public void onBackPressed() { if(editing) cancelEditor(); else super.onBackPressed(); }
}
