-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

-dontwarn org.jetbrains.annotations.**

-keep class com.appsflyer.** { *; }
-keep class com.android.installreferrer.** { *; }
-dontwarn com.appsflyer.**

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

-keep class androidx.security.crypto.** { *; }

-keep class com.voidloom.keel.KeelApp
-keep class com.voidloom.keel.wire.KeelPingService
-keep class com.voidloom.keel.KeelBootActivity
-keep class com.voidloom.keel.KeelPaneActivity
-keep class com.voidloom.keel.KeelHailActivity
-keep class com.voidloom.keel.KeelDryActivity
-keep class com.gravitysiege.gravitysiegegame.MainActivity
-keep class com.gravitysiege.gravitysiegegame.GameStore { *; }
-keep class com.gravitysiege.gravitysiegegame.AssetBitmaps { *; }
-keep class com.gravitysiege.gravitysiegegame.ui.** { *; }
-keep class com.gravitysiege.gravitysiegegame.game.** { *; }
-keep class com.gravitysiege.gravitysiegegame.audio.** { *; }

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
