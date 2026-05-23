package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dashboard.MainDashboardScreen
import com.example.ui.mindmap.MindMapScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MindMapViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme { // Natural Tones dynamic system theme adaptive mode
        val viewModel: MindMapViewModel = viewModel()
        val activeId by viewModel.activeMindMapId.collectAsState()

        // Handle physical back button presses inside the canvas editor
        if (activeId != null) {
            BackHandler {
                viewModel.selectMindMap(null)
            }
        }

        Crossfade(
            targetState = activeId,
            modifier = Modifier.fillMaxSize(),
            label = "ScreenTransition"
        ) { currentId ->
            if (currentId == null) {
                MainDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMindMap = { id ->
                        viewModel.selectMindMap(id)
                    }
                )
            } else {
                MindMapScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        viewModel.selectMindMap(null)
                    }
                )
            }
        }
      }
    }
  }
}

