# Bluetooth Hebrew

אפליקציית Android בעברית לניהול פעולות Bluetooth במכשיר.

## יכולות
- הצגת מכשירים ששויכו בעבר וסריקת מכשירים זמינים.
- בקשת הרשאות Bluetooth מתאימות ל-Android 11 ומעלה.
- בקשת צימוד למכשיר שנבחר והפניה להגדרות Bluetooth של Android.
- בחירת קובץ ושיתוף דרך אפליקציות השיתוף הזמינות, לרבות Bluetooth אם נתמך במכשיר.
- ממשק עברי מימין לשמאל והצגת שגיאות והרשאות.

הערה: Android אינו מאפשר לאפליקציה כללית להתחבר לכל פרופיל Bluetooth או להעביר קבצים באופן אוניברסלי דרך API יחיד. פעולות שמע וקבלת קבצים תלויות בפרופיל, בגרסת Android ובתמיכת המכשיר.

## בנייה
GitHub Actions בונה APK מסוג debug בכל push ל-main. קובץ ה-APK נשמר כ-artifact, ובנוסף workflow מנסה לפרסם אותו כקובץ Release להורדה ישירה.


## Build troubleshooting
The Android SDK setup step installs the platform and build tools explicitly before invoking Gradle.
