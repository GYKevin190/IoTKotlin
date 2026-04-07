package com.example.webshop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.webshop.data.WebshopRepository
import com.example.webshop.data.entity.CartItemEntity
import com.example.webshop.data.entity.ProductEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: WebshopRepository
) : ViewModel() {

    val products = repository.products.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val cartItems = repository.cartItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val orders = repository.orders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedProductsIfNeeded()
        }
    }

    fun addToCart(product: ProductEntity) {
        viewModelScope.launch {
            repository.addToCart(product)
        }
    }

    fun createProduct(
        name: String,
        description: String,
        price: Double,
        imageSource: String
    ) {
        viewModelScope.launch {
            repository.createProduct(name, description, price, imageSource)
        }
    }

    fun updateProduct(
        id: Int,
        name: String,
        description: String,
        price: Double,
        imageSource: String
    ) {
        viewModelScope.launch {
            repository.updateProduct(id, name, description, price, imageSource)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun increaseQuantity(item: CartItemEntity) {
        viewModelScope.launch {
            repository.increaseQuantity(item)
        }
    }

    fun decreaseQuantity(item: CartItemEntity) {
        viewModelScope.launch {
            repository.decreaseQuantity(item)
        }
    }

    fun checkout(email: String) {
        val currentCart = cartItems.value
        if (currentCart.isEmpty()) return

        viewModelScope.launch {
            repository.checkout(email, currentCart)
        }
    }
}

class MainViewModelFactory(
    private val repository: WebshopRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}
