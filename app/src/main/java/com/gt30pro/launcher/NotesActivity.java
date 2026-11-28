package com.gt30pro.launcher;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** Local notes: debounced auto-save, search, organization and recoverable deletion. */
public final class NotesActivity extends SwipeBackActivity {
    private AppUi ui; private Typeface font; private NoteStore store;
    private LinearLayout content, cards; private EditText title,body,search; private TextView saveStatus;
    private boolean editing; private long editId; private String lastTitle="",lastBody="",lastTag="",draftTag="";
    private TextView tagValue;
    private int section=NoteStore.ACTIVE; private String query=""; private int layout;
    private static final int EXPORT_TXT=601, EXPORT_PDF=602, BACKUP_WRITE=603, BACKUP_READ=604;
    private String exportTitle="",exportBody="";
    private final Handler handler=new Handler();
    private final Runnable autoSave=() -> saveDraft(false);
    private static final int[] LIGHT={0,0xFFFFE2D7,0xFFFFF1C9,0xFFDDF1DC,0xFFDCEAFF};
    private static final int[] DARK={0,0xFF52332F,0xFF51452B,0xFF304C38,0xFF304052};
    private static final String[] COLORS={"Bawaan","Merah","Kuning","Hijau","Biru"};
    @Override protected boolean supportsBottomSwipe() { return !editing; }
    private int dp(int n) { return ui.dp(this,n); }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(font); return t; }
    private Button button(String label,Runnable click) {
        Button b=new Button(this); b.setAllCaps(false); b.setText(label); b.setTypeface(font); b.setTextColor(ui.ink);
        b.setMinHeight(dp(48)); b.setBackground(ui.surface(this,ui.control)); b.setOnClickListener(v -> click.run()); return b;
    }
    private ImageButton icon(int resource,String description,Runnable click) {
        ImageButton b=new ImageButton(this); b.setImageResource(resource); b.setColorFilter(ui.ink); b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        b.setPadding(dp(12),dp(12),dp(12),dp(12)); b.setBackground(ui.surface(this,ui.control)); b.setContentDescription(description); b.setTooltipText(description); b.setOnClickListener(v->click.run()); return b;
    }
    private void addIcon(LinearLayout row,int resource,String description,Runnable click) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(48),dp(48)); p.setMargins(0,0,dp(6),0); row.addView(icon(resource,description,click),p);
    }
    private void add(View view) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(4)); content.addView(view,p); }
    private TextWatcher watcher(Runnable run) { return new TextWatcher() { public void beforeTextChanged(CharSequence s,int a,int c,int n) {} public void onTextChanged(CharSequence s,int a,int before,int count) {} public void afterTextChanged(Editable e) { run.run(); } }; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); ui=new AppUi(this); ui.window(this); font=Typeface.createFromAsset(getAssets(),"fonts/Doto.ttf"); store=new NoteStore(this);
        layout=getPreferences(MODE_PRIVATE).getInt("layout",getPreferences(MODE_PRIVATE).getBoolean("grid",false)?1:0); if(layout<0 || layout>2) layout=0;
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(ui.canvas);
        page.setOnApplyWindowInsetsListener((v,insets) -> { page.setPadding(dp(20),dp(16)+insets.getSystemWindowInsetTop(),dp(20),dp(16)+insets.getSystemWindowInsetBottom()); return insets; });
        LinearLayout header=ui.header(this,font,"Catatan"); header.getChildAt(0).setOnClickListener(v -> onBackPressed()); page.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,0,0,dp(80)); scroll.addView(content);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(page);
        if(state!=null) { section=state.getInt("section",NoteStore.ACTIVE); query=state.getString("query",""); exportTitle=state.getString("exportTitle",""); exportBody=state.getString("exportBody",""); }
        if(state!=null && state.getBoolean("editing")) {
            editId=state.getLong("id"); lastTitle=state.getString("lastTitle",""); lastBody=state.getString("lastBody",""); lastTag=state.getString("lastTag",""); draftTag=state.getString("draftTag",lastTag); editing=true;
            editor(state.getString("title",""),state.getString("body",""));
            if(!lastTitle.equals(title.getText().toString()) || !lastBody.equals(body.getText().toString()) || !lastTag.equals(draftTag)) queueSave();
        } else {
            showList(); long requested=getIntent().getLongExtra("note_id",0);
            if(requested>0) try { for(NoteStore.Note note:store.list()) if(note.id==requested && note.state!=NoteStore.TRASH) { openEditor(note); break; } } catch(SQLiteException e) { message("Tidak dapat membuka catatan."); }
        }
    }
    @Override protected void onResume() { super.onResume(); AppUi current=new AppUi(this); if(ui!=null && (ui.dark!=current.dark || !ui.choice.equals(current.choice))) recreate(); }
    @Override protected void onPause() { if(editing) saveDraft(false); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        if(editing) saveDraft(false);
        super.onSaveInstanceState(out); out.putBoolean("editing",editing); out.putInt("section",section); out.putString("query",query); out.putString("exportTitle",exportTitle); out.putString("exportBody",exportBody);
        if(editing && title!=null && body!=null) { out.putLong("id",editId); out.putString("lastTitle",lastTitle); out.putString("lastBody",lastBody); out.putString("lastTag",lastTag); out.putString("draftTag",draftTag); out.putString("title",title.getText().toString()); out.putString("body",body.getText().toString()); }
    }
    @Override protected void onDestroy() { handler.removeCallbacks(autoSave); if(store!=null) store.close(); super.onDestroy(); }
    private void message(String msg) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show(); }
    private void hideKeyboard() { View focused=getCurrentFocus(); InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); if(focused!=null && imm!=null) imm.hideSoftInputFromWindow(focused.getWindowToken(),0); }
    private void showList() {
        handler.removeCallbacks(autoSave); editing=false; title=null; body=null; content.removeAllViews();
        add(text("Tersimpan lokal; catatan terhapus jika aplikasi dihapus. Tidak tersinkronisasi.",12,ui.muted));
        LinearLayout controls=new LinearLayout(this); controls.setGravity(Gravity.CENTER_VERTICAL);
        String[] names={"Catatan","Arsip","Sampah"}; int[] locations={R.drawable.ic_notes_notes,R.drawable.ic_notes_archive,R.drawable.ic_notes_trash};
        addIcon(controls,locations[section],"Lokasi: "+names[section],() -> ui.dialog(this).setTitle("Tampilkan")
            .setItems(names,(d,w)->{section=w; showList();}).show());
        int[] layouts={R.drawable.ic_notes_list,R.drawable.ic_notes_grid,R.drawable.ic_notes_staggered};
        addIcon(controls,layouts[layout],"Tampilan: "+new String[]{"daftar","kotak","staggered"}[layout],() -> { layout=(layout+1)%3; getPreferences(MODE_PRIVATE).edit().putInt("layout",layout).apply(); showList(); });
        if(section==NoteStore.ACTIVE) addIcon(controls,R.drawable.ic_calendar_add,"Catatan baru",() -> openEditor(null));
        addIcon(controls,R.drawable.ic_notes_backup,"Cadangkan atau pulihkan",this::backupMenu); add(controls);
        search=field("Cari judul, isi, atau tag",200,false); search.setContentDescription("Cari catatan"); search.setText(query); add(search);
        search.addTextChangedListener(watcher(() -> { query=search.getText().toString(); renderCards(); }));
        cards=new LinearLayout(this); cards.setOrientation(LinearLayout.VERTICAL); add(cards); renderCards();
    }
    private void renderCards() {
        if(cards==null) return; cards.removeAllViews();
        final ArrayList<NoteStore.Note> notes;
        try { notes=store.list(); } catch(SQLiteException e) { cards.addView(text("Catatan tidak dapat dibaca. Data lama tidak dihapus.",14,ui.ink)); return; }
        ArrayList<NoteStore.Note> visible=new ArrayList<>();
        for(NoteStore.Note n:notes) if(n.state==section && NoteSearch.matches(n.title,n.body,n.tag,query)) visible.add(n);
        if(visible.isEmpty()) { cards.addView(text("Tidak ada catatan.",14,ui.muted)); return; }
        if(layout==0) for(NoteStore.Note note:visible) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(4)); cards.addView(card(note),p); }
        else if(layout==2) {
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout left=new LinearLayout(this),right=new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL); right.setOrientation(LinearLayout.VERTICAL);
            row.addView(left,new LinearLayout.LayoutParams(0,-2,1)); row.addView(right,new LinearLayout.LayoutParams(0,-2,1));
            for(int i=0;i<visible.size();i++) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(dp(2),dp(4),dp(2),dp(4)); (i%2==0?left:right).addView(card(visible.get(i)),p); }
            cards.addView(row);
        } else {
            for(int i=0;i<visible.size();i+=2) {
                LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
                for(int j=i;j<Math.min(i+2,visible.size());j++) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(dp(2),dp(4),dp(2),dp(4)); row.addView(card(visible.get(j)),p); }
                if(i+1>=visible.size()) row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
                cards.addView(row);
            }
        }
    }
    private View card(NoteStore.Note note) {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(12),dp(10),dp(12),dp(10));
        int index=Math.max(0,Math.min(note.color,LIGHT.length-1)); int color=index==0?ui.card:(ui.dark?DARK[index]:LIGHT[index]); card.setBackground(ui.surface(this,color));
        TextView heading=text((note.pinned!=0?"📌 ":"")+NoteText.displayTitle(note.title),16,ui.ink); heading.setTypeface(font,Typeface.BOLD); heading.setMaxLines(1); heading.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(heading);
        if(!note.body.trim().isEmpty()) { TextView summary=text(note.body,13,ui.muted); summary.setMaxLines(2); summary.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(summary); }
        if(!note.tag.isEmpty()) card.addView(text("#"+note.tag,12,ui.muted));
        card.setFocusable(true); card.setContentDescription("Catatan " + NoteText.displayTitle(note.title) + ". Ketuk untuk pilihan");
        card.setOnClickListener(v -> noteMenu(note)); return card;
    }
    private void noteMenu(NoteStore.Note note) {
        if(note.state==NoteStore.TRASH) {
            ui.dialog(this).setTitle(NoteText.displayTitle(note.title)).setItems(new String[]{"Pulihkan","Hapus permanen"},(d,w)->{
                if(w==0) mutate(() -> store.restore(note.id));
                else ui.dialog(this).setTitle("Hapus permanen?").setMessage("Catatan tidak dapat dipulihkan.").setNegativeButton("Batal",null).setPositiveButton("Hapus",(x,y)->mutate(() -> store.deletePermanently(note.id))).show();
            }).show(); return;
        }
        String[] actions={"Buka / edit",note.pinned!=0?"Lepas sematan":"Sematkan","Ubah label","Ubah warna",note.state==NoteStore.ARCHIVE?"Kembalikan dari arsip":"Arsipkan","Pindah ke sampah","Bagikan teks","Ekspor TXT","Ekspor PDF","Checklist","Pengingat"};
        ui.dialog(this).setTitle(NoteText.displayTitle(note.title)).setItems(actions,(d,w)->{
            if(w==0) openEditor(note);
            else if(w==1) mutate(() -> store.metadata(note.id,note.pinned==0?1:0,note.tag,note.color));
            else if(w==2) editTag(note);
            else if(w==3) ui.dialog(this).setTitle("Warna catatan").setItems(COLORS,(x,y)->mutate(() -> store.metadata(note.id,note.pinned,note.tag,y))).show();
            else if(w==4) mutate(() -> store.archive(note.id,note.state!=NoteStore.ARCHIVE));
            else if(w==5) ui.dialog(this).setTitle("Pindah ke sampah?").setMessage("Catatan dapat dipulihkan dari Sampah. Pengingat dibatalkan.").setNegativeButton("Batal",null).setPositiveButton("Pindah",(x,y)->mutate(() -> { boolean ok=store.trash(note.id); if(ok) NoteReminders.cancel(this,note.id); return ok; })).show();
            else if(w==6) { Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,NoteExport.text(note.title,note.body)); try { startActivity(Intent.createChooser(share,"Bagikan catatan")); } catch(RuntimeException e) { message("Tidak ada aplikasi berbagi."); } }
            else if(w==7 || w==8) export(note,w==8);
            else if(w==9) checklist(note);
            else if(w==10) reminderMenu(note);
        }).show();
    }
    private void reminderMenu(NoteStore.Note note) {
        long existing=NoteReminders.prefs(this).getLong(Long.toString(note.id),0);
        String message=existing>0?"Dijadwalkan: "+android.text.format.DateFormat.getDateFormat(this).format(new java.util.Date(existing))+" "+android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date(existing)):"Belum ada pengingat.";
        if(!NoteReminders.exact(this)) message+=" Izin alarm tepat belum aktif; pengingat dapat terlambat.";
        android.app.AlertDialog.Builder dialog=ui.dialog(this).setTitle("Pengingat catatan").setMessage(message).setNegativeButton("Batal",null)
            .setPositiveButton("Atur waktu",(d,w)->chooseReminderTime(note));
        if(existing>0) dialog.setNeutralButton("Hapus pengingat",(d,w)->{NoteReminders.cancel(this,note.id); message("Pengingat dibatalkan.");});
        dialog.show();
    }
    private void chooseReminderTime(NoteStore.Note note) {
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},605); message("Izinkan notifikasi, lalu atur pengingat lagi."); return;
        }
        android.app.NotificationManager notifications=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        android.app.NotificationChannel channel=notifications.getNotificationChannel("note_reminders");
        if(!notifications.areNotificationsEnabled() || (channel!=null && channel.getImportance()==android.app.NotificationManager.IMPORTANCE_NONE)) { message("Aktifkan notifikasi pengingat catatan di setelan perangkat."); return; }
        java.util.Calendar cal=java.util.Calendar.getInstance(); cal.add(java.util.Calendar.MINUTE,5);
        new android.app.DatePickerDialog(this,(view,year,month,day)->{
            cal.set(java.util.Calendar.YEAR,year); cal.set(java.util.Calendar.MONTH,month); cal.set(java.util.Calendar.DAY_OF_MONTH,day);
            new android.app.TimePickerDialog(this,(clock,hour,minute)->{
                cal.set(java.util.Calendar.HOUR_OF_DAY,hour); cal.set(java.util.Calendar.MINUTE,minute); cal.set(java.util.Calendar.SECOND,0); cal.set(java.util.Calendar.MILLISECOND,0);
                if(cal.getTimeInMillis()<=System.currentTimeMillis()) { message("Pilih waktu yang akan datang."); return; }
                if(NoteReminders.set(this,note.id,cal.getTimeInMillis())) message(NoteReminders.exact(this)?"Pengingat dijadwalkan.":"Pengingat dijadwalkan; dapat terlambat tanpa izin alarm tepat.");
                else message("Pengingat tidak dapat dijadwalkan.");
            },cal.get(java.util.Calendar.HOUR_OF_DAY),cal.get(java.util.Calendar.MINUTE),android.text.format.DateFormat.is24HourFormat(this)).show();
        },cal.get(java.util.Calendar.YEAR),cal.get(java.util.Calendar.MONTH),cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }
    private void backupMenu() {
        ui.dialog(this).setTitle("Cadangan manual").setMessage("Berkas tidak terenkripsi. Pilih Google Drive pada pemilih dokumen jika tersedia. Bukan sinkronisasi otomatis.")
            .setNegativeButton("Batal",null).setNeutralButton("Pulihkan salinan",(d,w)->ui.dialog(this).setTitle("Pulihkan sebagai salinan?")
                .setMessage("Semua catatan dalam cadangan ditambahkan sebagai salinan; catatan yang sudah ada tidak ditimpa. Impor ulang menghasilkan duplikat.")
                .setNegativeButton("Batal",null).setPositiveButton("Pilih berkas",(x,y)->chooseBackup(false)).show())
            .setPositiveButton("Buat cadangan",(d,w)->chooseBackup(true)).show();
    }
    private void chooseBackup(boolean write) {
        Intent i=new Intent(write?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
        if(write) i.putExtra(Intent.EXTRA_TITLE,"GT30-catatan-backup.json");
        try { startActivityForResult(i,write?BACKUP_WRITE:BACKUP_READ); } catch(RuntimeException e) { message("Pemilih dokumen tidak tersedia."); }
    }
    private void processBackup(android.net.Uri uri,boolean write) {
        final Context context=getApplicationContext(); message(write?"Membuat cadangan…":"Memulihkan cadangan…");
        new Thread(() -> {
            String result;
            try(NoteStore db=new NoteStore(context)) {
                if(write) {
                    byte[] data=NoteBackup.encode(db.list());
                    try(java.io.OutputStream out=context.getContentResolver().openOutputStream(uri,"w")) {
                        if(out==null) throw new java.io.IOException("Penyimpanan tidak tersedia"); out.write(data); out.flush();
                    }
                    result="Cadangan dibuat. Pastikan unggahan penyedia selesai.";
                } else {
                    ArrayList<NoteStore.Note> copies;
                    try(java.io.InputStream in=context.getContentResolver().openInputStream(uri)) {
                        if(in==null) throw new java.io.IOException("Berkas tidak tersedia"); copies=NoteBackup.decode(NoteBackup.read(in));
                    }
                    db.importCopies(copies); result=copies.size()+" salinan catatan dipulihkan.";
                }
            } catch(Exception ex) { result=write?"Cadangan gagal; periksa lokasi dan koneksi.":"Pemulihan gagal; tidak ada catatan yang diimpor."; }
            final String done=result;
            runOnUiThread(() -> { if(!isFinishing() && !isDestroyed()) { message(done); if(!editing) renderCards(); } });
        },"notes-backup").start();
    }
    private void checklist(NoteStore.Note note) {
        final ArrayList<NoteChecklist.Item> items=NoteChecklist.items(note.body);
        if(items.isEmpty()) { message("Belum ada item checklist. Tambahkan dari editor."); return; }
        String[] labels=new String[items.size()]; boolean[] checked=new boolean[items.size()];
        for(int i=0;i<items.size();i++) { labels[i]=items.get(i).text; checked[i]=items.get(i).checked; }
        final String[] current={note.body};
        ui.dialog(this).setTitle("Checklist").setMultiChoiceItems(labels,checked,(dialog,which,isChecked)->{
            String next=NoteChecklist.toggle(current[0],items.get(which).line,isChecked);
            try {
                if(store.save(note.id,note.title,next)<=0) throw new SQLiteException("Note missing");
                current[0]=next;
            } catch(SQLiteException e) { ((android.app.AlertDialog)dialog).getListView().setItemChecked(which,!isChecked); message("Checklist gagal disimpan."); }
        }).setPositiveButton("Selesai",(d,w)->renderCards()).setOnCancelListener(d->renderCards()).show();
    }
    private void export(NoteStore.Note note,boolean pdf) {
        exportTitle=note.title; exportBody=note.body;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(pdf?"application/pdf":"text/plain");
        intent.putExtra(Intent.EXTRA_TITLE,pdf?"catatan.pdf":"catatan.txt");
        try { startActivityForResult(intent,pdf?EXPORT_PDF:EXPORT_TXT); } catch(RuntimeException e) { message("Pemilih dokumen tidak tersedia."); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null) return;
        if(request==BACKUP_WRITE || request==BACKUP_READ) { processBackup(data.getData(),request==BACKUP_WRITE); return; }
        if(request!=EXPORT_TXT && request!=EXPORT_PDF) return;
        try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData(),"w")) {
            if(out==null) throw new java.io.IOException("No output stream");
            if(request==EXPORT_PDF) NoteExport.writePdf(out,exportTitle,exportBody); else NoteExport.writeText(out,exportTitle,exportBody);
            message("Catatan diekspor.");
        } catch(java.io.IOException | RuntimeException e) { message("Ekspor gagal. Periksa lokasi penyimpanan."); }
    }
    private interface Change { boolean run(); }
    private void mutate(Change action) { try { if(!action.run()) message("Catatan tidak ditemukan."); } catch(SQLiteException e) { message("Tidak dapat mengubah catatan."); } renderCards(); }
    private void editTag(NoteStore.Note note) {
        EditText input=field("Label",60,false); input.setText(note.tag);
        ui.dialog(this).setTitle("Label catatan").setView(input).setNegativeButton("Batal",null)
          .setPositiveButton("Simpan",(d,w)->mutate(() -> store.metadata(note.id,note.pinned,input.getText().toString().trim(),note.color))).show();
    }
    private void openEditor(NoteStore.Note note) { editId=note==null?0:note.id; lastTitle=note==null?"":note.title; lastBody=note==null?"":note.body; lastTag=note==null?"":note.tag; draftTag=lastTag; editing=true; editor(lastTitle,lastBody); }
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
        saveStatus=text("Tersimpan otomatis di perangkat",12,ui.muted); add(saveStatus);
        title.addTextChangedListener(watcher(this::queueSave)); body.addTextChangedListener(watcher(this::queueSave));
        LinearLayout tools=new LinearLayout(this); tools.setGravity(Gravity.CENTER_VERTICAL);
        addIcon(tools,R.drawable.ic_notes_tag,"Pilih atau buat tag",this::chooseTag);
        tagValue=text("",13,ui.muted); tools.addView(tagValue,new LinearLayout.LayoutParams(0,-2,1)); updateTagValue();
        addIcon(tools,R.drawable.ic_notes_checklist,"Tambah item checklist",() -> {
            EditText item=field("Nama item",200,false);
            ui.dialog(this).setTitle("Tambah item checklist").setView(item).setNegativeButton("Batal",null).setPositiveButton("Tambah",(d,w)->{
                String value=item.getText().toString().trim(); if(value.isEmpty()) return;
                String current=body.getText().toString(), addition=(current.isEmpty() || current.endsWith("\n")?"":"\n")+"☐ "+value;
                if(current.length()+addition.length()>NoteText.BODY_LIMIT) { message("Catatan terlalu panjang."); return; }
                body.append(addition);
            }).show();
        });
        addIcon(tools,R.drawable.ic_notes_done,"Selesai",this::finishEditor); add(tools);
    }
    private void updateTagValue() { if(tagValue!=null) { tagValue.setText(draftTag.isEmpty()?"":"#"+draftTag); tagValue.setContentDescription(draftTag.isEmpty()?"Tidak ada tag":"Tag: "+draftTag); } }
    private void chooseTag() {
        java.util.TreeSet<String> tags=new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try { for(NoteStore.Note n:store.list()) if(!n.tag.isEmpty()) tags.add(n.tag); }
        catch(SQLiteException e) { message("Daftar tag tidak dapat dibaca."); return; }
        if(!draftTag.isEmpty()) tags.add(draftTag);
        ArrayList<String> values=new ArrayList<>(); values.add("+ Buat tag baru"); values.add("Tanpa tag"); values.addAll(tags);
        ui.dialog(this).setTitle("Pilih tag").setItems(values.toArray(new String[0]),(d,which)->{
            if(which==0) {
                EditText input=field("Nama tag",60,false);
                ui.dialog(this).setTitle("Buat tag").setView(input).setNegativeButton("Batal",null).setPositiveButton("Pilih",(x,y)->{
                    String value=input.getText().toString().trim(); if(value.isEmpty()) { message("Nama tag tidak boleh kosong."); return; }
                    draftTag=value; updateTagValue(); queueSave();
                }).show();
            } else { draftTag=which==1?"":values.get(which); updateTagValue(); queueSave(); }
        }).show();
    }
    private void queueSave() { handler.removeCallbacks(autoSave); if(saveStatus!=null) saveStatus.setText("Menyimpan…"); handler.postDelayed(autoSave,600); }
    private boolean saveDraft(boolean report) {
        handler.removeCallbacks(autoSave); if(!editing || title==null || body==null) return true;
        String t=title.getText().toString(), b=body.getText().toString();
        if(t.equals(lastTitle) && b.equals(lastBody) && draftTag.equals(lastTag)) { if(saveStatus!=null) saveStatus.setText("Tersimpan di perangkat"); return true; }
        if(editId==0 && !NoteText.valid(t,b)) { if(saveStatus!=null) saveStatus.setText("Catatan kosong belum disimpan"); return t.trim().isEmpty() && b.trim().isEmpty(); }
        try {
            long id=store.save(editId,t,b,draftTag); if(id<=0) { if(saveStatus!=null) saveStatus.setText("Gagal menyimpan; teks masih di editor"); if(report) message("Tidak dapat menyimpan catatan."); return false; }
            editId=id; lastTitle=t; lastBody=b; lastTag=draftTag; if(saveStatus!=null) saveStatus.setText("Tersimpan di perangkat"); return true;
        } catch(SQLiteException e) { if(saveStatus!=null) saveStatus.setText("Gagal menyimpan; teks masih di editor"); if(report) message("Tidak dapat menyimpan catatan."); return false; }
    }
    private void finishEditor() { if(saveDraft(true)) { hideKeyboard(); showList(); } }
    @Override public void onBackPressed() { if(editing) finishEditor(); else super.onBackPressed(); }
}
