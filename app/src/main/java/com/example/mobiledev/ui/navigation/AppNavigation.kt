package com.example.mobiledev.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobiledev.data.model.UserRole
import com.example.mobiledev.ui.auth.*
import com.example.mobiledev.ui.merchant.AddBagScreen
import com.example.mobiledev.ui.merchant.MyBagsScreen
import com.example.mobiledev.ui.merchant.MyStoreScreen
import com.example.mobiledev.ui.customer.CustomerHome

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val CUSTOMER_HOME = "customer_home"
    const val MERCHANT_HOME = "merchant_home"
    const val ADD_BAG = "add_bag"
    const val MY_STORE = "my_store"
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val state by authViewModel.state.collectAsStateWithLifecycle()

    if (state.isCheckingSession) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when (state.loggedInRole) {
        UserRole.CUSTOMER -> Routes.CUSTOMER_HOME
        UserRole.MERCHANT -> Routes.MERCHANT_HOME
        null -> Routes.LOGIN
    }

    // Na login/registratie of logout: naar het juiste scherm
    LaunchedEffect(state.loggedInRole) {
        val target = when (state.loggedInRole) {
            UserRole.CUSTOMER -> Routes.CUSTOMER_HOME
            UserRole.MERCHANT -> Routes.MERCHANT_HOME
            null -> Routes.LOGIN
        }
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = state,
                onLogin = authViewModel::login,
                onGoToRegister = { authViewModel.clearMessages(); navController.navigate(Routes.REGISTER) },
                onGoToForgot = { authViewModel.clearMessages(); navController.navigate(Routes.FORGOT) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                state = state,
                onRegister = authViewModel::register,
                onGoToLogin = { authViewModel.clearMessages(); navController.popBackStack() }
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                state = state,
                onSend = authViewModel::resetPassword,
                onBack = { authViewModel.clearMessages(); navController.popBackStack() }
            )
        }
        composable(Routes.CUSTOMER_HOME) {
            CustomerHome(onLogout = authViewModel::logout)
        }
        // Handelaar: startscherm = "Mijn pakketten" (H5)
        composable(Routes.MERCHANT_HOME) {
            MyBagsScreen(
                onAddBag = { navController.navigate(Routes.ADD_BAG) },
                onOpenStore = { navController.navigate(Routes.MY_STORE) },
                onLogout = authViewModel::logout
            )
        }
        // Handelaar: "Pakket aanmaken" (H3 + H4)
        composable(Routes.ADD_BAG) {
            AddBagScreen(onSaved = { navController.popBackStack() })
        }
        // Handelaar: "Mijn winkel" (H1 + H2)
        composable(Routes.MY_STORE) {
            MyStoreScreen(onSaved = { navController.popBackStack() })
        }
    }
}
