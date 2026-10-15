package com.example.scuoladellestelle_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF0F2F5)) {
                SchermataOperatore()
            }
        }
    }
}

@Composable
fun SchermataOperatore() {
    var isConnected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Disconnesso. Premi 'Verifica Connessione' per iniziare.") }
    var fogliList by remember { mutableStateOf(listOf<StructElencofogliJson>()) }
    var selectedId by remember { mutableStateOf<Int?>(null) }

    // 🎯 Memoria di stato per la stringa del singolo foglio ricevuto
    var jsonFoglioSelezionato by remember { mutableStateOf<String?>(null) }

    var showConfermaDialog by remember { mutableStateOf(false) }
    var idDaScaricareTpc by remember { mutableStateOf<Int?>(null) }

    // 🎯 AGGANCIO DEL TUBO RADIO: Lo facciamo una volta sola all'avvio dell'app!
    LaunchedEffect(Unit) {
        ApiService.onContenutoFoglioRicevuto = { jsonRicevuto ->
            if (jsonRicevuto != "ERRORE" && jsonRicevuto.isNotBlank()) {
                // Il dato è entrato pulito: salviamo il testo e SPEGNIAMO la rotellina!
                jsonFoglioSelezionato = jsonRicevuto
                statusMessage = "Foglio pronto per essere compilato."
                isLoading = false
            } else {
                isLoading = false
                statusMessage = "Errore nel download del modulo."
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 1. ZONA ALTA
        ZonaAltaView(
            isConnected = isConnected,
            isLoading = isLoading,
            statusMessage = statusMessage,
            onStartLoading = { messaggio ->
                isLoading = true
                statusMessage = messaggio
            },
            onStatusChange = { connesso, messaggio, lista ->
                isConnected = connesso
                statusMessage = messaggio
                if (lista.isNotEmpty() || !connesso) {
                    fogliList = lista
                }
                isLoading = false
            }
        )

        // 2. ZONA CENTRALE
        ZonaCentraleView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            statusMessage = statusMessage,
            isLoading = isLoading,
            fogliList = fogliList,
            selectedId = selectedId,
            onFoglioSelezionato = { id ->
                idDaScaricareTpc = id
                showConfermaDialog = true
            },
            onDownloadCompletato = { json ->
                isLoading = false
                statusMessage = "Foglio pronto per essere compilato."
            },
            onErrore = {
                isLoading = false
                statusMessage = "Errore nel download del modulo."
            }
        )

        // 3. ZONA BASSA
        ZonaBassaView()
    }

    if (showConfermaDialog) {
        AlertDialog(
            onDismissRequest = { showConfermaDialog = false },
            title = { Text("Scaricamento Foglio", fontSize = 18.sp) },
            text = { Text("Vuoi scaricare il file JSON del modulo selezionato (ID: $idDaScaricareTpc) dal server VB.NET?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfermaDialog = false
                        idDaScaricareTpc?.let { id ->
                            selectedId = id
                            isLoading = true
                            statusMessage = "Richiesta file JSON in corso..."

                            val comandoFoglioJson = "{\"type\":\"RICHIEDI_FOGLIO\",\"id_questionario\":$id}"
                            ApiService.inviaComandoAlPC(comandoFoglioJson)
                        }
                    }
                ) {
                    Text("Sì, Scarica", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfermaDialog = false }) {
                    Text("Annulla", color = Color(0xFF475467))
                }
            }
        )
    }
}
