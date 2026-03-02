# Room
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.Entity

# Hilt
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.viewbinding.ViewBinding
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.json.Json { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Keep domain models
-keep class org.chronicheal.app.domain.model.** { *; }
