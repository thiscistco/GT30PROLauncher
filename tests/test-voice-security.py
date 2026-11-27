from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/gt30pro/launcher'
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
a='{http://schemas.android.com/apk/res/android}'
service=manifest.find("application/service[@%sname='.VoiceService']" % a)
assert service is not None and service.get(a+'exported')=='false' and service.get(a+'foregroundServiceType')=='microphone'
permissions={x.get(a+'name') for x in manifest.findall('uses-permission')}
assert {'android.permission.RECORD_AUDIO','android.permission.FOREGROUND_SERVICE_MICROPHONE','android.permission.POST_NOTIFICATIONS'} <= permissions
svc=(java/'VoiceService.java').read_text(); ctl=(java/'VoiceController.java').read_text()
for item in ('START_NOT_STICKY','FLAG_IMMUTABLE','ACTION_STOP','SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS','failures>=5','removeCallbacksAndMessages','recognizer.destroy()','PendingIntent.getActivity'):
    assert item in svc, item
assert 'startActivity(' not in svc
assert 'hasWindowFocus()' in ctl and 'TYPE_TEXT_VARIATION_PASSWORD' in ctl and 'TYPE_TEXT_VARIATION_WEB_PASSWORD' in ctl
assert 'mode==VoiceCommands.COMMAND && "stop".equals(stop)' in ctl
assert 'watchDialog(this,dialog)' in (java/'CalendarActivity.java').read_text()
assert 'watchDialog((BottomSwipeActivity)a, dialog)' in (java/'AppUi.java').read_text()
assert 'requestPermissions' in ctl and 'areNotificationsEnabled()' in ctl and 'voice_consent' in ctl
assert 'lock==null || lock.isDeviceLocked()' in ctl
assert 'foregroundUnlocked(this)' in svc and 'CalendarVoiceSummary.summarize' in svc
assert svc.index('foregroundUnlocked(this)') < svc.index('CalendarVoiceSummary.summarize')
assert 'if(matches!=null && !matches.isEmpty() && handle(matches.get(0))) return' in svc
assert '!replies.isBusy()' in svc and 'replies.close()' in svc and 'replies.cancel()' in svc
assert 'Buka launcher saat perangkat tidak terkunci, lalu tanyakan lagi.' in svc
replies=(java/'VoiceReplies.java').read_text()
assert '!voice.isNetworkConnectionRequired()' in replies and 'notInstalled' in replies
assert 'foregroundUnlocked(context)' in replies and 'postDelayed(privacyCheck,250)' in replies
assert 'android.intent.action.TTS_SERVICE' in (root/'app/src/main/AndroidManifest.xml').read_text()
print('Voice security and lifecycle structure OK')
