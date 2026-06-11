package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SiakadApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SiakadViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: SiakadViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SiakadApp(viewModel = viewModel)
      }
    }
  }
}
