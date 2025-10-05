package com.pavle.lostandfound.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onAddItem: (isLost: Boolean, category: String, description: String, secretDetails: String, imageUri: Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLost by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var secretDetails by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val categories = listOf("Ključevi", "Telefon", "Novčanik", "Ljubimac", "Ostalo")
    var isCategoryExpanded by remember { mutableStateOf(false) }

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeight = with(density) {
        windowInfo.containerSize.height.toDp()
    }
    val sheetHeight = screenHeight * 0.7f

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .heightIn(max = sheetHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Dodaj novi predmet", style = MaterialTheme.typography.headlineSmall)
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    RadioButton(selected = isLost, onClick = { isLost = true })
                    Text("Izgubljeno")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !isLost, onClick = { isLost = false })
                    Text("Pronađeno")
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth(),
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategorija") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis (vidljiv svima)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            item {
                OutlinedTextField(
                    value = secretDetails,
                    onValueChange = { secretDetails = it },
                    label = { Text("Skriveni detalji (za potvrdu)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Dodaj fotografiju")
                    }
                    imageUri?.let {
                        Image(
                            painter = rememberAsyncImagePainter(model = it),
                            contentDescription = "Izabrana slika",
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onAddItem(isLost, selectedCategory, description, secretDetails, imageUri)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedCategory.isNotBlank() && description.isNotBlank()
                ) {
                    Text("Sačuvaj")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}