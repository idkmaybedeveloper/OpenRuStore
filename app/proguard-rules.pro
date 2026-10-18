# OkHttp, Coil and kotlinx.serialization ship their own consumer rules, so only what this app does
# reflectively needs spelling out here.

# NOTE: navigation3 stores the back stack by serializing the NavKey classes, and it looks their
# serializers up through the companion object instead of a compile time reference, so the generated
# members have to survive both shrinking and obfuscation.
-keepclassmembers class ru.openrustore.** {
    *** Companion;
}
-keepclasseswithmembers class ru.openrustore.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ru.openrustore.**$$serializer { *; }

# The data classes are filled in by kotlinx.serialization from JSON the API returns.
-keep @kotlinx.serialization.Serializable class ru.openrustore.data.** { *; }
