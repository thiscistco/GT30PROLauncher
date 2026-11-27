package com.gt30pro.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/** Best effort recognition: Android and the selected speech provider may suspend it. */
public final class VoiceService extends Service {
    static final String ACTION_START="com.gt30pro.launcher.VOICE_START", ACTION_STOP="com.gt30pro.launcher.VOICE_STOP";
    static volatile int mode=VoiceCommands.OFF;
    private static final String CHANNEL="voice_listening", ALERTS="voice_alerts";
    private static final int ID=731, ALERT_ID=732, OPEN_ID=733;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private int failures=0, generation=0;
    private Runnable watchdog;
    private long lastRmsAt;
    private final Runnable resetRms=VoiceController::resetWave;
    private NotificationManager notifications;
    private VoiceReplies replies;

    @Override public void onCreate() {
        super.onCreate(); notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        replies=new VoiceReplies(this,handler);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"Suara aktif",NotificationManager.IMPORTANCE_LOW));
        notifications.createNotificationChannel(new NotificationChannel(ALERTS,"Status dan perintah suara",NotificationManager.IMPORTANCE_DEFAULT));
    }
    private PendingIntent stopIntent() {
        return PendingIntent.getService(this,0,new Intent(this,VoiceService.class).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private PendingIntent homeIntent() {
        return PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification listening() {
        String name=mode==VoiceCommands.COMMAND ? "Perintah · yo / kalender / set" : "Mengetik · ucapkan ketik";
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_voice_mic)
                .setContentTitle("Mikrofon aktif: " + name).setContentText("Ketuk untuk membuka launcher; hentikan bila selesai.")
                .setContentIntent(homeIntent()).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null,"Hentikan",stopIntent()).build()).build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if (intent==null || ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!ACTION_START.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        int requested=intent.getIntExtra("mode",VoiceCommands.OFF);
        if (requested!=VoiceCommands.COMMAND && requested!=VoiceCommands.TYPING) { stopSelf(); return START_NOT_STICKY; }
        mode=requested;
        try {
            if (Build.VERSION.SDK_INT>=29) startForeground(ID,listening(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            else startForeground(ID,listening());
        } catch (RuntimeException e) { fail("Mikrofon tidak dapat dimulai. Buka launcher dan periksa izin."); return START_NOT_STICKY; }
        VoiceController.refreshButtons();
        failures=0; restartRecognizer();
        return START_NOT_STICKY;
    }
    private void restartRecognizer() {
        replies.cancel(); handler.removeCallbacksAndMessages(null); VoiceController.resetWave(); lastRmsAt=0; generation++;
        if(recognizer!=null) { recognizer.destroy(); recognizer=null; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { fail("Pengenalan suara tidak tersedia di perangkat."); return; }
        try {
            recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            final int session=generation;
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) { }
                public void onBeginningOfSpeech() { }
                public void onRmsChanged(float rms) {
                    if (session!=generation || mode==VoiceCommands.OFF) return;
                    long now=android.os.SystemClock.elapsedRealtime();
                    if (now-lastRmsAt<60) return;
                    lastRmsAt=now; VoiceController.voiceLevel(rms);
                    handler.removeCallbacks(resetRms); handler.postDelayed(resetRms,350);
                }
                public void onBufferReceived(byte[] b) { }
                public void onEndOfSpeech() { if(session==generation) { handler.removeCallbacks(resetRms); VoiceController.resetWave(); } }
                public void onPartialResults(Bundle b) { }
                public void onEvent(int event, Bundle b) { }
                public void onError(int error) {
                    if (session!=generation || mode==VoiceCommands.OFF || replies.isBusy()) return;
                    clearWatchdog(); handler.removeCallbacks(resetRms); VoiceController.resetWave();
                    if (error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS || error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE || error==SpeechRecognizer.ERROR_CLIENT) {
                        fail("Pengenalan suara berhenti. Periksa izin, bahasa, dan layanan suara."); return;
                    }
                    // Silence is normal; repeated network, busy or audio errors eventually stop visibly.
                    if (error!=SpeechRecognizer.ERROR_NO_MATCH && error!=SpeechRecognizer.ERROR_SPEECH_TIMEOUT) failures++;
                    if(failures>=5) { fail("Pengenalan suara berhenti setelah beberapa kesalahan. Coba mulai lagi."); return; }
                    retry(session, error==SpeechRecognizer.ERROR_NO_MATCH || error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT ? 1000 : Math.min(15000,1000L << failures));
                }
                public void onResults(Bundle result) {
                    if (session!=generation || mode==VoiceCommands.OFF || replies.isBusy()) return;
                    clearWatchdog(); handler.removeCallbacks(resetRms); VoiceController.resetWave(); failures=0;
                    ArrayList<String> matches=result==null ? null : result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if(matches!=null && !matches.isEmpty() && handle(matches.get(0))) return;
                    if(mode!=VoiceCommands.OFF && !replies.isBusy()) retry(session,500);
                }
            });
            listen();
        } catch (RuntimeException e) { fail("Pengenalan suara tidak dapat dimulai."); }
    }
    private void clearWatchdog() { if(watchdog!=null) { handler.removeCallbacks(watchdog); watchdog=null; } }
    private void listen() {
        if(recognizer==null || mode==VoiceCommands.OFF || replies.isBusy()) return;
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"id-ID")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        try {
            recognizer.startListening(i);
            clearWatchdog();
            final int session=generation;
            watchdog=() -> {
                if(session!=generation || mode==VoiceCommands.OFF) return;
                if(++failures>=5) { fail("Layanan suara tidak merespons. Coba mulai lagi."); return; }
                handler.postDelayed(() -> { if(session==generation && mode!=VoiceCommands.OFF) restartRecognizer(); },Math.min(15000,1000L << failures));
            };
            handler.postDelayed(watchdog,45000);
        } catch (RuntimeException e) { fail("Pengenalan suara tidak dapat dimulai."); }
    }
    private void retry(int session,long delay) { handler.postDelayed(() -> { if(session==generation) listen(); },delay); }
    /** Returns true when a spoken reply owns the next recognizer restart. */
    private boolean handle(String spoken) {
        String body=VoiceCommands.body(mode,spoken);
        if(body==null) return false;
        String action=mode==VoiceCommands.COMMAND ? VoiceCommands.command(body) : "";
        if(mode==VoiceCommands.COMMAND && "stop".equals(action)) { stopSelf(); return false; }
        if("calendar_week".equals(action)) {
            if (!VoiceController.foregroundUnlocked(this)) {
                Intent target=VoiceController.destination(this,"calendar");
                PendingIntent open=PendingIntent.getActivity(this,1,target,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                notifications.notify(OPEN_ID,new Notification.Builder(this,ALERTS).setSmallIcon(R.drawable.ic_voice_mic)
                        .setContentTitle("Buka kalender untuk jawaban suara")
                        .setContentText("Buka launcher saat perangkat tidak terkunci, lalu tanyakan lagi.")
                        .setContentIntent(open).setAutoCancel(true).build());
                return false;
            }
            try {
                String json=getSharedPreferences("launcher_calendar",MODE_PRIVATE).getString("events_v1","[]");
                String answer=CalendarVoiceSummary.summarize(new org.json.JSONArray(json),java.time.LocalDate.now());
                final int session=generation;
                replies.speak(answer,() -> { if(session==generation && mode!=VoiceCommands.OFF) retry(session,500); });
            } catch(Exception e) {
                final int session=generation;
                replies.speak("Data kalender tidak dapat dibaca. Silakan buka kalender dan periksa data.",
                        () -> { if(session==generation && mode!=VoiceCommands.OFF) retry(session,500); });
            }
            return true;
        }
        if(VoiceController.deliver(mode,spoken)) return false;
        if(mode==VoiceCommands.COMMAND) {
            Intent target=VoiceController.destination(this,VoiceCommands.command(body));
            if(target!=null) {
                PendingIntent open=PendingIntent.getActivity(this,1,target,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                notifications.notify(OPEN_ID,new Notification.Builder(this,ALERTS).setSmallIcon(R.drawable.ic_voice_mic)
                        .setContentTitle("Perintah suara siap dibuka").setContentText("Ketuk untuk membuka halaman yang diminta.")
                        .setContentIntent(open).setAutoCancel(true).build());
            }
        }
        return false;
    }
    private void fail(String message) {
        notifications.notify(ALERT_ID,new Notification.Builder(this,ALERTS).setSmallIcon(R.drawable.ic_voice_mic)
                .setContentTitle("Suara berhenti").setContentText(message).setContentIntent(homeIntent()).setAutoCancel(true).build());
        stopSelf();
    }
    @Override public void onDestroy() {
        mode=VoiceCommands.OFF; VoiceController.resetWave(); VoiceController.refreshButtons(); generation++; replies.close(); handler.removeCallbacksAndMessages(null);
        if(recognizer!=null) { recognizer.destroy(); recognizer=null; }
        notifications.cancel(OPEN_ID); super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
