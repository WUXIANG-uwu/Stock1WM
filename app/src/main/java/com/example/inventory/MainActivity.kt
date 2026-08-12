package com.example.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 数据模型 (Data Model [数据模型])
data class InventoryItem(val name: String, val quantity: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InventoryScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    // 状态管理 (State Management [状态管理])
    var nameInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("") }
    val itemList = remember { mutableStateListOf<InventoryItem>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock1WM 库存管理系统") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // 输入区：商品名称与数量
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("商品名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = quantityInput,
                onValueChange = { quantityInput = it },
                label = { Text("库存数量") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 按钮：添加库存
            Button(
                onClick = {
                    val qty = quantityInput.toIntOrNull()
                    if (nameInput.isNotBlank() && qty != null) {
                        itemList.add(InventoryItem(nameInput, qty))
                        nameInput = ""
                        quantityInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("新增商品库存")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "当前库存列表：", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // 列表区：展示添加的商品
            LazyColumn {
                items(itemList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "数量: ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
