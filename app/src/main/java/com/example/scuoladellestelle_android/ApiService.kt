package com.example.scuoladellestelle_android

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// TABLET-ANDROID-01: timeout cancellabile durante l'identificazione.
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {

    private const val TAG = "ApiService"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    // TABLET-ANDROID-01: ogni tentativo possiede un solo timeout attivo.
    private var timeoutConnessione: Job? = null

    // 🎯 Bandiera di controllo hardware per il Timeout di sicurezza
    private var elencoGiaArrivato = false

    // 🎯 Memoria volatile per conservare l'ID hardware durante la sessione attiva
    private var idDispositivoSessione = "unknown_tablet"

    var onConnessioneStabilità: ((Boolean, String) -> Unit)? = null
    var onElencoRicevuto: ((List<StructElencofogliJson>) -> Unit)? = null
    var onContenutoFoglioRicevuto: ((String) -> Unit)? = null

    // TABLET-ANDROID-01: evento per la UI; non apre direttamente finestre.
    var onIdentificazioneRichiesta: ((String) -> Unit)? = null

    // TABLET-ANDROID-04: ultimo nome ricevuto dal server, indipendente dallo stato operativo.
    var nomeDispositivo: String = ""
        private set
    var onNomeDispositivoRicevuto: ((String) -> Unit)? = null

    /**
     * 🌐 CONNETTI, EFFETTUA HANDSHAKE E RICHIEDI ELENCO IN AUTOMATICO
     */
    @SuppressLint("HardwareIds")
    fun connettiAlServer(
        context: Context,
        notaUtente: String,
        onStatoConnessione: (Boolean, String) -> Unit,
        onElencoFogli: (List<StructElencofogliJson>) -> Unit,
        onFoglioDati: (String) -> Unit
    ) {
        this.onConnessioneStabilità = onStatoConnessione
        this.onElencoRicevuto = onElencoFogli

        // TABLET-ANDROID-01: il parametro notaUtente resta per compatibilità,
        // ma la nota locale non viene inviata nel primo HANDSHAKE.
        timeoutConnessione?.cancel()
        webSocket?.close(1000, "Reset")
        webSocket = null

        // Resettiamo la bandiera all'inizio di ogni tentativo di connessione
        elencoGiaArrivato = false

        // 🎯 Estrazione e memorizzazione immediata dell'ID hardware reale del tablet
        idDispositivoSessione = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_tablet"

        val urlWebSocket = NetworkConfig.BASE_URL.replace("http://", "ws://")
        val richiesta = Request.Builder().url(urlWebSocket).build()

        webSocket = client.newWebSocket(richiesta, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Tubo aperto. Spingo pacchetto di Handshake...")
                val pacchettoHandshake = JSONObject().apply {
                    put("type", "HANDSHAKE")
                    put("device_id", idDispositivoSessione)
                    // TABLET-ANDROID-01: la nota viene inviata solo su conferma dell'operatore.
                }
                webSocket.send(pacchettoHandshake.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    // TABLET-ANDROID-01: ignora messaggi di connessioni precedenti.
                    if (this@ApiService.webSocket !== webSocket) return@launch
                    elaboraTestoDalSocket(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    // TABLET-ANDROID-01: preserva il messaggio della chiusura controllata.
                    if (this@ApiService.webSocket !== webSocket) return@launch
                    timeoutConnessione?.cancel()
                    onConnessioneStabilità?.invoke(false, "Connessione interrotta dal server PC.")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    // TABLET-ANDROID-01: un vecchio socket non altera il tentativo corrente.
                    if (this@ApiService.webSocket !== webSocket) return@launch
                    timeoutConnessione?.cancel()
                    onConnessioneStabilità?.invoke(false, "Server non raggiungibile. Verifica IP e porta.")
                }
            }
        })

        // TABLET-ANDROID-01: mantiene gli 8 secondi, con possibilità di sospensione.
        avviaTimeoutConnessione()
    }

    // TABLET-ANDROID-01: riutilizzato dopo l'invio della nota, senza contare il tempo di digitazione.
    private fun avviaTimeoutConnessione() {
        timeoutConnessione?.cancel()
        timeoutConnessione = scope.launch {
            delay(8000)
            if (!elencoGiaArrivato) {
                Log.e(TAG, "⏳ TIMEOUT! Il server VB.NET non ha risposto entro 8 secondi.")
                disconnettiConMessaggio("Il server non risponde alla richiesta. Riprova.")
            }
        }
    }

    private fun elaboraTestoDalSocket(testoGrezzo: String) {
        try {
            val json = JSONObject(testoGrezzo)

            when (json.optString("type")) {
                // TABLET-ANDROID-01: il socket resta aperto mentre la UI raccoglie la nota.
                "HANDSHAKE_REGISTRATION_REQUIRED" -> {
                    timeoutConnessione?.cancel()
                    onIdentificazioneRichiesta?.invoke(
                        json.optString("messaggio", "Inserisci una nota identificativa.")
                    )
                }

                // TABLET-ANDROID-01: notifica il rifiuto usando il messaggio del server.
                "ACCESSO_NEGATO" -> {
                    disconnettiConMessaggio(json.optString("messaggio", "Dispositivo non autorizzato."))
                }

                "HANDSHAKE_OK" -> {
                    val nomeAssegnato = json.optString("nome_dispositivo", "Tablet Registrato")
                    // TABLET-ANDROID-04: conserva il dato e notifica la UI sul dispatcher principale.
                    nomeDispositivo = nomeAssegnato
                    onNomeDispositivoRicevuto?.invoke(nomeAssegnato)
                    onConnessioneStabilità?.invoke(true, "Connesso: $nomeAssegnato")

                    // 🎯 LA MODIFICA: Componiamo un JSON strutturato inserendo sia il type sia l'id hardware reale del tablet!
                    Log.d(TAG, "🚀 Handshake Approvato. Richiedo l'elenco fogli trasmettendo l'ID hardware...")
                    val comandoConElencoEId = JSONObject().apply {
                        put("type", "RICHIEDI_ELENCO")
                        put("device_id", idDispositivoSessione)
                    }
                    webSocket?.send(comandoConElencoEId.toString())
                }

                "HANDSHAKE_PENDING" -> {
                    val messaggioServer = json.optString("messaggio", "In attesa di approvazione.")
                    disconnettiConMessaggio(messaggioServer)
                }

                "HANDSHAKE_ERROR" -> {
                    val messaggioErrore = json.optString("messaggio", "Errore autenticazione.")
                    disconnettiConMessaggio(messaggioErrore)
                }

                "ELENCO_FOGLI_RISPOSTA" -> {
                    // OBIETTIVO RAGGIUNTO: I dati dell'elenco sono entrati! Disattiviamo il Timeout
                    elencoGiaArrivato = true

                    val datiArray = json.getJSONArray("dati")
                    val listaFogli = mutableListOf<StructElencofogliJson>()

                    for (i in 0 until datiArray.length()) {
                        val obj = datiArray.getJSONObject(i)
                        listaFogli.add(
                            StructElencofogliJson(
                                ID_Questionario = obj.getInt("ID_Questionario"),
                                Data_foglio = obj.getString("Data_foglio"),
                                Bambino = obj.getString("Bambino"),
                                ID_foglio = obj.getInt("ID_foglio"),
                                ID_Bambino = obj.getInt("ID_Bambino"),
                                Filename = obj.getString("Filename"),
                                note = obj.optString("note", ""),
                                disponibile = obj.optInt("disponibile", 1)
                            )
                        )
                    }

                    // 🎯 DIRETTIVA APPLICATA: Se la lista estratta dal server è vuota, notifichiamo uno stato speciale alla UI
                    if (listaFogli.isEmpty()) {
                        onConnessioneStabilità?.invoke(true, "SERVER_VUOTO")
                    } else {
                        onConnessioneStabilità?.invoke(true, "Elenco caricato con successo.")
                    }

                    onElencoRicevuto?.invoke(listaFogli)
                }

                "FOGLIO_RICEVUTO" -> {
                    try {
                        val stringaOggettoDati = json.getJSONObject("dati").toString()
                        Log.d(TAG, "🟢 SUCCESS! I byte del foglio sono entrati nel telefono.")
                        onContenutoFoglioRicevuto?.invoke(stringaOggettoDati)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Errore estrazione testo del foglio: ${ex.message}")
                        onContenutoFoglioRicevuto?.invoke("ERRORE")
                    }
                }

                // 🎯 NUOVA GESTIONE RISPOSTA PING DAL SERVER PC WINDOWS
                "PONG_ATTIVO" -> {
                    Log.d(TAG, "🏓 Ricevuto PONG dal server VB.NET. Il canale radio è attivo.")
                    scope.launch {
                        // Aggiorna lo stato sulla UI lasciando i fogli intatti
                        onConnessioneStabilità?.invoke(true, "Connessione verificata.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore decodifica nel Socket: ${e.message}")
        }
    }

    // TABLET-ANDROID-01: true indica che il socket ha accodato l'invio, non l'approvazione.
    // Note vuote o socket non disponibile restituiscono false senza inviare nulla.
    fun inviaHandshakeRegistrazione(notaUtente: String): Boolean {
        if (notaUtente.isBlank()) return false
        val socketCorrente = webSocket ?: return false
        val pacchettoHandshake = JSONObject().apply {
            put("type", "HANDSHAKE")
            put("device_id", idDispositivoSessione)
            put("nota_utente", notaUtente.trim())
        }
        val inviato = socketCorrente.send(pacchettoHandshake.toString())
        if (inviato) avviaTimeoutConnessione()
        return inviato
    }

    fun inviaComandoAlPC(comandoJson: String) {
        CoroutineScope(Dispatchers.IO).launch {
            webSocket?.send(comandoJson)
        }
    }

    /**
     * Chiude la linea in modo pulito forzando un testo di spiegazione sulla UI
     */
    private fun disconnettiConMessaggio(messaggioUtente: String) {
        // TABLET-ANDROID-01: evita che il timeout sovrascriva l'esito del server.
        timeoutConnessione?.cancel()
        webSocket?.close(1000, "Chiusura controllata")
        webSocket = null
        scope.launch {
            onConnessioneStabilità?.invoke(false, messaggioUtente)
        }
    }
    fun eseguiPingDiVerifica() {
        val pacchettoPing = JSONObject().apply {
            put("type", "PING_VERIFICA")
        }
        inviaComandoAlPC(pacchettoPing.toString())
    }

    fun disconnetti() {
        disconnettiConMessaggio("Disconnesso.")
    }
}
