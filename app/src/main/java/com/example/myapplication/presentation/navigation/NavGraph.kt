package com.example.myapplication.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.repository.BudgetRepository
import com.example.myapplication.data.repository.BudgetItemRepository
import com.example.myapplication.data.repository.ClientRepository
import com.example.myapplication.presentation.ui.screen.AddItemScreen
import com.example.myapplication.presentation.ui.screen.BudgetDetailScreen
import com.example.myapplication.presentation.ui.screen.BudgetListScreen
import com.example.myapplication.presentation.ui.screen.ClientListScreen
import com.example.myapplication.presentation.ui.screen.CreateBudgetScreen
import com.example.myapplication.presentation.ui.screen.EditClientScreen
import com.example.myapplication.presentation.ui.screen.HomeScreen
import com.example.myapplication.presentation.ui.screen.SettingsScreen
import com.example.myapplication.presentation.viewmodel.AddItemViewModel
import com.example.myapplication.presentation.viewmodel.BudgetDetailViewModel
import com.example.myapplication.presentation.viewmodel.BudgetListViewModel
import com.example.myapplication.presentation.viewmodel.ClientListViewModel
import com.example.myapplication.presentation.viewmodel.CreateBudgetViewModel
import com.example.myapplication.presentation.viewmodel.EditClientViewModel
import com.example.myapplication.presentation.viewmodel.SettingsViewModel

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object BudgetList : Destination("budget_list")
    data object CreateBudget : Destination("create_budget")
    data object BudgetDetail : Destination("budget_detail/{budgetId}") {
        fun createRoute(budgetId: Int) = "budget_detail/$budgetId"
    }
    data object AddItem : Destination("add_item/{budgetId}") {
        fun createRoute(budgetId: Int) = "add_item/$budgetId"
    }
    data object EditItem : Destination("edit_item/{budgetId}/{itemId}") {
        fun createRoute(budgetId: Int, itemId: Int) = "edit_item/$budgetId/$itemId"
    }
    data object ClientList : Destination("client_list")
    data object Settings : Destination("settings")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    budgetRepository: BudgetRepository,
    clientRepository: ClientRepository,
    budgetItemRepository: BudgetItemRepository,
    settingsRepository: com.example.myapplication.data.repository.SettingsRepository,
    pdfGeneratorService: com.example.myapplication.data.service.PdfGeneratorService,
    sharingService: com.example.myapplication.data.service.SharingService,
    startDestination: String = Destination.Home.route
) {
    val sharedSettingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(settingsRepository)
    )
    val settingsState by sharedSettingsViewModel.uiState.collectAsState()
    val businessType = settingsState.businessType

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onNavigateToCreateBudget = { navController.navigate(Destination.CreateBudget.route) },
                onNavigateToClients = { navController.navigate(Destination.ClientList.route) },
                onNavigateToBudgetList = { navController.navigate(Destination.BudgetList.route) },
                onNavigateToSettings = { navController.navigate(Destination.Settings.route) }
            )
        }

        composable(Destination.BudgetList.route) {
            val viewModel: BudgetListViewModel = viewModel(
                factory = BudgetListViewModel.provideFactory(budgetRepository, budgetItemRepository)
            )
            BudgetListScreen(
                viewModel = viewModel,
                onNavigateToCreateBudget = { navController.navigate(Destination.CreateBudget.route) },
                onNavigateToBudgetDetail = { budgetId ->
                    navController.navigate(Destination.BudgetDetail.createRoute(budgetId))
                },
                onNavigateToHome = { navController.navigateUp() },
                onNavigateToDuplicatedBudget = { budgetId ->
                    navController.navigate(Destination.BudgetDetail.createRoute(budgetId))
                }
            )
        }

        composable(Destination.CreateBudget.route) {
            val viewModel: CreateBudgetViewModel = viewModel(
                factory = CreateBudgetViewModel.provideFactory(budgetRepository, clientRepository)
            )
            CreateBudgetScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onBudgetCreated = { budgetId ->
                    navController.navigate(Destination.BudgetDetail.createRoute(budgetId)) {
                        popUpTo(Destination.CreateBudget.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Destination.BudgetDetail.route) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")?.toIntOrNull() ?: 0
            val viewModel: BudgetDetailViewModel = viewModel(
                factory = BudgetDetailViewModel.provideFactory(
                    budgetRepository,
                    budgetItemRepository,
                    clientRepository,
                    settingsRepository,
                    pdfGeneratorService,
                    sharingService,
                    budgetId
                )
            )
            BudgetDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddItem = { bId ->
                    navController.navigate(Destination.AddItem.createRoute(bId))
                },
                onNavigateToEditItem = { bId, itemId ->
                    navController.navigate(Destination.EditItem.createRoute(bId, itemId))
                },
                onNavigateToDuplicatedBudget = { newBudgetId ->
                    navController.navigate(Destination.BudgetDetail.createRoute(newBudgetId))
                },
                onBudgetDeleted = { navController.navigateUp() }
            )
        }

        composable(Destination.AddItem.route) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")?.toIntOrNull() ?: 0
            val viewModel: AddItemViewModel = viewModel(
                factory = AddItemViewModel.provideFactory(budgetItemRepository, budgetId)
            )
            AddItemScreen(
                viewModel = viewModel,
                businessType = businessType,
                onNavigateBack = { navController.navigateUp() },
                onItemAdded = { navController.navigateUp() }
            )
        }

        composable(Destination.EditItem.route) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")?.toIntOrNull() ?: 0
            val itemId = backStackEntry.arguments?.getString("itemId")?.toIntOrNull() ?: 0
            val viewModel: AddItemViewModel = viewModel(
                factory = AddItemViewModel.provideFactory(budgetItemRepository, budgetId, itemId)
            )
            AddItemScreen(
                viewModel = viewModel,
                businessType = businessType,
                onNavigateBack = { navController.navigateUp() },
                onItemAdded = { navController.navigateUp() }
            )
        }

        composable(Destination.ClientList.route) {
            val viewModel: ClientListViewModel = viewModel(
                factory = ClientListViewModel.provideFactory(clientRepository)
            )
            ClientListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToEditClient = { clientId ->
                    navController.navigate("edit_client/$clientId")
                }
            )
        }

        composable("edit_client/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")?.toIntOrNull() ?: 0
            val viewModel: EditClientViewModel = viewModel(
                factory = EditClientViewModel.provideFactory(clientRepository, clientId)
            )
            EditClientScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Destination.Settings.route) {
            SettingsScreen(
                viewModel = sharedSettingsViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
