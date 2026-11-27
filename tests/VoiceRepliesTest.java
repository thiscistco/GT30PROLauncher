package com.gt30pro.launcher;
import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.Toast;
import java.util.Locale;
import java.util.Collections;
public class VoiceRepliesTest {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static Voice local() { return new Voice(new Locale("id","ID"),false,Collections.emptySet()); }
    static Voice online() { return new Voice(new Locale("id","ID"),true,Collections.emptySet()); }
    public static void main(String[] args) {
        Handler h=new Handler(); Context ctx=new Context(); VoiceReplies r=new VoiceReplies(ctx,h); int[] done={0};
        VoiceController.unlocked=true; Toast.count=0;
        r.speak("Acara pribadi",()->done[0]++); TextToSpeech engine=TextToSpeech.latest;
        check(r.isBusy() && done[0]==0 && engine.speaks==0,"wait for engine");
        engine.voices.add(online()); engine.voices.add(local()); engine.initialize(TextToSpeech.SUCCESS);
        check(r.isBusy() && engine.speaks==1 && !engine.chosen.isNetworkConnectionRequired(),"offline voice selected");
        check(engine.text.equals("Acara pribadi"),"speaks reply"); String old=engine.id;
        engine.listener.onDone(old); check(!r.isBusy() && done[0]==1,"complete once");
        engine.listener.onDone(old); check(done[0]==1,"duplicate callback ignored");
        r.speak("Kedua",()->done[0]++); String current=engine.id;
        engine.listener.onDone(old); check(r.isBusy() && done[0]==1,"old callback cannot finish new reply");
        VoiceController.unlocked=false; h.runDelay(250);
        check(!r.isBusy() && done[0]==2 && engine.stops>0,"pause speech when no foreground or locked");
        engine.listener.onDone(current); check(done[0]==2,"privacy stop completes once");
        VoiceController.unlocked=true; r.speak("Ketiga",()->done[0]++); String cancelled=engine.id; r.cancel();
        engine.listener.onDone(cancelled); check(done[0]==2 && !r.isBusy(),"cancel does not restart");
        r.speak("Keempat",()->done[0]++); h.runDelay(60000); check(done[0]==3 && !r.isBusy(),"timeout resumes");
        r.close(); check(engine.shutdowns==1,"engine shutdown");
        VoiceReplies noVoice=new VoiceReplies(ctx,new Handler()); int[] finished={0};
        noVoice.speak("Jangan kirim",()->finished[0]++); TextToSpeech e=TextToSpeech.latest; e.voices.add(online()); e.initialize(TextToSpeech.SUCCESS);
        check(e.speaks==0 && finished[0]==1 && !noVoice.isBusy() && Toast.count>0,"no offline voice means no speech"); noVoice.close();
        VoiceReplies missingVoice=new VoiceReplies(ctx,new Handler()); int[] missingDone={0};
        missingVoice.speak("X",()->missingDone[0]++); TextToSpeech missing=TextToSpeech.latest;
        missing.voices.add(new Voice(new Locale("id","ID"),false,Collections.singleton("notInstalled"))); missing.initialize(TextToSpeech.SUCCESS);
        check(missing.speaks==0 && missingDone[0]==1,"not installed voice must not be used"); missingVoice.close();
        VoiceReplies lockedInit=new VoiceReplies(ctx,new Handler()); int[] lockDone={0};
        lockedInit.speak("Rahasia",()->lockDone[0]++); TextToSpeech locked=TextToSpeech.latest; locked.voices.add(local());
        VoiceController.unlocked=false; locked.initialize(TextToSpeech.SUCCESS);
        check(locked.speaks==0 && lockDone[0]==1,"do not speak if locked before initialization"); lockedInit.close(); VoiceController.unlocked=true;
        VoiceReplies late=new VoiceReplies(ctx,new Handler()); int[] afterClose={0};
        late.speak("a",()->afterClose[0]++); TextToSpeech stale=TextToSpeech.latest; late.close();
        late.speak("b",()->afterClose[0]++); TextToSpeech fresh=TextToSpeech.latest; fresh.voices.add(local());
        stale.initialize(TextToSpeech.ERROR); check(afterClose[0]==0 && late.isBusy() && fresh.shutdowns==0,"stale init cannot disrupt fresh engine");
        fresh.initialize(TextToSpeech.SUCCESS); fresh.listener.onDone(fresh.id); check(afterClose[0]==1,"fresh engine succeeds"); late.close();
        VoiceReplies badEngine=new VoiceReplies(ctx,new Handler()); int[] failed={0};
        badEngine.speak("X",()->failed[0]++); TextToSpeech f=TextToSpeech.latest; f.initialize(TextToSpeech.ERROR);
        check(f.shutdowns==1 && failed[0]==1 && !badEngine.isBusy(),"failed init closes engine"); badEngine.close();
        System.out.println("Voice reply: offline voice, init, privacy, timeout, cancellation and stale callbacks OK");
    }
}
