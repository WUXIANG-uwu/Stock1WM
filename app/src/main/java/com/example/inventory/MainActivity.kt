package com.example.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var db: InventoryDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = InventoryDatabase.getDatabase(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(db.inventoryDao())
                }
            }
        }
    }
}

@Composable
fun MainApp(dao: InventoryDao) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showCustomerManager by remember { mutableStateOf(false) }

    when {
        selectedItem != null -> {
            ItemDetailScreen(
                item = selectedItem!!,
                dao = dao,
                onBack = { selectedItem = null }
            )
        }
        showCustomerManager -> {
            CustomerManagerScreen(
                dao = dao,
                onBack = { showCustomerManager = false }
            )
        }
        else -> {
            HomeScreen(
                dao = dao,
                onItemClick = { item -> selectedItem = item },
                onOpenCustomers = { showCustomerManager = true }
            )
        }
    }
}

// ---------------- 1. 原汁原味的主界面 ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(dao: InventoryDao, onItemClick: (Item) -> Unit, onOpenCustomers: () -> Unit) {
    val itemList by dao.getAllItems().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock1WM", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenCustomers) {
                        Icon(Icons.Default.Person, contentDescription = "客户名单")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加货品")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (itemList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无货品，请点击右下角 + 添加", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(itemList) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { onItemClick(item) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("价格: $${item.price}", fontSize = 14.sp, color = Color.Gray)
                                }
                                Text("库存: ${item.quantity}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(dao = dao, onDismiss = { showAddDialog = false })
    }
}

// ---------------- 2. 客户名单界面（右上角图标点击进入） ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagerScreen(dao: InventoryDao, onBack: () -> Unit) {
    val customerList by dao.getAllCustomers().collectAsState(initial = emptyList())
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("客户名单管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("输入新客户", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("客户姓名") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("联系电话 / 备注") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                scope.launch {
                                    dao.insertCustomer(Customer(name = nameInput, phone = phoneInput))
                                    nameInput = ""
                                    phoneInput = ""
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("保存客户")
                    }
                }
            }

            Text("已保存客户列表", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(customerList) { customer ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(customer.name, fontWeight = FontWeight.Medium)
                            Text(customer.phone, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 3. 点击货品卡片查看的出入库明细 ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(item: Item, dao: InventoryDao, onBack: () -> Unit) {
    val logs by dao.getLogsForItem(item.id).collectAsState(initial = emptyList())
    val customerList by dao.getAllCustomers().collectAsState(initial = emptyList())
    var showLogDialog by remember { mutableStateOf(false) }
    var isStockIn by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${item.name} 明细") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前库存: ${item.quantity}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { isStockIn = true; showLogDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                            Text("货品入库")
                        }
                        Button(onClick = { isStockIn = false; showLogDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                            Text("货品出库")
                        }
                    }
                }
            }

            Text("出入库记录明细", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(logs) { log ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val tag = if (log.type == "IN") "[入库]" else "[出库]"
                                val color = if (log.type == "IN") Color(0xFF2E7D32) else Color(0xFFC62828)
                                Text("$tag ${log.quantity} 件", fontWeight = FontWeight.Bold, color = color)
                                Text(log.date, fontSize = 12.sp, color = Color.Gray)
                            }
                            if (log.type == "OUT" && log.customerName.isNotBlank()) {
                                Text("出货给客户: ${log.customerName}", fontSize = 14.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        StockActionDialog(
            item = item,
            isStockIn = isStockIn,
            customers = customerList,
            dao = dao,
            onDismiss = { showLogDialog = false }
        )
    }
}

// ---------------- 4. 对话框：添加货品 ----------------
@Composable
fun AddItemDialog(dao: InventoryDao, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新货品") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("货品名称") })
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("价格") })
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("初始库存") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    scope.launch {
                        val newQuantity = qty.toIntOrNull() ?: 0
                        val newItem = Item(name = name, price = price.toDoubleOrNull() ?: 0.0, quantity = newQuantity)
                        dao.insertItem(newItem)

                        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        dao.insertLog(StockLog(itemId = newItem.id, type = "IN", quantity = newQuantity, date = currentDate))
                        onDismiss()
                    }
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ---------------- 5. 对话框：出库/入库 ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockActionDialog(
    item: Item,
    isStockIn: Boolean,
    customers: List<Customer>,
    dao: InventoryDao,
    onDismiss: () -> Unit
) {
    var qtyInput by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isStockIn) "货品入库" else "货品出库") },
        text = {
            Column {
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { qtyInput = it },
                    label = { Text("变动数量") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isStockIn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = if (selectedCustomer.isEmpty()) "点击选择客户" else selectedCustomer,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("出货给谁 (客户)") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (customers.isEmpty()) {
                                DropdownMenuItem(text = { Text("暂无客户，请先在主页右上角添加") }, onClick = { expanded = false })
                            } else {
                                customers.forEach { customer ->
                                    DropdownMenuItem(
                                        text = { Text(customer.name) },
                                        onClick = {
                                            selectedCustomer = customer.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val qty = qtyInput.toIntOrNull() ?: 0
                if (qty > 0) {
                    scope.launch {
                        val updatedQty = if (isStockIn) item.quantity + qty else (item.quantity - qty).coerceAtLeast(0)
                        dao.updateItem(item.copy(quantity = updatedQty))

                        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        val logType = if (isStockIn) "IN" else "OUT"
                        dao.insertLog(
                            StockLog(
                                itemId = item.id,
                                type = logType,
                                quantity = qty,
                                date = currentDate,
                                customerName = if (!isStockIn) selectedCustomer else ""
                            )
                        )
                        onDismiss()
                    }
                }
            }) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
