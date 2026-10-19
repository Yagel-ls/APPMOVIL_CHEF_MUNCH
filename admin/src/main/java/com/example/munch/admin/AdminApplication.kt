package com.example.munch.admin

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("munchapp-9829d")
                    .setApplicationId("1:720736794797:android:297e0c5d22273cad51ae24")
                    .setApiKey("AIzaSyAPPDhfsXeX8e0cs_m3V1N6izKWHxf-XTU")
                    .setStorageBucket("munchapp-9829d.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
            Log.d("MUNCH_FIREBASE", "FirebaseApp inicializado correctamente en AdminApplication")
        } catch (e: Exception) {
            Log.e("MUNCH_FIREBASE", "Error inicializando FirebaseApp en AdminApplication", e)
        }
    }
}
