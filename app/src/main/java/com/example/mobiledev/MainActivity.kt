package com.example.mobiledev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.mobiledev.ui.auth.AuthViewModel
import com.example.mobiledev.ui.navigation.AppNavigation
import com.example.mobiledev.ui.theme.MobiledevTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobiledevTheme {
                AppNavigation(authViewModel)
            }
        }
    }
}