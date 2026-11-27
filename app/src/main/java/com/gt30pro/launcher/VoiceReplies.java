package com.gt30pro.launcher;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.widget.Toast;
import java.util.Set;

/** A bounded, local-voice reply; recognition resumes only after speech ends. */
final class VoiceReplies {
    private final Context context;
    private final Handler handler;
    private TextToSpeech tts;
    private boolean initializing, ready, busy;
    private int token, engineGeneration;
    private String text;
    private Runnable done;
    private Runnable timeout, privacyCheck;
    VoiceReplies(Context context,Handler handler) { this.context=context; this.handler=handler; }
    boolean isBusy() { return busy; }
    void speak(String answer,Runnable finished) {
        cancel(); busy=true; text=answer; done=finished;
        final int current=++token;
        timeout=() -> { if(busy && token==current) { stopOutput(); complete(current,true); } };
        handler.postDelayed(timeout,60000);
        privacyCheck=new Runnable() { public void run() {
            if (!busy || token!=current) return;
            if (!VoiceController.foregroundUnlocked(context)) { stopOutput(); complete(current,false); }
            else handler.postDelayed(this,250);
        }};
        handler.postDelayed(privacyCheck,250);
        if(ready) { play(current); return; }
        if(initializing) return;
        initializing=true;
        final int engineSession=++engineGeneration;
        try {
            tts=new TextToSpeech(context,status -> handler.post(() -> {
                if(engineSession!=engineGeneration) return;
                initializing=false;
                if(status!=TextToSpeech.SUCCESS || tts==null) {
                    if(tts!=null) { try { tts.shutdown(); } catch(RuntimeException ignored) { } tts=null; } ready=false;
                    if(busy) complete(token,true); return;
                }
                ready=true;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { }
                    @Override public void onDone(String id) { handler.post(() -> finishId(id,false)); }
                    @Override public void onError(String id) { handler.post(() -> finishId(id,true)); }
                    @Override public void onStop(String id,boolean interrupted) { handler.post(() -> finishId(id,interrupted)); }
                });
                if(busy) play(token);
            }));
        } catch (RuntimeException e) { initializing=false; engineGeneration++; complete(current,true); }
    }
    private void play(int current) {
        if(!busy || token!=current || tts==null) return;
        if(!VoiceController.foregroundUnlocked(context)) { complete(current,false); return; }
        try {
            // Select only voices the engine identifies as offline. The engine itself must be trusted.
            Set<Voice> voices=tts.getVoices(); Voice selected=null;
            if(voices!=null) for(Voice voice:voices) {
                String language=voice.getLocale().getLanguage();
                if(("id".equals(language) || "in".equals(language)) && !voice.isNetworkConnectionRequired()
                        && (voice.getFeatures()==null || !voice.getFeatures().contains("notInstalled"))) { selected=voice; break; }
            }
            if(selected==null || tts.setVoice(selected)!=TextToSpeech.SUCCESS) { complete(current,true); return; }
            int result=tts.speak(text,TextToSpeech.QUEUE_FLUSH,new Bundle(),String.valueOf(current));
            if(result!=TextToSpeech.SUCCESS) complete(current,true);
        } catch(RuntimeException e) { complete(current,true); }
    }
    private void finishId(String id,boolean failed) {
        try { complete(Integer.parseInt(id),failed); } catch (RuntimeException ignored) { }
    }
    private void complete(int current,boolean failed) {
        if(!busy || current!=token) return;
        if(timeout!=null) handler.removeCallbacks(timeout);
        if(privacyCheck!=null) handler.removeCallbacks(privacyCheck);
        busy=false; text=null; Runnable callback=done; done=null;
        if(failed && VoiceController.foregroundUnlocked(context))
            Toast.makeText(context,"Suara Indonesia offline tidak tersedia. Periksa mesin dan data suara di setelan Android.",Toast.LENGTH_LONG).show();
        if(callback!=null) callback.run();
    }
    private void stopOutput() { if(tts!=null) try { tts.stop(); } catch(RuntimeException ignored) { } }
    void cancel() {
        token++; busy=false; text=null; done=null;
        if(timeout!=null) handler.removeCallbacks(timeout);
        if(privacyCheck!=null) handler.removeCallbacks(privacyCheck);
        stopOutput();
    }
    void close() { cancel(); engineGeneration++; initializing=false; if(tts!=null) { try { tts.shutdown(); } catch(RuntimeException ignored) { } tts=null; } ready=false; }
}
