package android.speech.tts;
import android.content.Context;
import android.os.Bundle;
import java.util.Set;
import java.util.LinkedHashSet;
public class TextToSpeech {
    public static final int SUCCESS=0, ERROR=-1, QUEUE_FLUSH=0;
    public interface OnInitListener { void onInit(int status); }
    public static TextToSpeech latest;
    public OnInitListener init; public UtteranceProgressListener listener; public final Set<Voice> voices=new LinkedHashSet<>();
    public int stops,shutdowns,speaks,voiceResult=SUCCESS,speakResult=SUCCESS;
    public String text,id; public Voice chosen;
    public TextToSpeech(Context c,OnInitListener init) { this.init=init; latest=this; }
    public void initialize(int status) { init.onInit(status); }
    public int setOnUtteranceProgressListener(UtteranceProgressListener listener) { this.listener=listener; return SUCCESS; }
    public Set<Voice> getVoices() { return voices; }
    public int setVoice(Voice voice) { chosen=voice; return voiceResult; }
    public int speak(CharSequence text,int queue,Bundle params,String id) { speaks++; this.text=text.toString(); this.id=id; return speakResult; }
    public int stop() { stops++; return SUCCESS; }
    public void shutdown() { shutdowns++; }
}
