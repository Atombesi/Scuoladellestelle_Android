package com.example.scuoladellestelle_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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

    Column(modifier = Modifier.fillMaxSize()) {

        // 1. ZONA ALTA (Modulo Autonomo per i WebSocket)
        ZonaAltaView(
            isConnected = isConnected,
            isLoading = isLoading,
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

        // 2. ZONA CENTRALE (Modulo Autonomo)
        ZonaCentraleView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            statusMessage = statusMessage,
            isLoading = isLoading,
            fogliList = fogliList,
            selectedId = selectedId,
            onFoglioSelezionato = { id ->
                selectedId = id
                isLoading = true
                statusMessage = "Scaricamento foglio ID_Questionario: $id..."

                // Inviamo la richiesta del singolo foglio dentro il tubo del WebSocket
                val comandoFoglioJson = "{\"type\":\"RICHIEDI_FOGLIO\",\"id_questionario\":$id}"
                ApiService.inviaComandoAlPC(comandoFoglioJson)
            },
            onDownloadCompletato = { json ->
                isLoading = false
                statusMessage = "Foglio $selectedId pronto per essere compilato."
            },
            onErrore = {
                isLoading = false
                statusMessage = "Errore nel download del modulo."
            }
        )

        // 3. ZONA BASSA (Modulo Autonomo)
        ZonaBassaView()
    }
}
