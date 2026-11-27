from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]; java=root/'app/src/main/java/com/gt30pro/launcher'
activity=(java/'LauncherSettingsActivity.java').read_text(); superapp=(java/'SuperAppActivity.java').read_text(); svc=(java/'VoiceService.java').read_text(); ctl=(java/'VoiceController.java').read_text()
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot(); a='{http://schemas.android.com/apk/res/android}'
settings=manifest.find("application/activity[@%sname='.LauncherSettingsActivity']" % a)
assert settings is not None and settings.get(a+'exported')=='false'
for item in ('Settings.ACTION_HOME_SETTINGS','Settings.ACTION_APPLICATION_DETAILS_SETTINGS','com.android.settings.TTS_SETTINGS','VoiceController.start(this,VoiceCommands.COMMAND)','VoiceController.start(this,VoiceCommands.TYPING)','VoiceController.stop(this)','Pengenalan khusus suara Anda belum tersedia','Runtime AI belum terintegrasi'):
    assert item in activity, item
assert 'LauncherSettingsActivity.class' in superapp
assert 'mode==VoiceCommands.COMMAND && "stop".equals(action)' in svc and 'stopSelf();' in svc
assert 'case "super": target=SuperAppActivity.class' in ctl
assert 'mode==VoiceCommands.TYPING) { insert(a,body)' in ctl
print('Settings: real Android entry points, explicit unavailable capabilities, command stop and Super App routing OK')
