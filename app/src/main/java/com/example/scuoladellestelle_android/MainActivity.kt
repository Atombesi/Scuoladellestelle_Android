package com.example.scuoladellestelle_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var showConfermaInvioDialog by remember { mutableStateOf(false) }
    // 🎯 VARIABILE AGGIUNTA PER IL POPUP DI ANNULLAMENTO
    var showConfermaInterrompiDialog by remember { mutableStateOf(false) }

    // 🎯 NUOVA VARIABILE DI STATO: Memorizza il foglio intero cliccato per estrarre i dati reali
    var foglioSelezionatoPerConferma by remember { mutableStateOf<StructElencofogliJson?>(null) }

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
                // 🎯 CERCA PER ID_QUESTIONARIO: Ripristina la logica nativa del tuo server VB.NET
                foglioSelezionatoPerConferma = fogliList.find { it.ID_Questionario == id }
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
            },
            // 🎯 PASSAGGIO DELLE LAMBDA REALI PER MANDARE I DATI AL PULSANTE SPOSTATO AL CENTRO
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
                    showConfermaInvioDialog = true
                },
                // 🎯 AGGANCIO NUOVO: Mostra il popup di interruzione al tocco
                onInterrompiClick = {
                    showConfermaInterrompiDialog = true
                }
            )
        }
    } // Chiude la Column principale
// 🎯 NUOVO DIALOG DI SICUREZZA CON DETTAGLI REALI E STRUTTURA COERENTE CON LE TUE VARIABILI ORIGINALI
    if (showConfermaDialog && foglioSelezionatoPerConferma != null) {
        AlertDialog(
            onDismissRequest = {
                showConfermaDialog = false
                foglioSelezionatoPerConferma = null
            },
            title = {
                Text(
                    text = "Conferma Caricamento",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1D2939)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sei sicuro di voler caricare questo foglio di lavoro?",
                        fontSize = 14.sp,
                        color = Color(0xFF344054),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
// Scheda grigia riassuntiva dei dati reali del foglio
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E7EC))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "👶  Bambino: ${foglioSelezionatoPerConferma?.Bambino ?: "N/D"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2939))
                            Text(text = "📅  Data Foglio: ${foglioSelezionatoPerConferma?.Data_foglio ?: "N/D"}", fontSize = 13.sp, color = Color(0xFF475467))
                            Text(text = "🔢  ID Foglio: # ${foglioSelezionatoPerConferma?.ID_foglio ?: "0"}", fontSize = 12.sp, color = Color(0xFF98A2B3))
                        }
                    }
                    Text(
                        text = "Premendo Sì, verrà inviata la richiesta al server.",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF475467),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfermaDialog = false
                        idDaScaricareTpc?.let { id ->
                            selectedId = id
                            isLoading = true
                            statusMessage = "Richiesta file JSON in corso..."

                            // 🎯 Invia il pacchetto originale con l'ID corretto che il server si aspetta
                            val comandoFoglioJson = """{"type":"RICHIEDI_FOGLIO","id_questionario":$id}"""
                            ApiService.inviaComandoAlPC(comandoFoglioJson)
                        }
                        foglioSelezionatoPerConferma = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12B76A))
                ) {
                    Text("Sì, Scarica", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showConfermaDialog = false
                        foglioSelezionatoPerConferma = null
                    }
                ) {
                    Text("No, Annulla", color = Color(0xFF475467))
                }
            }
        )
    }
    // 🎯 NUOVO POPUP DI SICUREZZA PER L'INVIO DEI DATI COMPILATI AL SERVER PC WINDOWS
    if (showConfermaInvioDialog) {
        AlertDialog(
            onDismissRequest = { showConfermaInvioDialog = false },
            title = {
                Text(
                    text = "Conferma invio foglio compilato",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1D2939)
                )
            },
            text = {
                Text(
                    text = "Vuoi inviare il foglio compilato al Server?",
                    fontSize = 14.sp,
                    color = Color(0xFF344054)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 1. Chiude e distrugge istantaneamente lo strato del Dialog
                        showConfermaInvioDialog = false
                        isLoading = true
                        statusMessage = "Invio del foglio compilato al PC Windows..."

                        // 2. Riesegue l'esatta logica nativa che avevi nel tuo file funzionante
                        val rootObj = JSONObject(jsonFoglioSelezionato!!)
                        val pacchettoDiRitorno = JSONObject().apply {
                            put("type", "SALVA_FOGLIO_COMPILATO")
                            put("id_foglio", rootObj.optInt("ID_Foglio"))
                            put("dati", rootObj)
                        }
                        ApiService.inviaComandoAlPC(pacchettoDiRitorno.toString())
                        statusMessage = "Dati spinti in rete verso il PC Windows."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12B76A)) // Verde operativo
                ) {
                    Text("Sì, Invia", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfermaInvioDialog = false }
                ) {
                    Text("Annulla", color = Color(0xFF475467))
                }
            }
        )
    }
    // 🎯 NUOVO POPUP DI SICUREZZA PER INTERROMPERE LA COMPILAZIONE E SBLOCCARE IL SERVER
    if (showConfermaInterrompiDialog) {
        AlertDialog(
            onDismissRequest = { showConfermaInterrompiDialog = false },
            title = {
                Text(
                    text = "Annulla Compilazione",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1D2939)
                )
            },
            text = {
                Text(
                    text = "Vuoi interrompere la compilazione del modulo attuale?\nIl server rilascerà lo stato occupato di questo documento.",
                    fontSize = 14.sp,
                    color = Color(0xFF344054)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfermaInterrompiDialog = false
                        isLoading = true
                        statusMessage = "Invio annullamento al Server..."

                        try {
                            // 🎯 ESTRAZIONE CORRETTA: Prende l'ID_Questionario nativo del server PC
                            val rootObj = JSONObject(jsonFoglioSelezionato!!)
                            var idQuestionarioEstratto = rootObj.optInt("ID_Questionario", 0)
                            if (idQuestionarioEstratto == 0) {
                                idQuestionarioEstratto = rootObj.optInt("ID_questionario", 0)
                            }

                            // Costruisce il pacchetto concordato con la chiave corretta
                            val pacchettoAnnullamento = JSONObject().apply {
                                put("type", "COMPILAZIONE_ANNULLATA")
                                put("id_questionario", idQuestionarioEstratto)
                            }

                            // Spinge il comando radio nel tunnel nativo
                            ApiService.inviaComandoAlPC(pacchettoAnnullamento.toString())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Ripristina l'applicazione tornando alla schermata di connessione/griglia
                        jsonFoglioSelezionato = null
                        selectedId = null
                        statusMessage = "Compilazione annullata. Sessione sbloccata sul Server."
                        isLoading = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF04438)) // Rosso avviso
                ) {
                    Text("Sì, Interrompi", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfermaInterrompiDialog = false }
                ) {
                    Text("No, Continua", color = Color(0xFF475467))
                }
            }
        )
    }


}