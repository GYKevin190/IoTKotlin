package com.example.webshop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.webshop.data.entity.OrderEntity
import com.example.webshop.data.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>
}