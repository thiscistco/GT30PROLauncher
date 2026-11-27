package com.gt30pro.launcher;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Weekly teaching schedule, stored only on this device. */
public class TimeTableActivity extends SwipeBackActivity {
    private static final String[] DAYS={"Senin","Selasa","Rabu","Kamis","Jumat"};
    private static final String[] SHORT_DAYS={"Sen","Sel","Rab","Kam","Jum"};
    private AppUi ui;
    private Typeface font;
    private JSONArray rows;
    private LinearLayout table, page;
    private ScrollView vertical;
    private int viewportHeight;
    private boolean storageSafe=true;
    @Override protected boolean supportsBottomSwipe() { return true; }
    private int dp(int n) { return ui.dp(this,n); }
    private TextView text(String value,int size,int color) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(font); t.setFontVariationSettings("'wght' 800"); return t;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); ui=new AppUi(this); ui.window(this); font=Typeface.createFromAsset(getAssets(),"fonts/Doto.ttf");
        android.content.SharedPreferences prefs=getSharedPreferences("timetable",Context.MODE_PRIVATE);
        int skipped=0;
        try {
            rows=new JSONArray(prefs.getString("rows","[]"));
            if(!prefs.getBoolean("default_rows_v2",false)) {
                TimeTableData.SeedResult seeded=TimeTableData.seed(rows); rows=seeded.rows; skipped=seeded.skipped;
                prefs.edit().putString("rows",rows.toString()).putBoolean("default_rows_v2",true).apply();
            }
            rows=TimeTableData.sorted(rows);
        } catch(Exception e) { rows=new JSONArray(); storageSafe=false; }
        if(!storageSafe) Toast.makeText(this,"Data jadwal tidak dapat dibaca. Data lama tidak diubah.",Toast.LENGTH_LONG).show();
        else if(skipped>0) Toast.makeText(this,"Sebagian baris bawaan bertabrakan dengan jadwal lama dan tidak ditambahkan. Jadwal lama tetap tersimpan.",Toast.LENGTH_LONG).show();
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(ui.canvas);
        page.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(16),dp(12)+insets.getSystemWindowInsetTop(),dp(16),dp(12)+insets.getSystemWindowInsetBottom());return insets;});
        setContentView(page); page.addView(ui.header(this,font,"Jadwal Mengajar"));
        LinearLayout title=new LinearLayout(this); title.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=text("JADWAL MENGAJAR",16,ui.ink); title.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton add=new ImageButton(this); add.setImageResource(R.drawable.ic_calendar_add); add.setColorFilter(ui.ink); add.setScaleType(ImageView.ScaleType.FIT_CENTER); add.setPadding(dp(14),dp(14),dp(14),dp(14)); add.setBackground(ui.surface(this,ui.control)); add.setContentDescription("Tambah jam"); add.setTooltipText("Tambah jam"); add.setOnClickListener(v->editTime(-1));
        title.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48))); page.addView(title);
        TextView hint=text("Ketuk jam atau sel untuk mengubah.",10,ui.muted); hint.setPadding(0,0,0,dp(4)); page.addView(hint);
        vertical=new ScrollView(this); vertical.setFillViewport(true);
        table=new LinearLayout(this); table.setOrientation(LinearLayout.VERTICAL); vertical.addView(table,new ScrollView.LayoutParams(-1,-2));
        page.addView(vertical,new LinearLayout.LayoutParams(-1,0,1));
        vertical.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,oright,ob)->{ int height=b-t; if(height!=viewportHeight){viewportHeight=height;render();} });
        render();
    }
    @Override protected void onResume() { super.onResume(); if(ui!=null) { AppUi now=new AppUi(this); if(now.dark!=ui.dark || !now.choice.equals(ui.choice)) recreate(); } }
    private void persist() { getSharedPreferences("timetable",Context.MODE_PRIVATE).edit().putString("rows",rows.toString()).apply(); }
    private LinearLayout row() { LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    private void addBox(LinearLayout row,View child,int width,int height) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width==0?0:dp(width),height,width==0?1:0); p.setMargins(dp(1),dp(1),dp(1),dp(1)); row.addView(child,p);
    }
    private TextView box(String label,int size,int color,int bg) {
        TextView t=text(label,size,color); t.setGravity(Gravity.CENTER); t.setPadding(dp(2),dp(1),dp(2),dp(1)); t.setBackground(ui.surface(this,bg)); return t;
    }
    private void render() {
        table.removeAllViews(); LinearLayout head=row();
        addBox(head,box("JAM",10,ui.ink,ui.control),62,dp(32));
        for(int d=0;d<DAYS.length;d++) { TextView name=box(SHORT_DAYS[d],10,ui.ink,ui.control); name.setContentDescription(DAYS[d]); addBox(head,name,0,dp(32)); }
        table.addView(head);
        int rowHeight=Math.max(dp(38),rows.length()==0?dp(38):(viewportHeight-dp(34)-dp(2)*rows.length())/rows.length());
        for(int i=0;i<rows.length();i++) {
            final int index=i; JSONObject data=rows.optJSONObject(i); if(data==null) continue;
            String start=data.optString("start"), end=data.optString("end"), label=data.optString("label","");
            LinearLayout line=row(); TextView time=box(start+"\n"+end,10,ui.ink,ui.control);
            time.setContentDescription("Ubah jam " + start + " sampai " + end); time.setOnClickListener(v->editTime(index)); addBox(line,time,62,rowHeight);
            if(!label.isEmpty()) {
                TextView breakLabel=box(label,11,ui.ink,ui.event); breakLabel.setSingleLine(true); breakLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
                breakLabel.setContentDescription(label+", "+start+" sampai "+end+". Ubah jam dan label"); breakLabel.setOnClickListener(v->editTime(index)); addBox(line,breakLabel,0,rowHeight);
            } else {
                JSONArray cells=data.optJSONArray("cells");
                for(int d=0;d<DAYS.length;d++) {
                    final int day=d; JSONObject cell=cells==null?null:cells.optJSONObject(d);
                    String subject=cell==null?"":cell.optString("subject",""); String room=cell==null?"":cell.optString("class","");
                    String value=subject.isEmpty()?room:room.isEmpty()?subject:subject+"\n"+room;
                    TextView entry=box(value.isEmpty()?"+":value,value.isEmpty()?16:9,ui.ink,value.isEmpty()?ui.card:ui.event);
                    entry.setMaxLines(2); entry.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    entry.setContentDescription(DAYS[d]+", "+start+" sampai "+end+", "+(value.isEmpty()?"kosong":value)+". Ubah jadwal");
                    entry.setOnClickListener(v->editCell(index,day)); addBox(line,entry,0,rowHeight);
                }
            }
            table.addView(line);
        }
        if(rows.length()==0) { TextView empty=text("Belum ada jam. Ketuk + untuk menambahkan baris waktu.",13,ui.muted); empty.setPadding(dp(8),dp(18),0,0); table.addView(empty); }
    }
    private void editTime(int index) {
        if(!storageSafe) return;
        JSONObject current=index<0?null:rows.optJSONObject(index); if(index>=0 && current==null)return;
        final String[] times={current==null?"07:00":current.optString("start"),current==null?"08:00":current.optString("end")};
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),dp(8),dp(20),0);
        TextView start=box("Mulai  "+times[0],16,ui.ink,ui.control), end=box("Selesai  "+times[1],16,ui.ink,ui.control);
        form.addView(start,new LinearLayout.LayoutParams(-1,dp(56))); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(56)); p.topMargin=dp(8); form.addView(end,p);
        EditText label=field("Nama baris (opsional)",current==null?"":current.optString("label",""));
        label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)}); form.addView(label);
        TextView labelHint=text("Isi nama untuk baris bersama; kosongkan untuk mengisi tiap hari.",11,ui.muted); form.addView(labelHint);
        start.setOnClickListener(v->pickTime(times,0,start,"Mulai")); end.setOnClickListener(v->pickTime(times,1,end,"Selesai"));
        AlertDialog.Builder builder=ui.dialog(this).setTitle(index<0?"Tambah jam":"Ubah jam").setView(form).setNegativeButton("Batal",null).setPositiveButton("Simpan",null);
        if(index>=0) builder.setNeutralButton("Hapus",(dialog,w)->confirmDelete(index));
        AlertDialog dialog=builder.show(); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(!TimeTableData.validRange(times[0],times[1],rows,index)) { Toast.makeText(this,"Jam selesai harus setelah mulai dan tidak boleh bertabrakan dengan baris lain.",Toast.LENGTH_LONG).show(); return; }
            try { JSONObject item=current==null?new JSONObject():current; item.put("start",times[0]); item.put("end",times[1]); item.put("label",label.getText().toString().trim()); if(current==null){item.put("cells",new JSONArray());rows.put(item);} rows=TimeTableData.sorted(rows); persist(); render(); dialog.dismiss(); }
            catch(Exception e){ Toast.makeText(this,"Jadwal tidak dapat disimpan.",Toast.LENGTH_SHORT).show(); }
        });
    }
    private void pickTime(String[] values,int i,TextView view,String label) {
        int value=TimeTableData.minutes(values[i]); if(value<0)value=0;
        new TimePickerDialog(this,(picker,h,m)->{values[i]=String.format(Locale.US,"%02d:%02d",h,m);view.setText(label+"  "+values[i]);},value/60,value%60,true).show();
    }
    private void confirmDelete(int index) {
        ui.dialog(this).setTitle("Hapus jam?").setMessage("Semua jadwal pada baris jam ini juga akan dihapus.").setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w)->{JSONArray kept=new JSONArray();for(int i=0;i<rows.length();i++)if(i!=index)kept.put(rows.opt(i));rows=kept;persist();render();}).show();
    }
    private EditText field(String hint,String value) {
        EditText f=new EditText(this); f.setSingleLine(true); f.setTextSize(16); f.setTypeface(font); f.setTextColor(ui.ink); f.setHintTextColor(ui.muted); f.setHint(hint); f.setText(value); f.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)}); return f;
    }
    private void editCell(int index,int day) {
        if(!storageSafe)return;
        JSONObject row=rows.optJSONObject(index); if(row==null)return; JSONArray cells=row.optJSONArray("cells"); JSONObject old=cells==null?null:cells.optJSONObject(day);
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),dp(8),dp(20),0);
        EditText subject=field("Pelajaran / kegiatan",old==null?"":old.optString("subject","")); EditText classroom=field("Kelas / catatan",old==null?"":old.optString("class",""));
        form.addView(subject);form.addView(classroom);
        ui.dialog(this).setTitle(DAYS[day]+" · "+row.optString("start")+"–"+row.optString("end")).setView(form).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
            try { JSONArray updated=row.optJSONArray("cells"); if(updated==null)updated=new JSONArray(); JSONObject cell=new JSONObject(); cell.put("subject",subject.getText().toString().trim());cell.put("class",classroom.getText().toString().trim());updated.put(day,cell);row.put("cells",updated);persist();render(); }
            catch(Exception e){ Toast.makeText(this,"Jadwal tidak dapat disimpan.",Toast.LENGTH_SHORT).show(); }
        }).show();
    }
}
