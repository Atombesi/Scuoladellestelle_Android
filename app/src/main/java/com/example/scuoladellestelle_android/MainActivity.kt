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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
// TABLET-ANDROID-02: campo identificativo e adattamento alla tastiera.
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.OutlinedTextField

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

    // TABLET-ANDROID-04: stato separato dai messaggi; recupera anche un nome già ricevuto.
    var nomeDispositivo by remember { mutableStateOf(ApiService.nomeDispositivo) }
    DisposableEffect(Unit) {
        val nomeRicevuto: (String) -> Unit = { nomeDispositivo = it }
        ApiService.onNomeDispositivoRicevuto = nomeRicevuto
        nomeDispositivo = ApiService.nomeDispositivo
        onDispose {
            if (ApiService.onNomeDispositivoRicevuto === nomeRicevuto) {
                ApiService.onNomeDispositivoRicevuto = null
            }
        }
    }

    // TABLET-ANDROID-02: nessuna apertura iniziale; solo la callback abilita il dialog.
    var showIdentificazioneDialog by remember { mutableStateOf(false) }
    var notaIdentificazione by remember { mutableStateOf("") }
    var erroreIdentificazione by remember { mutableStateOf<String?>(null) }

    // TABLET-ANDROID-02: registra una sola callback e rimuove soltanto quella
    // appartenente a questa schermata quando esce dalla composizione.
    DisposableEffect(Unit) {
        val identificazioneRichiesta: (String) -> Unit = { _ ->
            if (!showIdentificazioneDialog) {
                notaIdentificazione = ""
                erroreIdentificazione = null
                showIdentificazioneDialog = true
            }
            isLoading = false
        }
        ApiService.onIdentificazioneRichiesta = identificazioneRichiesta
        onDispose {
            if (ApiService.onIdentificazioneRichiesta === identificazioneRichiesta) {
                ApiService.onIdentificazioneRichiesta = null
            }
        }
    }

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

