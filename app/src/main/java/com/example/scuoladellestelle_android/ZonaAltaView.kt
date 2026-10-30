package com.example.scuoladellestelle_android

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZonaAltaView(
    isConnected: Boolean,
    isLoading: Boolean,
    statusMessage: String,
    onStartLoading: (String) -> Unit,
    onStatusChange: (Boolean, String, List<StructElencofogliJson>) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("config_rete", Context.MODE_PRIVATE) }
    var showDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val ipSalvato = prefs.getString("ip_pc", NetworkConfig.IP_PC) ?: NetworkConfig.IP_PC
    val portaSalvata = prefs.getString("porta_pc", NetworkConfig.PORTA_PC) ?: NetworkConfig.PORTA_PC

    var inputIp by remember { mutableStateOf(ipSalvato) }
    var inputPorta by remember { mutableStateOf(portaSalvata) }
    var inputNota by remember { mutableStateOf(prefs.getString("nota_utente", "Tablet Aula 1") ?: "Tablet Aula 1") }

    LaunchedEffect(Unit) {
        NetworkConfig.IP_PC = ipSalvato
        NetworkConfig.PORTA_PC = portaSalvata
        inputIp = ipSalvato
        inputPorta = portaSalvata
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .background(Color.White)
            .border(1.dp, Color(0xFFE4E7EC))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { showDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Impostazioni Rete",
                    tint = Color(0xFF475467),
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clickable(enabled = !isLoading) {
                        ApiService.eseguiPingDiVerifica()
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Text(text = if (isConnected) "🟢" else "🔴", fontSize = 14.sp)
                Text(
                    text = if (isConnected) statusMessage.take(35) else "Non Connesso",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected) Color(0xFF12B76A) else Color(0xFFF04438)
                )

                Text(
                    text = "($inputIp)",
                    fontSize = 11.sp,
                    color = Color(0xFF98A2B3)
                )

                Text(
                    text = " - $inputNota",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475467)
                )
            }
        }

        IconButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Informazioni Sessione",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(22.dp)
            )
        }
    }

    // 🎯 RICHIAMO ALLA NUOVA VISTA ESTERNA DEL POPUP INFORMATIVO
    if (showInfoDialog) {
        InfoDialog(onDismissRequest = { showInfoDialog = false })
    }

    // FINESTRA CONFIGURAZIONE RETE
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Configurazione Rete", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = inputIp, onValueChange = { inputIp = it }, label = { Text("IP PC Windows") }, singleLine = true)
                    OutlinedTextField(value = inputPorta, onValueChange = { inputPorta = it }, label = { Text("Porta Server") }, singleLine = true)
                    OutlinedTextField(value = inputNota, onValueChange = { inputNota = it }, label = { Text("Nota Postazione") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().apply {
                        putString("ip_pc", inputIp)
                        putString("porta_pc", inputPorta)
                        putString("nota_utente", inputNota)
                        apply()
                    }
                    NetworkConfig.IP_PC = inputIp
                    NetworkConfig.PORTA_PC = inputPorta
                    showDialog = false
                }) { Text("Salva") }
            }
        )
    }
}
