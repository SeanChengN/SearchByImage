-keepattributes *Annotation*
-dontwarn org.conscrypt.**

# Room and WorkManager ship consumer rules. Keep only JSON-parsed result models.
-keep class io.github.seancheng.searchbyimage.domain.NativeResultItem { *; }
