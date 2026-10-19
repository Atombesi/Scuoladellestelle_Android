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
import org.json.JSONArray
import org.json.JSONObject

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

    // Memoria di stato per la stringa del singolo foglio ricevuto
    var jsonFoglioSelezionato by remember { mutableStateOf<String?>(null) }

    var showConfermaDialog by remember { mutableStateOf(false) }
    var idDaScaricareTpc by remember { mutableStateOf<Int?>(null) }

    // ID del Tablet configurato per la sincronizzazione con l'anagrafica VB.NET
    val dispositivoHardwareId = "TABLET_01"

    // AGGANCIO DEL TUBO RADIO NATIVO: Lo facciamo una volta sola all'avvio dell'app!
    LaunchedEffect(Unit) {
        ApiService.onContenutoFoglioRicevuto = { jsonRicevuto ->
            if (jsonRicevuto != null && jsonRicevuto != "ERRORE" && jsonRicevuto.isNotBlank()) {
                jsonFoglioSelezionato = jsonRicevuto
                statusMessage = "Foglio pronto per essere compilato."
                isLoading = false

                // HANDSHAKE AUTOMATICO (Opzione A): Invio notifica strutturata con spaziature pulite
                try {
                    val root = JSONObject(jsonRicevuto)
                    var idQuestionarioEstratto = root.optInt("ID_Questionario", 0)
                    if (idQuestionarioEstratto == 0) {
                        idQuestionarioEstratto = root.optInt("ID_questionario", 0)
                    }

                    if (idQuestionarioEstratto > 0) {
                        val pacchettoConferma = JSONObject().apply {
                            put("type", "CONFERMA_RICEZIONE_FOGLIO")
                            put("id_questionario", idQuestionarioEstratto)
                            put("device_id", dispositivoHardwareId)
                        }
                        ApiService.inviaComandoAlPC(pacchettoConferma.toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

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
            onStartLoading = { messaggio: String ->
                isLoading = true
                statusMessage = messaggio
            },
            onStatusChange = { connesso: Boolean, messaggio: String, lista: List<StructElencofogliJson> ->
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
            onFoglioSelezionato = { id: Int ->
                idDaScaricareTpc = id
                showConfermaDialog = true
            },
            onDownloadCompletato = { json: String ->
                isLoading = false

                // 🎯 L'INTERVENTO CHIRURGICO SUL CICLO DEI MESSAGGI:
                // Se la ZonaCentrale intercetta la stringa di conferma dal server PC...
                if (json == "OK_SALVATO") {
                    jsonFoglioSelezionato = null // Svuota lo stato principale per chiudere la Zona Bassa
                    statusMessage = "Foglio registrato sul PC Windows con successo!"
                } else {
                    statusMessage = "Foglio pronto per essere compilato."
                    jsonFoglioSelezionato = json
                }
            },
            onErrore = {
                isLoading = false
                statusMessage = "Errore nel download del modulo."
            },
            onModificaFoglio = { jsonAggiornato: String ->
                jsonFoglioSelezionato = jsonAggiornato
            }
        )

        // 3. ZONA BASSA VISIBILE SOLO SE IL FOGLIO È APERTO
        if (!jsonFoglioSelezionato.isNullOrBlank()) {

            val rootObj = remember(jsonFoglioSelezionato) { JSONObject(jsonFoglioSelezionato!!) }

            val arrayG = remember(jsonFoglioSelezionato) { rootObj.optJSONArray("VociDiUnFoglio") ?: JSONArray() }
            val arrayRT = remember(jsonFoglioSelezionato) { rootObj.optJSONArray("RT") ?: JSONArray() }
            val arrayCP = remember(jsonFoglioSelezionato) { rootObj.optJSONArray("CP") ?: JSONArray() }

            val totaliG = arrayG.length()
            val compilatiG = (0 until arrayG.length()).map { arrayG.getJSONObject(it) }.count { it.optBoolean("COMPILATO", false) }

            val totaliRT = arrayRT.length()
            val compilatiRT = (0 until arrayRT.length()).map { arrayRT.getJSONObject(it) }.count { it.optBoolean("COMPILATO", false) || it.optInt("COMPILATO", 0) == 1 }

            val totaliCP = arrayCP.length()
            val compilatiCP = (0 until arrayCP.length()).map { arrayCP.getJSONObject(it) }.count { it.optBoolean("COMPILATO", false) || it.optInt("COMPILATO", 0) == 1 }

            ZonaBassaView(
                totaliG = totaliG, compilatiG = compilatiG,
                totaliRT = totaliRT, compilatiRT = compilatiRT,
                totaliCP = totaliCP, compilatiCP = compilatiCP,
                onSaltoCategoria = { categoriaSelezionata: String ->
                    when (categoriaSelezionata) {
                        "GIORNALIERO" -> {
                            val relativoVuoto = (0 until arrayG.length()).indexOfFirst { !arrayG.getJSONObject(it).optBoolean("COMPILATO", false) }
                            if (relativoVuoto != -1) {
                                selectedId = relativoVuoto
                            }
                        }
                        "COMPORTAMENTALE" -> {
                            val relativoVuoto = (0 until arrayCP.length()).indexOfFirst { !arrayCP.getJSONObject(it).optBoolean("COMPILATO", false) && arrayCP.getJSONObject(it).optInt("COMPILATO", 0) != 1 }
                            if (relativoVuoto != -1) {
                                selectedId = arrayG.length() + relativoVuoto
                            }
                        }
                        "ROUTINE" -> {
                            val relativoVuoto = (0 until arrayRT.length()).indexOfFirst { !arrayRT.getJSONObject(it).optBoolean("COMPILATO", false) && arrayRT.getJSONObject(it).optInt("COMPILATO", 0) != 1 }
                            if (relativoVuoto != -1) {
                                selectedId = arrayG.length() + arrayCP.length() + relativoVuoto
                            }
                        }
                    }
                },
                onInviaClick = {
                    val pacchettoDiRitorno = JSONObject().apply {
                        put("type", "SALVA_FOGLIO_COMPILATO")
                        put("id_foglio", rootObj.optInt("ID_Foglio"))
                        put("dati", rootObj)
                    }
                    ApiService.inviaComandoAlPC(pacchettoDiRitorno.toString())
                    statusMessage = "Dati spinti in rete verso il PC Windows."
                }
            )
        }
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
                            val comandoFoglioJson = """{"type":"RICHIEDI_FOGLIO","id_questionario":$id}"""
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
