package com.example.webshop.data

import androidx.room.withTransaction
import com.example.webshop.data.dao.CartDao
import com.example.webshop.data.dao.OrderDao
import com.example.webshop.data.dao.ProductDao
import com.example.webshop.data.entity.CartItemEntity
import com.example.webshop.data.entity.OrderEntity
import com.example.webshop.data.entity.OrderItemEntity
import com.example.webshop.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class WebshopRepository(
    private val productDao: ProductDao,
    private val cartDao: CartDao,
    private val orderDao: OrderDao,
    private val db: AppDatabase
) {

    val products: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val cartItems: Flow<List<CartItemEntity>> = cartDao.getCartItems()
    val orders: Flow<List<OrderEntity>> = orderDao.getAllOrders()

    suspend fun seedProductsIfNeeded() {
        if (productDao.countProducts() == 0) {
            productDao.insertAll(
                listOf(
                    ProductEntity(name = "Póló", description = "Kényelmes pamut póló", price = 4990.0, imageResName = "tshirt"),
                    ProductEntity(name = "Farmernadrág", description = "Klasszikus kék farmer", price = 12990.0, imageResName = "jeans"),
                    ProductEntity(name = "Sportcipő", description = "Kényelmes hétköznapi sportcipő", price = 18990.0, imageResName = "sneaker"),
                    ProductEntity(name = "Kapucnis pulóver", description = "Meleg, puha pulóver", price = 8990.0, imageResName = "hoodie"),
                    ProductEntity(name = "Téli kabát", description = "Vastag, bélelt téli kabát", price = 24990.0, imageResName = "jacket"),
                    ProductEntity(name = "Sapka", description = "Kötött téli sapka", price = 2990.0, imageResName = "cap"),
                    ProductEntity(name = "Hátizsák", description = "Mindennapi használatra", price = 10990.0, imageResName = "backpack"),
                )
            )
        }
    }

    suspend fun createProduct(
        name: String,
        description: String,
        price: Double,
        imageSource: String
    ) {
        productDao.insert(
            ProductEntity(
                name = name,
                description = description,
                price = price,
                imageResName = imageSource
            )
        )
    }

    suspend fun updateProduct(
        id: Int,
        name: String,
        description: String,
        price: Double,
        imageSource: String
    ) {
        db.withTransaction {
            productDao.update(
                ProductEntity(
                    id = id,
                    name = name,
                    description = description,
                    price = price,
                    imageResName = imageSource
                )
            )
            cartDao.updateProductSnapshot(id, name, price)
        }
    }

    suspend fun deleteProduct(product: ProductEntity) {
        db.withTransaction {
            cartDao.deleteByProductId(product.id)
            productDao.deleteById(product.id)
        }
    }

    suspend fun addToCart(product: ProductEntity) {
        val existing = cartDao.findByProductId(product.id)
        if (existing == null) {
            cartDao.insert(
                CartItemEntity(
                    productId = product.id,
                    productName = product.name,
                    productPrice = product.price,
                    quantity = 1
                )
            )
        } else {
            cartDao.updateQuantity(existing.id, existing.quantity + 1)
        }
    }

    suspend fun increaseQuantity(item: CartItemEntity) {
        cartDao.updateQuantity(item.id, item.quantity + 1)
    }

    suspend fun decreaseQuantity(item: CartItemEntity) {
        if (item.quantity <= 1) {
            cartDao.deleteById(item.id)
        } else {
            cartDao.updateQuantity(item.id, item.quantity - 1)
        }
    }

    suspend fun checkout(email: String, cartItems: List<CartItemEntity>) {
        db.withTransaction {
            val total = cartItems.sumOf { it.productPrice * it.quantity }

            val summary = cartItems.joinToString(", ") {
                "${it.quantity}x ${it.productName}"
            }

            val orderId = orderDao.insertOrder(
                OrderEntity(
                    email = email,
                    totalAmount = total,
                    createdAt = System.currentTimeMillis(),
                    itemsSummary = summary
                )
            ).toInt()

            val orderItems = cartItems.map { item ->
                OrderItemEntity(
                    orderId = orderId,
                    productId = item.productId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                    lineTotal = item.productPrice * item.quantity
                )
            }

            orderDao.insertOrderItems(orderItems)
            cartDao.clearCart()
        }
    }
}
