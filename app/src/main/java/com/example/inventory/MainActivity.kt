package com.example.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6750A4),
                    secondary = Color(0xFF625B71),
                    background = Color(0xFFFBF8FF)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainApp(db.inventoryDao())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(dao: InventoryDao) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 盘点, 1: 客户, 2: 报告
    var selectedItemForDetail by remember { mutableStateOf<Item?>(null) }

    if (selectedItemForDetail != null) {
        ItemDetailScreen(
            item = selectedItemForDetail!!,
            dao = dao,
            onBack = { selectedItemForDetail = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Stock1WM 盘点与库存管理系统",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF32105C)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF3EDF7))
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFF3EDF7)) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.List, contentDescription = "库存盘点") },
                        label = { Text("库存盘点") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "客户管理") },
                        label = { Text("客户管理") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Info, contentDescription = "数据报告") },
                        label = { Text("数据报告") }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> InventoryHomeScreen(dao = dao, onItemClick = { selectedItemForDetail = it })
                    1 -> CustomerManagerScreen(dao = dao)
                    2 -> DataReportScreen(dao = dao)
                }
            }
        }
    }
}

// ---------------- 1. 原汁原味盘点首页 ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryHomeScreen(dao: InventoryDao, onItemClick: (Item) -> Unit) {
    val itemList by dao.getAllItems().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredList = itemList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索商品名称或类别...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 标题栏 + 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("商品列表 (${filteredList.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("新增商品")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 商品列表卡片
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredList) { item ->
                val isLowStock = item.quantity <= item.threshold

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLowStock) Color(0xFFFFF0F0) else Color(0xFFF3EDF7)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (isLowStock) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFD32F2F),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "库存预警",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = {
                                    scope.launch { dao.deleteItem(item) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFD32F2F))
                                }
                            }
                        }

                        Text("类别: ${item.category} | 单价: RM ${String.format("%.2f", item.price)}", color = Color.Gray, fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        // 加减号快捷改库存
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val qtyColor = if (isLowStock) Color(0xFFD32F2F) else Color.Black
                            Text("当前库存: ${item.quantity} (阀值: ${item.threshold})", fontWeight = FontWeight.Bold, color = qtyColor)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedIconButton(
                                    onClick = {
                                        if (item.quantity > 0) {
                                            scope.launch {
                                                val newQty = item.quantity - 1
                                                dao.updateItem(item.copy(quantity = newQty))
                                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                                dao.insertLog(StockLog(itemId = item.id, type = "OUT", quantity = 1, date = date, customerName = "快速扣减"))
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

                                Text("${item.quantity}", modifier = Modifier.padding(horizontal = 12.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                                OutlinedIconButton(
                                    onClick = {
                                        scope.launch {
                                            val newQty = item.quantity + 1
                                            dao.updateItem(item.copy(quantity = newQty))
                                            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                            dao.insertLog(StockLog(itemId = item.id, type = "IN", quantity = 1, date = date))
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
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

// ---------------- 2. 客户管理标签页 ----------------
@Composable
fun CustomerManagerScreen(dao: InventoryDao) {
    val customerList by dao.getAllCustomers().collectAsState(initial = emptyList())
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("添加新客户", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("客户姓名") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("联系电话 / 备注") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
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
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) { Text("保存客户") }
            }
        }

        Text("已保存客户列表 (${customerList.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(customerList) { customer ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (customer.phone.isNotBlank()) Text("电话/备注: ${customer.phone}", color = Color.Gray, fontSize = 14.sp)
                        }
                        IconButton(onClick = { scope.launch { dao.deleteCustomer(customer) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 3. 数据报告标签页 ----------------
@Composable
fun DataReportScreen(dao: InventoryDao) {
    val itemList by dao.getAllItems().collectAsState(initial = emptyList())
    val totalTypes = itemList.size
    val totalQty = itemList.sumOf { it.quantity }
    val lowStockCount = itemList.count { it.quantity <= it.threshold }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("库存数据总览", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("商品总种类: $totalTypes 种", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("库存总数量: $totalQty 件", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("预警商品数: $lowStockCount 种", fontSize = 16.sp, color = if (lowStockCount > 0) Color(0xFFD32F2F) else Color.Unspecified)
            }
        }
    }
}

// ---------------- 4. 货品详情与出入库日志（点击卡片查看） ----------------
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
                title = { Text("${item.name} 出入库明细") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("商品名称: ${item.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("当前总库存: ${item.quantity}", fontSize = 16.sp, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
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

            Text("历史变动记录", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
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

// ---------------- 5. 弹窗：添加商品 ----------------
@Composable
fun AddItemDialog(dao: InventoryDao, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("20") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新商品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("商品名称") })
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("单价 (RM)") })
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("初始库存") })
                OutlinedTextField(value = threshold, onValueChange = { threshold = it }, label = { Text("预警阀值") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    scope.launch {
                        val initialQty = qty.toIntOrNull() ?: 0
                        val newItem = Item(
                            name = name,
                            price = price.toDoubleOrNull() ?: 0.0,
                            quantity = initialQty,
                            threshold = threshold.toIntOrNull() ?: 20
                        )
                        dao.insertItem(newItem)

                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        dao.insertLog(StockLog(itemId = newItem.id, type = "IN", quantity = initialQty, date = date))
                        onDismiss()
                    }
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ---------------- 6. 弹窗：出库/入库 ----------------
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
                            label = { Text("选择出货客户") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (customers.isEmpty()) {
                                DropdownMenuItem(text = { Text("暂无客户（请先在“客户管理”中添加）") }, onClick = { expanded = false })
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
            }) { Text("确认提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
