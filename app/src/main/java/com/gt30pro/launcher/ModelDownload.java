package com.gt30pro.launcher;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** Downloads only the pinned official Qwen GGUF, checks length and SHA-256 before publication. */
final class ModelDownload {
    static final long BYTES=639446688L;
    static final String SHA256="9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031";
    static final String URL_STRING="https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/1eaf4d9657fe65ad10a51eab76a8db5b363bddaa/Qwen3-0.6B-Q8_0.gguf";
    private static volatile boolean active, cancel;
    private static volatile long received;
    private static volatile String message="";
    private static File folder(Context c) { return new File(c.getFilesDir(),"ai"); }
    private static File model(Context c) { return new File(folder(c),"qwen3-0.6b-q8_0.gguf"); }
    static boolean available(Context c) { return model(c).isFile() && model(c).length()==BYTES; }
    static boolean active() { return active; }
    static String status(Context c) {
        if(active) return "Mengunduh model: " + (received*100/BYTES) + "% · biarkan aplikasi terbuka";
        if(!message.isEmpty()) return message;
        return available(c) ? "Model tersimpan. Runtime AI belum tersedia; model belum dapat digunakan." : "Model belum diunduh. Runtime AI belum tersedia.";
    }
    static synchronized boolean start(Context context) {
        Context app=context.getApplicationContext();
        if(active) return false;
        if(available(app)) { message="Model sudah tersimpan. Runtime AI belum tersedia; model belum dapat digunakan."; return false; }
        File dir=folder(app);
        if(dir.isDirectory()) { File stale=new File(dir,"qwen.part"); if(stale.exists() && !stale.delete()) { message="File sementara tidak dapat dihapus."; return false; } }
        if((!dir.isDirectory() && !dir.mkdirs()) || dir.getUsableSpace()<BYTES+100_000_000L) { message="Ruang kosong tidak cukup; diperlukan sekitar 740 MB."; return false; }
        active=true; cancel=false; received=0; message="";
        new Thread(() -> download(app),"model-download").start();
        return true;
    }
    static void cancel() { cancel=true; }
    static synchronized boolean delete(Context context) {
        if(active) return false;
        File file=model(context); boolean ok=!file.exists() || file.delete();
        message=ok?"Model dihapus.":"Model tidak dapat dihapus."; return ok;
    }
    private static HttpURLConnection connect(URL initial) throws Exception {
        URL url=initial;
        for(int i=0;i<6;i++) {
            if(!"https".equalsIgnoreCase(url.getProtocol())) throw new java.io.IOException("HTTPS diperlukan");
            HttpURLConnection c=(HttpURLConnection)url.openConnection();
            c.setInstanceFollowRedirects(false); c.setConnectTimeout(20000); c.setReadTimeout(20000);
            int status=c.getResponseCode();
            if(status>=300 && status<400) {
                String location=c.getHeaderField("Location"); c.disconnect();
                if(location==null) throw new java.io.IOException("redirect tanpa tujuan");
                url=new URL(url,location); continue;
            }
            if(status!=200) { c.disconnect(); throw new java.io.IOException("HTTP "+status); }
            return c;
        }
        throw new java.io.IOException("terlalu banyak redirect");
    }
    private static void download(Context app) {
        File temp=new File(folder(app),"qwen.part"); HttpURLConnection c=null;
        try {
            if(temp.exists() && !temp.delete()) throw new java.io.IOException("file sementara tidak dapat dihapus");
            c=connect(new URL(URL_STRING));
            long length=c.getContentLengthLong();
            if(length>0 && length!=BYTES) throw new java.io.IOException("ukuran model tidak sesuai");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream in=c.getInputStream(); FileOutputStream out=new FileOutputStream(temp)) {
                byte[] buf=new byte[65536]; int n;
                while((n=in.read(buf))!=-1) {
                    if(cancel) throw new InterruptedException();
                    received+=n; if(received>BYTES) throw new java.io.IOException("file terlalu besar");
                    digest.update(buf,0,n); out.write(buf,0,n);
                }
                out.getFD().sync();
            }
            if(cancel) throw new InterruptedException();
            if(!ModelIntegrity.matches(BYTES,received,SHA256,digest.digest())) throw new java.io.IOException("verifikasi SHA-256 gagal");
            if(!temp.renameTo(model(app))) throw new java.io.IOException("model tidak dapat disimpan");
            message="Unduhan terverifikasi. Runtime AI belum tersedia; model belum dapat digunakan.";
        } catch (Exception e) { message=cancel?"Unduhan dibatalkan.":"Unduhan gagal: " + (e.getMessage()==null?"coba lagi":e.getMessage()); }
        finally { if(c!=null)c.disconnect(); if(temp.exists())temp.delete(); active=false; }
    }
    private ModelDownload() { }
}
