package com.medusko.velocast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.medusko.velocast.ui.MainScreen
import com.medusko.velocast.ui.VelocastViewModel
import com.medusko.velocast.ui.theme.VelocastTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VelocastViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            VelocastTheme(darkTheme = isDarkMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
