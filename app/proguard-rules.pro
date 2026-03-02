# General
-keepattributes Signature,Annotation,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

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
-dontwarn dagger.hilt.android.internal.managers.**
-keep class **_HiltComponents* { *; }
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.EntryPoint { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep @dagger.Module class *
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class com.google.dagger.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** $serializer;
}
-keep @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keep @kotlinx.serialization.Serializable class * {
    *** $serializer;
}
-keep @kotlinx.serialization.Serializable class * {
    <init>(...);
}
-keep class **$$serializer { *; }
-keep class kotlinx.serialization.json.Json { *; }
-keep class kotlinx.serialization.internal.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Keep data and domain classes
-keep class org.chronicheal.app.domain.model.** { *; }
-keepclassmembers class org.chronicheal.app.domain.model.** { *; }
-keep class org.chronicheal.app.data.local.** { *; }
-keepclassmembers class org.chronicheal.app.data.local.** { *; }

# Keep enums names for Serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Java Time desugaring
-keep class java.time.** { *; }
-dontwarn java.time.**
