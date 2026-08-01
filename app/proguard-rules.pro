# Add project specific ProGuard rules here.

# Readable crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signatures + annotations: required by Gson TypeToken and the Firebase POJO mappers.
-keepattributes Signature,*Annotation*,InnerClasses

# Firestore / Realtime Database map documents onto these classes by reflection
# (no-arg constructor + property names), so their members must keep their names.
-keepclassmembers class com.example.messenger.domain.model.** {
    <init>();
    <fields>;
    <methods>;
}
-keepclassmembers class com.example.messenger.data.remote.dto.** {
    <init>();
    <fields>;
    <methods>;
}

# enum.name is persisted (ObjectBox / Firestore / Realtime DB) and read back with valueOf(),
# so constant names must stay stable across builds.
-keepclassmembers enum com.example.messenger.domain.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ObjectBox entities + generated cursors: kept broadly. The package is tiny and this is the
# persistence layer with prior schema-migration crash history, so the marginal shrink is not
# worth the data-loss risk.
-keep class com.example.messenger.data.local.obx.** { *; }

# Gson: keep generic type info on the TypeToken subclasses used by the ObjectBox converters.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Agora voice SDK is native; its C++ layer calls into these classes by name over JNI.
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# kotlinx.serialization: Navigation 3 serializes @Serializable route keys to persist the
# back stack across process death, so the generated serializers must survive shrinking.
-keepclassmembers @kotlinx.serialization.Serializable class com.example.messenger.presentation.navigation.** {
    *** Companion;
    *** serializer(...);
}
-keep class com.example.messenger.presentation.navigation.**$$serializer { *; }
