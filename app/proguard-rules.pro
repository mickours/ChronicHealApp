# General
-keepattributes Signature,Annotation,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *
-keep @androidx.room.Entity class *
-keep @androidx.room.TypeConverter class *
-keep class * extends androidx.room.TypeConverter
-keep class **_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <fields>;
}

# Hilt / Dagger
# Most rules are bundled, keeping only essential ones if needed
-dontwarn dagger.hilt.android.internal.managers.**
-keep class **_HiltComponents* { *; }
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Kotlin Serialization
# The library provides its own rules, but we keep serializable classes to be safe
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}

# Keep data and domain classes for Room/Serialization
-keep class org.chronicheal.app.domain.model.** { *; }
-keep class org.chronicheal.app.data.local.** { *; }

# Keep enums names for Serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Java Time desugaring
-keep class java.time.** { *; }
-dontwarn java.time.**

# Suppress warnings
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue
