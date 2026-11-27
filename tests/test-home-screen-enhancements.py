from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]; java=root/'app/src/main/java/com/gt30pro/launcher'
main=(java/'MainActivity.java').read_text(); sup=(java/'SuperAppActivity.java').read_text(); settings=(java/'LauncherSettingsActivity.java').read_text(); download=(java/'ModelDownload.java').read_text(); html=(root/'preview/index.html').read_text()
a='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot(); receiver=manifest.find("application/receiver[@%sname='.ScreenLockAdmin']" % a)
assert receiver is not None and receiver.get(a+'permission')=='android.permission.BIND_DEVICE_ADMIN'
policy=ET.parse(root/'app/src/main/res/xml/screen_lock_admin.xml').getroot(); assert policy.find('uses-policies/force-lock') is not None
for text in ('public boolean onDoubleTap(MotionEvent e)', 'lockHomeScreen();', 'policy.isAdminActive(admin)', 'ACTION_ADD_DEVICE_ADMIN', 'policy.lockNow()'): assert text in main
assert 'home_swipe_time' in main and 'home_swipe_time' in sup and 'age<=2000' in sup and 'AppDrawerActivity.class' in sup
for text in ('startDragAndDrop', 'DragEvent.ACTION_DROP', 'getLocalState()', 'putString("order"', 'params.height = cellWidth', 'ModuleOrder.parse'): assert text in sup
assert '#superGrid > * { aspect-ratio:1; height:auto; }' in html and 'super-reorder.js?v=1' in html
for text in ('new Switch(this)', 'setOnCheckedChangeListener', 'Tema aplikasi', 'ModelDownload.start(this)', 'ModelDownload.cancel()', 'ModelDownload.delete(this)', 'Runtime AI belum terintegrasi'): assert text in settings
for text in ('639446688L', '9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031', 'setInstanceFollowRedirects(false)', '"https"', 'MessageDigest.getInstance("SHA-256")', 'ModelIntegrity.matches(BYTES,received,SHA256,digest.digest())', 'temp.renameTo(model(app))', 'if(temp.exists())temp.delete()', 'setReadTimeout'): assert text in download
for f in ('backup_rules.xml','data_extraction_rules.xml'): assert 'domain="file" path="ai/"' in (root/'app/src/main/res/xml'/f).read_text()
print('Screen lock consent, double swipe, persistent drag order, actionable settings and pinned verified model download wiring OK')
