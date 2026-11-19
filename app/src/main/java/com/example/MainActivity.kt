package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.configureAppCheck
import com.example.ui.navigation.StudyTimeRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must install App Check provider before any Firebase AI or Firestore calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            StudyTimeRoot()
        }
    }
}
