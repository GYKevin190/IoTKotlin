package com.example.webshop.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.webshop.data.entity.CartItemEntity
import com.example.webshop.data.entity.OrderEntity
import com.example.webshop.data.entity.ProductEntity
import com.example.webshop.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AdminScreen {
    PRODUCTS,
    CART,
    ORDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    fun AdminWebshopApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(AdminScreen.PRODUCTS) }

    val products = viewModel.products.collectAsState().value
    val cartItems = viewModel.cartItems.collectAsState().value
    val orders = viewModel.orders.collectAsState().value

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

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
            val cartCount = cartItems.sumOf { it.quantity }

            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == AdminScreen.PRODUCTS,
                    onClick = { currentScreen = AdminScreen.PRODUCTS },
                    label = { Text("Termekek") },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Termekek"
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == AdminScreen.CART,
                    onClick = { currentScreen = AdminScreen.CART },
                    label = { Text("Kosar") },
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
                                contentDescription = "Kosar"
                            )
                        }
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == AdminScreen.ORDERS,
                    onClick = { currentScreen = AdminScreen.ORDERS },
                    label = { Text("Rendelesek") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Rendelesek"
                        )
                    }
                )
            }
        }
    ) { padding ->
        when (currentScreen) {
            AdminScreen.PRODUCTS -> AdminProductScreen(
                products = products,
                onAddToCart = viewModel::addToCart,
                onCreateProduct = { name, description, price, imageSource ->
                    viewModel.createProduct(name, description, price, imageSource)
                    showMessage("A termek mentve lett.")
                },
                onUpdateProduct = { id, name, description, price, imageSource ->
                    viewModel.updateProduct(id, name, description, price, imageSource)
                    showMessage("A termek frissitve lett.")
                },
                onDeleteProduct = { product ->
                    viewModel.deleteProduct(product)
                    showMessage("A termek torolve lett.")
                },
                modifier = Modifier.padding(padding)
            )

            AdminScreen.CART -> AdminCartScreen(
                cartItems = cartItems,
                onIncrease = viewModel::increaseQuantity,
                onDecrease = viewModel::decreaseQuantity,
                onCheckout = { email ->
                    viewModel.checkout(email)
                    showMessage("Sikeres rendeles leadva.")
                    currentScreen = AdminScreen.ORDERS
                },
                modifier = Modifier.padding(padding)
            )

            AdminScreen.ORDERS -> AdminOrdersScreen(
                orders = orders,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun AdminProductScreen(
    products: List<ProductEntity>,
    onAddToCart: (ProductEntity) -> Unit,
    onCreateProduct: (String, String, Double, String) -> Unit,
    onUpdateProduct: (Int, String, String, Double, String) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

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
            label = { Text("Kereses") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                productToEdit = null
                showEditor = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Uj termek letrehozasa")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredProducts.isEmpty()) {
            Text("Nincs talalat.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    AdminProductCard(
                        product = product,
                        onAddToCart = onAddToCart,
                        onEdit = {
                            productToEdit = product
                            showEditor = true
                        },
                        onDelete = {
                            productToDelete = product
                        }
                    )
                }
            }
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            initialProduct = productToEdit,
            onDismiss = {
                showEditor = false
                productToEdit = null
            },
            onSave = { name, description, price, imageSource ->
                val editing = productToEdit
                if (editing == null) {
                    onCreateProduct(name, description, price, imageSource)
                } else {
                    onUpdateProduct(editing.id, name, description, price, imageSource)
                }
                showEditor = false
                productToEdit = null
            }
        )
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Termek torlese") },
            text = {
                Text("Biztosan torolni szeretned ezt a termeket? A kosarbol is kikerul.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(product)
                        productToDelete = null
                    }
                ) {
                    Text("Torles")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToDelete = null }) {
                    Text("Megse")
                }
            }
        )
    }
}

@Composable
private fun AdminProductCard(
    product: ProductEntity,
    onAddToCart: (ProductEntity) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProductImagePreview(
                imageSource = product.imageResName,
                contentDescription = product.name
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = product.name)
            Text(text = product.description)
            Text(text = "${product.price} Ft")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onAddToCart(product) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kosárba")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Módosítás")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Törlés")
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initialProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String) -> Unit
) {
    val context = LocalContext.current

    var name by remember(initialProduct) { mutableStateOf(initialProduct?.name.orEmpty()) }
    var description by remember(initialProduct) {
        mutableStateOf(initialProduct?.description.orEmpty())
    }
    var priceText by remember(initialProduct) {
        mutableStateOf(initialProduct?.price?.toString().orEmpty())
    }
    var imageSource by remember(initialProduct) {
        mutableStateOf(initialProduct?.imageResName.orEmpty())
    }

    val normalizedPrice = priceText.replace(',', '.').toDoubleOrNull()
    val isFormValid = name.isNotBlank() && description.isNotBlank() && normalizedPrice != null

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageSource = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialProduct == null) {
                    "Új termék"
                } else {
                    "Termék szerkesztése"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                ProductImagePreview(
                    imageSource = imageSource,
                    contentDescription = name.ifBlank { "Termék kép" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nev") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Leírás") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Ár (Ft)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        if (priceText.isNotBlank() && normalizedPrice == null) {
                            Text("Kérlek érvényes számot adj meg.")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (imageSource.isBlank()) {
                            "Kép kiválasztása"
                        } else {
                            "Kép cseréje"
                        }
                    )
                }

                if (imageSource.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { imageSource = "" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kép eltávolítása")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        description.trim(),
                        normalizedPrice ?: 0.0,
                        imageSource
                    )
                },
                enabled = isFormValid
            ) {
                Text("Mentés")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}

@Composable
private fun ProductImagePreview(
    imageSource: String,
    contentDescription: String
) {
    val context = LocalContext.current

    val localBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = imageSource
    ) {
        value = null
        if (imageSource.startsWith("content://")) {
            value = runCatching {
                val uri = Uri.parse(imageSource)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, uri)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            }.getOrNull()
        }
    }

    val imageResId = remember(imageSource) {
        if (imageSource.isNotBlank() && !imageSource.startsWith("content://")) {
            context.resources.getIdentifier(
                imageSource,
                "drawable",
                context.packageName
            )
        } else {
            0
        }
    }

    when {
        imageSource.startsWith("content://") && localBitmap != null -> {
            Image(
                bitmap = localBitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        }

        imageResId != 0 -> {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Fit
            )
        }

        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (imageSource.isBlank()) {
                        "Nincs kép kiválasztva"
                    } else {
                        "A kép nem jeleníthető meg"
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminCartScreen(
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
                items(cartItems, key = { it.id }) { item ->
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
                Text("Rendelés véglegesítése")
            }
        }
    }

    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("Rendelés véglegesítése") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nev") },
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
                            if (it.all { char -> char.isDigit() }) {
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
                                Text("Adj meg érvényes email címet.")
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
                OutlinedButton(onClick = { showCheckoutDialog = false }) {
                    Text("Megse")
                }
            }
        )
    }
}

@Composable
private fun AdminOrdersScreen(
    orders: List<OrderEntity>,
    modifier: Modifier = Modifier
) {
    if (orders.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Még nincs rendelése.")
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("ID: ${order.id}")
                    Text("Email: ${order.email}")
                    Text("Termékek: ${order.itemsSummary}")
                    Text("Összeg: ${order.totalAmount} Ft")
                    Text("Dátum: ${formatAdminOrderDate(order.createdAt)}")
                }
            }
        }
    }
}

private fun formatAdminOrderDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
