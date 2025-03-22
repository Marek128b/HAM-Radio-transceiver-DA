buildscript{
    dependencies{
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.52")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.0")

    }
    /*
    repositories{
        mavenCentral()
    }

     */
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}