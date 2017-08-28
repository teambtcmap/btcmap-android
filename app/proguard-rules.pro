# Okio

-dontwarn okio.**

# OkHttp

-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Retrofit

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Also you must note that if you are using GSON for conversion from JSON to POJO representation, you must ignore those POJO classes from being obfuscated.
# Here include the POJO's that have you have created for mapping JSON response to POJO for example.

-keep class com.bubelov.coins.api.** { *; }
-keep class com.bubelov.coins.model.** { *; }

# Picasso

-dontwarn com.squareup.okhttp.**

# Dagger 2

-dontwarn com.google.errorprone.annotations.*