package com.example.webshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.webshop.data.AppDatabase
import com.example.webshop.data.WebshopRepository
import com.example.webshop.ui.AdminWebshopApp
import com.example.webshop.ui.theme.WebshopTheme
import com.example.webshop.viewmodel.MainViewModel
import com.example.webshop.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(this)
        val repository = WebshopRepository(
            productDao = db.productDao(),
            cartDao = db.cartDao(),
            orderDao = db.orderDao(),
            db = db
        )

        setContent {
            WebshopTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(repository)
                )
                AdminWebshopApp(viewModel)
            }
        }
    }
}
