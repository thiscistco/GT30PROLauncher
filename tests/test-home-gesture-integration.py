from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
a='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
assert 'android.permission.EXPAND_STATUS_BAR' in {p.get(a+'name') for p in manifest.findall('uses-permission')}
java=root/'app/src/main/java/com/gt30pro/launcher'
main=(java/'MainActivity.java').read_text(); panels=(java/'SystemPanels.java').read_text(); html=(root/'preview/index.html').read_text()
assert 'HomeGesture.SUPER) openSuperAppFromSwipe()' in main
assert 'HomeGesture.NOTIFICATIONS) SystemPanels.open(this,false)' in main
assert 'HomeGesture.CONTROLS) SystemPanels.open(this,true)' in main
assert 'MotionEvent.ACTION_CANCEL' in main and 'MotionEvent.ACTION_POINTER_DOWN' in main and 'homeCancelled ? HomeGesture.NONE' in main
assert 'onFling(' not in main
for item in ('expandSettingsPanel','expandNotificationsPanel','ReflectiveOperationException','RuntimeException','LinkageError','Geser dari tepi atas layar'):
    assert item in panels
assert 'Ini hanya simulasi preview.' in html and 'Browser tidak dapat membuka panel sistem' in html
assert 'touch-action:none' in html and 'system-panel-preview' in html
print('Android home gesture wiring, permission, fallbacks and honest preview simulation: OK')
