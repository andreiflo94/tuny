package com.heixss.guitartuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.heixss.guitartuner.presentation.ui.TunerScreen
import com.heixss.guitartuner.ui.theme.TunyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TunyTheme(darkTheme = true, dynamicColor = false) {
                TunerScreen()
            }
        }
    }
}
