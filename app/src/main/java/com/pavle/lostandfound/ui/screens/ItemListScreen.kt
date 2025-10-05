package com.pavle.lostandfound.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.pavle.lostandfound.model.Item
import com.pavle.lostandfound.ui.viewmodel.MapViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    mapViewModel: MapViewModel,
    onNavigateBack: () -> Unit
) {
    val allItems by mapViewModel.items.collectAsState()
    var filterByType by remember { mutableStateOf<Boolean?>(null) }
    var showMyItems by remember { mutableStateOf(false) }
    var filterByCategory by remember { mutableStateOf<String?>(null) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Ključevi", "Telefon", "Novčanik", "Ljubimac", "Ostalo")
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    val filteredItems = remember(allItems, filterByType, showMyItems, filterByCategory) {
        var itemsToShow = allItems
        if (filterByType != null) {
            itemsToShow = itemsToShow.filter { it.lostStatus == filterByType }
        }
        if (showMyItems) {
            itemsToShow = itemsToShow.filter { it.userId == userId }
        }
        if (filterByCategory != null) {
            itemsToShow = itemsToShow.filter { it.category == filterByCategory }
        }
        itemsToShow
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista predmeta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterByType == true,
                    onClick = { filterByType = if (filterByType == true) null else true },
                    label = { Text("Izgubljeno") }
                )
                FilterChip(
                    selected = filterByType == false,
                    onClick = { filterByType = if (filterByType == false) null else false },
                    label = { Text("Pronađeno") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showMyItems,
                        onCheckedChange = { showMyItems = it }
                    )
                    Text("Moji predmeti")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isCategoryExpanded,
                onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
            ) {
                TextField(
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    value = filterByCategory ?: "Sve kategorije",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtriraj po kategoriji") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = isCategoryExpanded,
                    onDismissRequest = { isCategoryExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sve kategorije") },
                        onClick = {
                            filterByCategory = null
                            isCategoryExpanded = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                filterByCategory = category
                                isCategoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nema predmeta koji odgovaraju filteru.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(item: Item) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium
            )
            if (item.secretDetails.isNotBlank() && item.userId == currentUserId) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Skriveni detalj: ${item.secretDetails}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val statusText = if (item.lostStatus) {
                "Status: Izgubljeno"
            } else {
                "Status: Pronađeno"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall
            )
            val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
            Text(
                text = "Vreme: ${sdf.format(item.timestamp.toDate())}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}