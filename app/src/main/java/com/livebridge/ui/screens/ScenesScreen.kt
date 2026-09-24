package com.livebridge.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.R
import com.livebridge.studio.Scene
import com.livebridge.studio.StudioStore
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav

@Composable
fun ScenesScreen(
    scenes: List<Scene>,
    currentSceneId: String,
    store: StudioStore,
    onBack: () -> Unit,
    onSelectNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sceneMenuOpenForId by remember { mutableStateOf<String?>(null) }
    var renameDialogOpenForId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    val thumbnailDrawables = listOf(
        R.drawable.scene_1,
        R.drawable.scene_2,
        R.drawable.scene_3,
        R.drawable.scene_4
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top App Bar: < Scènes   +
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text = "Scènes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = { store.addScene("Scène ${scenes.size + 1}") }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter une scène",
                    tint = Color.White
                )
            }
        }

        // List of Scenes
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(scenes) { index, scene ->
                val isSelected = scene.id == currentSceneId
                val thumbRes = thumbnailDrawables[index % thumbnailDrawables.size]

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandBlue else Color(0xFF1F2D47),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { store.selectScene(scene.id) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 16:9 Thumbnail Image
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            Image(
                                painter = painterResource(id = thumbRes),
                                contentDescription = scene.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Text(
                            text = scene.name,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        // 3 Dots Menu Button
                        Box {
                            IconButton(onClick = { sceneMenuOpenForId = scene.id }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options de scène",
                                    tint = TextSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = sceneMenuOpenForId == scene.id,
                                onDismissRequest = { sceneMenuOpenForId = null },
                                modifier = Modifier.background(SurfaceCardLight)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sélectionner", color = Color.White) },
                                    onClick = {
                                        store.selectScene(scene.id)
                                        sceneMenuOpenForId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Renommer", color = Color.White) },
                                    onClick = {
                                        renameDialogOpenForId = scene.id
                                        renameText = scene.name
                                        sceneMenuOpenForId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dupliquer", color = Color.White) },
                                    onClick = {
                                        store.duplicateScene(scene.id)
                                        sceneMenuOpenForId = null
                                    }
                                )
                                if (scenes.size > 1) {
                                    DropdownMenuItem(
                                        text = { Text("Supprimer", color = LiveRed) },
                                        onClick = {
                                            store.deleteScene(scene.id)
                                            sceneMenuOpenForId = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rename Dialog
        if (renameDialogOpenForId != null) {
            AlertDialog(
                onDismissRequest = { renameDialogOpenForId = null },
                title = { Text("Renommer la scène", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            renameDialogOpenForId?.let { id ->
                                store.renameScene(id, renameText)
                            }
                            renameDialogOpenForId = null
                        }
                    ) {
                        Text("Enregistrer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogOpenForId = null }) {
                        Text("Annuler")
                    }
                },
                containerColor = SurfaceCardLight
            )
        }

        // Bottom Navigation Bar
        LiveBridgeBottomNav(
            currentSection = "Scènes",
            onSelectSection = onSelectNav
        )
    }
}
