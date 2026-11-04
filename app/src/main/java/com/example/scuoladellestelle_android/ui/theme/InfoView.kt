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
import androidx.compose.ui.text.style.TextAlign
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

    // Rilevamento tramite la funzione globale centralizzata in Version.kt
    val isTablet = AppVersion.isTablet(context)

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            // 🎯 MODIFICATO: Altezza smartphone incrementata a 720.dp (+40%) per garantire massima estensione verticale
            modifier = Modifier
                .width(if (isTablet) 550.dp else 320.dp)
                .height(if (isTablet) 340.dp else 720.dp),
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
                                .size(if (isTablet) 65.dp else 55.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Text(
                            text = "Andrea Tombesi",
                            color = Color.White,
                            fontSize = if (isTablet) 20.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sezione Contatti Spaziati e Adattivi
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = if (isTablet) 40.dp else 4.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 16.dp)
                    ) {
                        // 1. RIGA EMAIL
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "✉", color = Color.White, fontSize = if (isTablet) 14.sp else 12.sp)
                            Text(
                                text = "brz.modena@gmail.com",
                                color = Color(0xFF63B3ED),
                                fontSize = if (isTablet) 14.sp else 12.sp,
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

                        // 2. RIGA WORDPRESS
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🌐", color = Color.White, fontSize = if (isTablet) 14.sp else 12.sp)
                            Text(
                                text = "://wordpress.com",
                                color = Color(0xFF63B3ED),
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    try {
                                        val intentBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("https://://wordpress.com"))
                                        context.startActivity(intentBrowser)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }

                        // 3. RIGA GITHUB
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "💻", color = Color.White, fontSize = if (isTablet) 14.sp else 12.sp)
                            Text(
                                text = "://github.com",
                                color = Color(0xFF63B3ED),
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    try {
                                        val intentBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("https://://github.com"))
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
                            Text(text = "📍", color = Color.White, fontSize = if (isTablet) 14.sp else 12.sp)
                            Text(
                                text = "Modena, Italy",
                                color = Color.White,
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Sezione Bassa: Info Dispositivo + Versione + Ringraziamenti
                    Column(modifier = Modifier.fillMaxWidth()) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Scritta permanente discreta a sinistra
                            Text(
                                text = if (isTablet) "Tablet" else "Smartphone",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.40f)
                            )

                            // Versione in basso a destra
                            Text(
                                text = "Versione n. ${AppVersion.CODICE}",
                                fontSize = 11.sp,

                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.50f)
                            )
                        }
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.25f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Special thanks to the code generator and the open-source libraries that made this project possible.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = if (isTablet) 11.sp else 10.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = if (isTablet) 15.sp else 13.sp
                        )
                    }
                }
// Pulsante di Chiusura ad Icona in alto a destra
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