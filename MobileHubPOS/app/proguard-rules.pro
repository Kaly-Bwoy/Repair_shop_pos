# Add project specific ProGuard rules here.
# By default, the noise in this file is to provide a starting point for
# Personalization. Keep your code clean!

# Keep Room database and classes
-keep class com.mobilehub.pos.data.local.entity.** { *; }
-keep class com.mobilehub.pos.data.local.dao.** { *; }

# Keep Gson serialization classes
-keep class com.google.gson.** { *; }
-keep class com.mobilehub.pos.data.repository.BackupRepository$** { *; }