// 🎯 VARIABILE ADATTIVA CORRETTA: Usa il tuo file Version.kt nativo passando il contesto di Compose
    val contestoLocal = androidx.compose.ui.platform.LocalContext.current
    val isTablet = AppVersion.isTablet(contestoLocal)

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
            nomeDispositivo = nomeDispositivo,
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
                // 1. Se abbiamo già intercettato il server vuoto, blocchiamo la successiva sovrascrittura automatica
                if (statusMessage == "Nessun foglio disponibile da compilare" && messaggio == "Elenco caricato con successo.") {
                    isLoading = false
                } else {
                    // 2. Comportamento standard per tutte le altre notifiche
                    isConnected = connesso

                    if (messaggio == "SERVER_VUOTO") {
                        statusMessage = "Nessun foglio disponibile da compilare"
                        fogliList = emptyList() // Svuota la griglia se c'erano vecchi residui
                    } else {
                        statusMessage = messaggio
                    }

                    if (lista.isNotEmpty() || !connesso) {
                        fogliList = lista
                    }
                    isLoading = false
                }
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





    // TABLET-ANDROID-02: dimensioni naturali e contenuto scorrevole anche con IME
    // in landscape. Indietro e tocco esterno equivalgono ad Annulla.
    if (showIdentificazioneDialog) {
        val annullaIdentificazione: () -> Unit = {
            showIdentificazioneDialog = false
            erroreIdentificazione = null
            isLoading = false
            ApiService.disconnetti()
        }
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = annullaIdentificazione,
            title = {
                Text("Identificazione dispositivo", fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Inserisci un messaggio per richiedere l'accesso.", fontSize = 14.sp)
                    OutlinedTextField(
                        value = notaIdentificazione,
                        onValueChange = {
                            notaIdentificazione = it
                            erroreIdentificazione = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Messaggio identificativo") },
                        singleLine = true,
                        isError = erroreIdentificazione != null
                    )
                    erroreIdentificazione?.let { errore ->
                        Text(errore, color = Color(0xFFB42318), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = notaIdentificazione.isNotBlank(),
                    onClick = {
                        if (ApiService.inviaHandshakeRegistrazione(notaIdentificazione)) {
                            // TABLET-ANDROID-02: PENDING non riapre il dialog;
                            // l'invio accodato non equivale all'approvazione.
                            showIdentificazioneDialog = false
                            isLoading = true
                            statusMessage = "Richiesta di accesso inviata. Attendo risposta dal server..."
                        } else {
                            erroreIdentificazione = "Invio non riuscito. Verifica la connessione o annulla."
                        }
                    }
                ) { Text("Invia") }
            },
            dismissButton = {
                TextButton(onClick = annullaIdentificazione) { Text("Annulla") }
            }
        )
    }

    // 🎯 NUOVO DIALOG DI SICUREZZA ADATTIVO PER TABLET E CELLULARE IN ORIZZONTALE
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
                    fontSize = if (isTablet) 18.sp else 14.sp,
                    color = Color(0xFF1D2939)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 8.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Sei sicuro di voler caricare questo foglio di lavoro?",
                        fontSize = if (isTablet) 14.sp else 11.sp,
                        color = Color(0xFF344054),
                        modifier = Modifier.padding(bottom = if (isTablet) 8.dp else 1.dp)
                    )

                    // Scheda grigia adattiva: Column su tablet, Row semplificata su cellulare
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E7EC))
                    ) {
                        if (isTablet) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "👶  Bambino: ${foglioSelezionatoPerConferma?.Bambino ?: "N/D"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2939))
                                Text(text = "📅  Data Foglio: ${foglioSelezionatoPerConferma?.Data_foglio ?: "N/D"}", fontSize = 13.sp, color = Color(0xFF475467))
                                Text(text = "🔢  ID Foglio: # ${foglioSelezionatoPerConferma?.ID_foglio ?: "0"}", fontSize = 12.sp, color = Color(0xFF98A2B3))
                            }
                        } else {
                            // 🎯 ROW SEMPLIFICATA SENZA WEIGHT: Spazia gli elementi in orizzontale in modo nativo
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "👶 ${foglioSelezionatoPerConferma?.Bambino ?: "N/D"}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2939))
                                Text(text = "📅 ${foglioSelezionatoPerConferma?.Data_foglio ?: "N/D"}", fontSize = 10.sp, color = Color(0xFF475467))
                                Text(text = "# ${foglioSelezionatoPerConferma?.ID_foglio ?: "0"}", fontSize = 10.sp, color = Color(0xFF98A2B3))
                            }
                        }
                    }

                    Text(
                        text = "Premendo Sì, verrà inviata la richiesta al server.",
                        fontSize = if (isTablet) 13.sp else 10.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF475467),
                        modifier = Modifier.padding(top = if (isTablet) 6.dp else 1.dp)
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
                            val comandoFoglioJson = """{"type":"RICHIEDI_FOGLIO","id_questionario":$id}"""
                            ApiService.inviaComandoAlPC(comandoFoglioJson)
                        }
                        foglioSelezionatoPerConferma = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12B76A)),
                    contentPadding = PaddingValues(horizontal = if (isTablet) 16.dp else 10.dp, vertical = if (isTablet) 8.dp else 2.dp)
                ) {
                    Text("Sì, Scarica", fontWeight = FontWeight.Bold, color = Color.White, fontSize = if (isTablet) 14.sp else 11.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showConfermaDialog = false
                        foglioSelezionatoPerConferma = null
                    },
                    contentPadding = PaddingValues(horizontal = if (isTablet) 16.dp else 10.dp, vertical = if (isTablet) 8.dp else 2.dp)
                ) {
                    Text("No, Annulla", color = Color(0xFF475467), fontSize = if (isTablet) 14.sp else 11.sp)
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
