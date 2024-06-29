plugins {
    id("com.android.application")
}

android {
    namespace = "com.fortanix.qrcode_scanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fortanix.qrcode_scanner"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    /* Default inclusion of dependencies for android project */
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    /* Application (QRCode scanner app) specific dependencies */
    implementation("com.google.zxing:core:3.4.1")
    implementation ("me.dm7.barcodescanner:zxing:1.9.8")
    implementation ("androidx.preference:preference:1.2.1")

    /* Inclusion of JCE/JAVA DSM-Accelerator JAR */
    implementation(files("lib/sdkms-jce-provider-android-dsma-4.24.devel.jar"))

}