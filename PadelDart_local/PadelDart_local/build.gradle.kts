// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Google Services: necesario para que el plugin lea el google-services.json del módulo :app
    alias(libs.plugins.google.services)     apply false
}
