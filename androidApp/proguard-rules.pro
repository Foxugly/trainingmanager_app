# kotlinx.serialization — keep @Serializable DTOs + generated serializers.
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers,allowshrinking class * {
    @kotlinx.serialization.Serializable <methods>;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    *** serializer(...);
}
# Ktor + coroutines internals referenced reflectively.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Release builds drop verbose/debug/info logcat calls (audit A-M2): AppLogger.debug/info
# log request paths, some of which carry link tokens. Warnings and errors stay.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static boolean isLoggable(java.lang.String, int);
}
