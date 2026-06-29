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
