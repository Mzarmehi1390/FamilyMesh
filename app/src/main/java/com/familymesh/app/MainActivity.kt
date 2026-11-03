package com.familymesh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.familymesh.app.ui.FamilyMeshHomeScreen
import com.familymesh.core.designsystem.FamilyMeshTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyMeshTheme {
                FamilyMeshHomeScreen()
            }
        }
    }
}
