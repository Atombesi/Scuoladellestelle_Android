package com.example.scuoladellestelle_android

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    var inputIp by remember { mutableStateOf(prefs.getString("ip_pc", NetworkConfig.IP_PC) ?: NetworkConfig.IP_PC) }
    var inputPorta by remember { mutableStateOf(prefs.getString("porta_pc", NetworkConfig.PORTA_PC) ?: NetworkConfig.PORTA_PC) }
    var inputNota by remember { mutableStateOf(prefs.getString("nota_utente", "Tablet Aula 1") ?: "Tablet Aula 1") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp) // 🎯 ALTEZZA DIMEZZATA FISSA E PERMANENTE
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

            // Barra info concentrata su un'unica riga compattata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                // 🎯 MODIFICA RICONNESIONE SILENZIOSA: Se tocchi il pallino rosso, l'applicazione
                // riapre il tubo WebSocket facendo l'handshake, ma passa richiedeLista = false.
                // In questo modo il tunnel radio torna verde in background senza azzerare la griglia o il foglio aperto!
                modifier = Modifier
                    .clickable(enabled = !isLoading) {
                        if (!isConnected) {
                            onStartLoading("Ripristino canale radio in corso...")
                            ApiService.connettiAlServer(
                                context = context,
                                notaUtente = inputNota,
                                richiedeLista = false, // 🎯 FORZA IL RECUPERO SENZA CHIEDERE L'ELENCO
                                onStatoConnessione = { connesso, messaggio ->
                                    onStatusChange(connesso, messaggio, emptyList())
                                },
                                onElencoFogli = { _ -> },
                                onFoglioDati = { _ -> }
                            )
                        } else {
                            // Se era già connesso, esegue il normale ping di controllo
                            ApiService.eseguiPingDiVerifica()
                        }
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
                    text = "(${NetworkConfig.IP_PC})",
                    fontSize = 11.sp,
                    color = Color(0xFF98A2B3)
                )
            }
        }

        // 🎯 IL PULSANTE DI CONNETTI GENERALE: Mantiene il comportamento pulito standard (richiede l'elenco da zero)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onStartLoading("Connessione ed estrazione elenco in corso...")

                    ApiService.connettiAlServer(
                        context = context,
                        notaUtente = inputNota,
                        richiedeLista = true, // 🎯 SCARICA L'ELENCO COMPLETO DA ZERO
                        onStatoConnessione = { connesso, messaggio ->
                            onStatusChange(connesso, messaggio, emptyList())
                        },
                        onElencoFogli = { listaRicevuta ->
                            onStatusChange(true, "Elenco caricato con successo.", listaRicevuta)
                        },
                        onFoglioDati = { _ -> }
                    )
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(6.dp),
                enabled = !isLoading
            ) {
                Text("Connetti", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

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
