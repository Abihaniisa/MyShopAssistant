package com.shopassistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shopassistant.ShopApp
import com.shopassistant.ui.detail.ProductDetailScreen
import com.shopassistant.ui.detail.ProductDetailViewModel
import com.shopassistant.ui.form.ProductFormScreen
import com.shopassistant.ui.form.ProductFormViewModel
import com.shopassistant.ui.list.ProductListScreen
import com.shopassistant.ui.list.ProductListViewModel

object Routes {
    const val LIST = "products"
    const val DETAIL = "product/{id}"
    const val FORM = "form/{id}"

    fun detail(id: Long) = "product/$id"
    fun form(id: Long) = "form/$id"
}

@Composable
fun ShopNavHost() {
    val navController = rememberNavController()
    val repository = (LocalContext.current.applicationContext as ShopApp).repository
    val idArgument = listOf(navArgument("id") { type = NavType.LongType })

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            val viewModel: ProductListViewModel = viewModel(
                factory = viewModelFactory { initializer { ProductListViewModel(repository) } }
            )
            ProductListScreen(
                viewModel = viewModel,
                onAdd = { navController.navigate(Routes.form(0L)) },
                onOpen = { navController.navigate(Routes.detail(it)) }
            )
        }

        composable(Routes.DETAIL, arguments = idArgument) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val viewModel: ProductDetailViewModel = viewModel(
                factory = viewModelFactory { initializer { ProductDetailViewModel(repository, id) } }
            )
            ProductDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.form(it)) },
                onDeleted = { navController.popBackStack() }
            )
        }

        composable(Routes.FORM, arguments = idArgument) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val viewModel: ProductFormViewModel = viewModel(
                factory = viewModelFactory { initializer { ProductFormViewModel(repository, id) } }
            )
            ProductFormScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
