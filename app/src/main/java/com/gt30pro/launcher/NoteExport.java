package com.gt30pro.launcher;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Plain text and paginated PDF export, to an explicitly user-selected document. */
final class NoteExport {
    static String text(String title,String body) { return (title.trim().isEmpty()?"":title+"\n\n")+body; }
    static void writeText(OutputStream out,String title,String body) throws IOException { out.write(text(title,body).getBytes(StandardCharsets.UTF_8)); }
    static void writePdf(OutputStream out,String title,String body) throws IOException {
        PdfDocument pdf=new PdfDocument();
        try {
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(Color.BLACK); paint.setTextSize(12f); paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
            String[] lines=text(title,body).split("\\n",-1); int pageNo=1; PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create()); Canvas canvas=page.getCanvas(); float y=50f;
            for(String line:lines) {
                int offset=0;
                do {
                    if(y>800f) { pdf.finishPage(page); page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create()); canvas=page.getCanvas(); y=50f; }
                    int count=line.isEmpty()?0:paint.breakText(line,offset,line.length(),true,515f,null);
                    if(!line.isEmpty() && count<=0) count=1;
                    if(count>0) canvas.drawText(line,offset,offset+count,40f,y,paint);
                    y+=18f; offset+=count;
                } while(offset<line.length());
            }
            pdf.finishPage(page); pdf.writeTo(out);
        } finally { pdf.close(); }
    }
    private NoteExport() { }
}
