# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt

# Keep Moshi adapters
-keepclassmembers @com.squareup.moshi.** class * {
    <methods>;
}

# Keep data classes used with Moshi
-keepclassmembers class com.nastv.data.model.** {
    <methods>;
}

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep native method names
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
