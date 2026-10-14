package com.example.scuoladellestelle_android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZonaAltaView(
    isConnected: Boolean,
    isLoading: Boolean,
    onStartLoading: (String) -> Unit,
    onStatusChange: (Boolean, String, List<StructElencofogliJson>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.White)
            .border(1.dp, Color(0xFFE4E7EC))
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sinistra: Stato Connessione e Dettagli IP/Porta
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (isConnected) "🟢 " else "🔴 ", fontSize = 24.sp)
                Text(
                    text = if (isConnected) "Connesso al PC" else "Non Connesso",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected) Color(0xFF12B76A) else Color(0xFFF04438)
                )
            }
            if (!isConnected) {
                Text(
                    text = "Tentativo su IP: ${NetworkConfig.IP_PC}  Porta: ${NetworkConfig.PORTA_PC}",
                    fontSize = 13.sp,
                    color = Color(0xFF98A2B3),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Destra: I due pulsanti sequenziali
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            // PULSANTE 1: Apre il canale stabile WebSocket verso il server VB.NET
            Button(
                onClick = {
                    onStartLoading("Apertura canale con il server VB in corso...")

                    // Chiamiamo il nuovo ApiService per connettere il tubo
                    ApiService.connettiAlServer(
                        onStatoConnessione = { connesso ->
                            if (connesso) {
                                onStatusChange(true, "Server VB Raggiunto con successo! Ora puoi richiedere l'elenco.", emptyList())
                            } else {
                                onStatusChange(false, "Connessione fallita o interrotta. Controlla il server VB.", emptyList())
                            }
                        },
                        onElencoFogli = { listaRicevuta ->
                            onStatusChange(true, "Elenco caricato. Disponibili ${listaRicevuta.size} fogli.", listaRicevuta)
                        },
                        onFoglioDati = { contenutoJson ->
                            // Lo gestiremo nella Fase 3, per ora mantiene lo stato pronto
                            onStatusChange(true, "Foglio ricevuto ed elaborato con successo.", emptyList())
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280)),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            ) {
                Text("Verifica Connessione", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // PULSANTE 2: Spinge il comando di richiesta dentro il canale già aperto
            Button(
                onClick = {
                    onStartLoading("Richiesta elenco fogli in corso...")

                    // Spediamo la richiesta testuale concordata direttamente nel tubo del WebSocket
                    val comandoJson = "{\"type\":\"RICHIEDI_ELENCO\"}"
                    ApiService.inviaComandoAlPC(comandoJson)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(8.dp),
                enabled = isConnected && !isLoading // Attivo SOLO se il pallino è verde
            ) {
                Text("Aggiorna Elenco Fogli", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
