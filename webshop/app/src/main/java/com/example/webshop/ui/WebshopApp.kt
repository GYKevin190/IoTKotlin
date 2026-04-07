package com.example.webshop.ui

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.webshop.data.entity.CartItemEntity
import com.example.webshop.data.entity.OrderEntity
import com.example.webshop.data.entity.ProductEntity
import com.example.webshop.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    PRODUCTS, CART, ORDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebshopApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.PRODUCTS) }

    val products = viewModel.products.collectAsState().value
    val cartItems = viewModel.cartItems.collectAsState().value
    val orders = viewModel.orders.collectAsState().value

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Webshop") }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.PRODUCTS,
                    onClick = { currentScreen = Screen.PRODUCTS },
                    label = { Text("Termékek") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Termékek"
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.CART,
                    onClick = { currentScreen = Screen.CART },
                    label = { Text("Kosár") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge {
                                        Text(cartCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Kosár"
                            )
                        }
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.ORDERS,
                    onClick = { currentScreen = Screen.ORDERS },
                    label = { Text("Rendelések") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Rendelések"
                        )
                    }
                )
            }
        }
    ) { padding ->
        when (currentScreen) {
            Screen.PRODUCTS -> ProductScreen(
                products = products,
                onAddToCart = viewModel::addToCart,
                modifier = Modifier.padding(padding)
            )

            Screen.CART -> CartScreen(
                cartItems = cartItems,
                onIncrease = viewModel::increaseQuantity,
                onDecrease = viewModel::decreaseQuantity,
                onCheckout = { email ->
                    viewModel.checkout(email)
                    scope.launch {
                        snackbarHostState.showSnackbar("Sikeres rendelés leadva.")
                    }
                    currentScreen = Screen.ORDERS
                },
                modifier = Modifier.padding(padding)
            )

            Screen.ORDERS -> OrdersScreen(
                orders = orders,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun ProductScreen(
    products: List<ProductEntity>,
    onAddToCart: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchText) {
        if (searchText.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(searchText, ignoreCase = true) ||
                        it.description.contains(searchText, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Keresés") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredProducts.isEmpty()) {
            Text("Nincs találat.")
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredProducts) { product ->
                ProductCard(
                    product = product,
                    onAddToCart = onAddToCart
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onAddToCart: (ProductEntity) -> Unit
) {
    val context = LocalContext.current

    val imageResId = remember(product.imageResName) {
        context.resources.getIdentifier(
            product.imageResName,
            "drawable",
            context.packageName
        )
    }

    Log.d("IMG", "name=${product.imageResName}, id=$imageResId")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Text("Kép nem található: ${product.imageResName}")
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(text = product.name)
            Text(text = product.description)
            Text(text = "${product.price} Ft")

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onAddToCart(product) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kosárba")
            }
        }
    }
}

@Composable
fun CartScreen(
    cartItems: List<CartItemEntity>,
    onIncrease: (CartItemEntity) -> Unit,
    onDecrease: (CartItemEntity) -> Unit,
    onCheckout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCheckoutDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    val isPhoneValid = phone.all { it.isDigit() } && phone.isNotEmpty()
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()

    val isFormValid = name.isNotBlank() &&
            address.isNotBlank() &&
            isPhoneValid &&
            isEmailValid

    val total = cartItems.sumOf { it.productPrice * it.quantity }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (cartItems.isEmpty()) {
            Text("A kosár üres.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.productName)
                            Text("Ár: ${item.productPrice} Ft")
                            Text("Mennyiség: ${item.quantity}")
                            Text("Összesen: ${item.productPrice * item.quantity} Ft")

                            Spacer(Modifier.height(8.dp))

                            Row {
                                Button(onClick = { onDecrease(item) }) {
                                    Text("-")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { onIncrease(item) }) {
                                    Text("+")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Teljes összeg: $total Ft")
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showCheckoutDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Checkout")
            }
        }
    }

    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("Megrendelés véglegesítése") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Név") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Szállítási cím") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() }) {
                                phone = it
                            }
                        },
                        label = { Text("Telefonszám") },
                        isError = phone.isNotEmpty() && !isPhoneValid,
                        supportingText = {
                            if (phone.isNotEmpty() && !isPhoneValid) {
                                Text("Csak számokat adhatsz meg.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Email") },
                        isError = email.isNotEmpty() && !isEmailValid,
                        supportingText = {
                            if (email.isNotEmpty() && !isEmailValid) {
                                Text("Adj meg egy érvényes email címet.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isFormValid) {
                            onCheckout(email)

                            name = ""
                            address = ""
                            phone = ""
                            email = ""

                            showCheckoutDialog = false
                        }
                    },
                    enabled = isFormValid
                ) {
                    Text("Mentés")
                }
            },
            dismissButton = {
                Button(onClick = { showCheckoutDialog = false }) {
                    Text("Mégse")
                }
            }
        )
    }
}

@Composable
fun OrdersScreen(
    orders: List<OrderEntity>,
    modifier: Modifier = Modifier
) {
    if (orders.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Még nincs rendelés.")
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders) { order ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("ID: ${order.id}")
                    Text("Email: ${order.email}")
                    Text("Termékek: ${order.itemsSummary}")
                    Text("Összeg: ${order.totalAmount} Ft")
                    Text("Dátum: ${formatOrderDate(order.createdAt)}")
                }
            }
        }
    }
}

fun formatOrderDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}