# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep OpenAI API classes
-keep class com.personaleenergy.app.api.** { *; }
-keepclassmembers class com.personaleenergy.app.api.** { *; }

# Keep data classes
-keep class com.personaleenergy.app.data.** { *; }

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }

