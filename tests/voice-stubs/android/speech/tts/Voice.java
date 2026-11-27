package android.speech.tts;
import java.util.Locale;
import java.util.Set;
public class Voice {
    private final Locale locale; private final boolean network; private final Set<String> features;
    public Voice(Locale locale,boolean network,Set<String> features) { this.locale=locale; this.network=network; this.features=features; }
    public Locale getLocale() { return locale; }
    public boolean isNetworkConnectionRequired() { return network; }
    public Set<String> getFeatures() { return features; }
}
