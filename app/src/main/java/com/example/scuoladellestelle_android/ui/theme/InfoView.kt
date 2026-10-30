package com.example.scuoladellestelle_android

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext

@Composable
fun InfoDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .width(550.dp)
                .height(340.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Immagine di Sfondo
                Image(
                    painter = painterResource(id = R.drawable.sfondo),
                    contentDescription = "Sfondo Informativa",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )

                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Blocco Superiore: Foto e Nome affiancati
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.io),
                            contentDescription = "Foto Andrea Tombesi",
                            modifier = Modifier
                                .size(65.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Text(
                            text = "Andrea Tombesi",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sezione Contatti Spaziati e Corretti
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, top = 8.dp), // Allineamento rientrato a metà foto
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. RIGA EMAIL
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "✉", color = Color.White, fontSize = 14.sp)
                            Text(
                                text = "brz.modena@gmail.com",
                                color = Color(0xFF63B3ED),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    try {
                                        val intentEmail = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:brz.modena@gmail.com")
                                        }
                                        context.startActivity(intentEmail)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }

                        // 2. RIGA WORDPRESS (Modificata con la tua stringa pulita)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🌐", color = Color.White, fontSize = 14.sp)
                            Text(
                                text = "AndreaTombesi.wordpress.com",
                                color = Color(0xFF63B3ED),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    try {
                                        val intentBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("https://andreatombesi.wordpress.com"))
                                        context.startActivity(intentBrowser)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }

                        // 3. RIGA GITHUB (Modificata con il puntamento corretto alla tua repository)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "💻", color = Color.White, fontSize = 14.sp)
                            Text(
                                text = "Github.com/Atombesi",
                                color = Color(0xFF63B3ED),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    try {
                                        val intentBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Atombesi"))
                                        context.startActivity(intentBrowser)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }

                        // 4. RIGA LOCALITÀ
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "📍", color = Color.White, fontSize = 14.sp)
                            Text(
                                text = "Modena, Italy",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Sezione Bassa: Ringraziamenti
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.25f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Special thanks to the code generator and the open-source libraries that made this project possible.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Bottone ad incrocio (X)
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi Finestra",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
