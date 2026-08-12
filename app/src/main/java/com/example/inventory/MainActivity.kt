package com.example.inventory

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ==========================================
// 1. Room 数据库架构 (Database Architecture)
// ==========================================

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val quantity: Int,
    val minThreshold: Int = 5,
    val unitPrice: Double = 0.0
)

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY id DESC")
    fun getAllItems(): kotlinx.coroutines.flow.Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<Int>)
}

@Database(entities = [InventoryItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 2. ViewModel 控制器 (State Management)
// ==========================================

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).inventoryDao()

    val itemList: StateFlow<List<InventoryItem>> = dao.getAllItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            // 如果数据库为空，预填初始演示数据
            dao.getAllItems().collect { list ->
                if (list.isEmpty()) {
                    dao.insertItem(InventoryItem(name = "可口可乐 (Coca Cola)", category = "饮料", quantity = 18, minThreshold = 10, unitPrice = 2.5))
                    dao.insertItem(InventoryItem(name = "鲜牛奶 (Fresh Milk)", category = "冷藏", quantity = 3, minThreshold = 5, unitPrice = 6.5))
                    dao.insertItem(InventoryItem(name = "吐司面包 (Bread)", category = "食品", quantity = 2, minThreshold = 5, unitPrice = 4.0))
                }
            }
        }
    }

    fun addItem(name: String, category: String, quantity: Int, minThreshold: Int, unitPrice: Double) {
        viewModelScope.launch {
            dao.insertItem(InventoryItem(name = name, category = category, quantity = quantity, minThreshold = minThreshold, unitPrice = unitPrice))
        }
    }

    fun updateItem(item: InventoryItem) {
        viewModelScope.launch {
            dao.updateItem(item)
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            dao.deleteItem(item)
        }
    }

    fun deleteBatch(ids: List<Int>) {
        viewModelScope.launch {
            dao.deleteItemsByIds(ids)
        }
    }
}

