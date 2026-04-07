package com.example.webshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val productId: Int,
    val productName: String,
    val productPrice: Double,
    val quantity: Int,
    val lineTotal: Double
)