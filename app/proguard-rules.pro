# Add project specific ProGuard rules here.
# Keep Room entities
-keep class cz.kovmak.pomocnik.data.database.** { *; }

# Keep data classes
-keep class cz.kovmak.pomocnik.data.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
