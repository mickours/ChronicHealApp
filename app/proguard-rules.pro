# General
-keepattributes Signature,Annotation,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.lifecycle.ViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.TypeConverter
-keep class **_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <fields>;
}

# Hilt
-keep class **_HiltComponents* { *; }
-keep class **_HiltModules* { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.UnsafeCasts.InternalFactory
-keep class * implements dagger.hilt.EntryPoint
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep @dagger.Module class *

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses
-keep class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class kotlinx.serialization.json.Json { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Keep domain models and their members
-keep class org.chronicheal.app.domain.model.** { *; }
-keepclassmembers class org.chronicheal.app.domain.model.** { *; }

# Keep enums names for Serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
