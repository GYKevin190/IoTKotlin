package com.example.webshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val totalAmount: Double,
    val createdAt: Long,
    val itemsSummary: String
)