// ==========================================
// 3. Activity 入口 (UI Component)
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6750A4),
                    secondary = Color(0xFF625B71),
                    tertiary = Color(0xFF7D5260)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainInventoryApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainInventoryApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val viewModel: InventoryViewModel = viewModel()
    val itemList by viewModel.itemList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Stock1WM 盘点与库存管理系统", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "库存盘点") },
                    label = { Text("库存盘点") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "数据报告") },
                    label = { Text("数据报告") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                InventoryCheckTab(itemList = itemList, viewModel = viewModel)
            } else {
                InventoryReportTab(itemList = itemList)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCheckTab(itemList: List<InventoryItem>, viewModel: InventoryViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    
    // 批量删除与防误触删除状态 (Selection & Deletion Protection)
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<Int>() }
    var itemToDeleteSingle by remember { mutableStateOf<InventoryItem?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val filteredList = itemList.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.category.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("搜索商品名称或类别...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 控制栏：新增与批量选择控制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "商品列表 (${filteredList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                TextButton(
                    onClick = { 
                        isSelectionMode = !isSelectionMode
                        selectedItemIds.clear()
                    }
                ) {
                    Text(if (isSelectionMode) "取消多选" else "批量删除")
                }
                
                if (!isSelectionMode) {
                    Button(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增商品")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("未检索到符合条件的商品记录", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedItemIds.contains(item.id),
                        onToggleSelect = {
                            if (selectedItemIds.contains(item.id)) {
                                selectedItemIds.remove(item.id)
                            } else {
                                selectedItemIds.add(item.id)
                            }
                        },
                        onQuantityChange = { delta ->
                            val newQty = (item.quantity + delta).coerceAtLeast(0)
                            viewModel.updateItem(item.copy(quantity = newQty))
                        },
                        onEdit = { itemToEdit = item },
                        onDeleteSingle = { itemToDeleteSingle = item }
                    )
                }
            }
        }

        // 批量删除底部执行条
        if (isSelectionMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { if (selectedItemIds.isNotEmpty()) showBatchDeleteConfirm = true },
                enabled = selectedItemIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确定删除已选中的 ${selectedItemIds.size} 项商品")
            }
        }
    }

    // 弹窗 1：新增 / 编辑弹窗
    if (showAddDialog || itemToEdit != null) {
        ItemFormDialog(
            initialItem = itemToEdit,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { name, category, qty, threshold, price ->
                if (itemToEdit != null) {
                    viewModel.updateItem(
                        itemToEdit!!.copy(
                            name = name,
                            category = category,
                            quantity = qty,
                            minThreshold = threshold,
                            unitPrice = price
                        )
                    )
                } else {
                    viewModel.addItem(name, category, qty, threshold, price)
                }
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    // 弹窗 2：单项删除二次确认 (Single Item Delete Protection Dialog)
    if (itemToDeleteSingle != null) {
        AlertDialog(
            onDismissRequest = { itemToDeleteSingle = null },
            title = { Text("确认删除商品？") },
            text = { Text("您确定要删除“${itemToDeleteSingle?.name}”吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDeleteSingle?.let { viewModel.deleteItem(it) }
                        itemToDeleteSingle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("确定删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteSingle = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 弹窗 3：批量删除二次确认 (Batch Delete Confirmation Dialog)
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("确认批量删除？") },
            text = { Text("您确定要彻底删除选中的 ${selectedItemIds.size} 项商品吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBatch(selectedItemIds.toList())
                        selectedItemIds.clear()
                        isSelectionMode = false
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("确认批量删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDeleteSingle: () -> Unit
) {
    val isLowStock = item.quantity <= item.minThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isSelectionMode) Modifier.clickable { onToggleSelect() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) Color(0xFFFFF3F3) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLowStock) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFFD32F2F),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "库存预警",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "类别: ${item.category} | 单价: RM ${String.format("%.2f", item.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    if (!isSelectionMode) {
                        Row {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, contentDescription = "修改", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onDeleteSingle) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFD32F2F))
                            }
                        }
                    } else {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "当前库存: ${item.quantity} (阀值: ${item.minThreshold})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLowStock) Color(0xFFD32F2F) else Color.Unspecified
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedIconButton(
                            onClick = { onQuantityChange(-1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${item.quantity}",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedIconButton(
                            onClick = { onQuantityChange(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryReportTab(itemList: List<InventoryItem>) {
    val totalTypes = itemList.size
    val totalQuantity = itemList.sumOf { it.quantity }
    val totalValue = itemList.sumOf { it.quantity * it.unitPrice }
    val lowStockItems = itemList.filter { it.quantity <= it.minThreshold }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📊 库存数据与分析报告",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportCard(
                title = "商品种类",
                value = "$totalTypes 种",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFE8F0FE)
            )
            ReportCard(
                title = "总库存量",
                value = "$totalQuantity 件",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFE6F4EA)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportCard(
                title = "低库存需补货",
                value = "${lowStockItems.size} 项",
                modifier = Modifier.weight(1f),
                bgColor = if (lowStockItems.isNotEmpty()) Color(0xFFFCE8E6) else Color(0xFFF1F3F4)
            )
            ReportCard(
                title = "预估总价值",
                value = "RM ${String.format("%.2f", totalValue)}",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFFEF7E0)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "⚠️ 需优先补货明细清单",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (lowStockItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA))
            ) {
                Text(
                    text = "✅ 目前所有商品库存充裕，无需补货！",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF137333)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lowStockItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = "剩余: ${item.quantity} (警告阀值: ${item.minThreshold})",
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(title: String, value: String, modifier: Modifier = Modifier, bgColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ItemFormDialog(
    initialItem: InventoryItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, qty: Int, threshold: Int, price: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "通用") }
    var qtyText by remember { mutableStateOf(initialItem?.quantity?.toString() ?: "1") }
    var thresholdText by remember { mutableStateOf(initialItem?.minThreshold?.toString() ?: "5") }
    var priceText by remember { mutableStateOf(initialItem?.unitPrice?.toString() ?: "0.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "新增商品" else "修改商品信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("商品名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("商品类别") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("当前数量") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("预警触发阀值") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("单价 (RM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyText.toIntOrNull() ?: 0
                    val t = thresholdText.toIntOrNull() ?: 5
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSave(name, category, q, t, p)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
