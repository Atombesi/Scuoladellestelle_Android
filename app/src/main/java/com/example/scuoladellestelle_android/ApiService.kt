package com.example.scuoladellestelle_android

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // 🎯 Bandiera di controllo hardware per il Timeout di sicurezza
    private var elencoGiaArrivato = false

    var onConnessioneStabilità: ((Boolean, String) -> Unit)? = null
    var onElencoRicevuto: ((List<StructElencofogliJson>) -> Unit)? = null
    var onContenutoFoglioRicevuto: ((String) -> Unit)? = null

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

        webSocket?.close(1000, "Reset")
        webSocket = null

        // Resettiamo la bandiera all'inizio di ogni tentativo di connessione
        elencoGiaArrivato = false

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_tablet"
        val urlWebSocket = NetworkConfig.BASE_URL.replace("http://", "ws://")
        val richiesta = Request.Builder().url(urlWebSocket).build()

        webSocket = client.newWebSocket(richiesta, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Tubo aperto. Spingo pacchetto di Handshake...")
                val pacchettoHandshake = JSONObject().apply {
                    put("type", "HANDSHAKE")
                    put("device_id", androidId)
                    put("nota_utente", notaUtente.ifBlank { "Postazione Tablet" })
                }
                webSocket.send(pacchettoHandshake.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    elaboraTestoDalSocket(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    onConnessioneStabilità?.invoke(false, "Connessione interrotta dal server PC.")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    onConnessioneStabilità?.invoke(false, "Server non raggiungibile. Verifica IP e porta.")
                }
            }
        })

        // 🎯 1. TIMER DI TIMEOUT DI SICUREZZA (8 Secondi)
        scope.launch {
            delay(8000) // Aspetta 8 secondi ad orologio fermo in background
            if (!elencoGiaArrivato) {
                // Se dopo 8 secondi l'elenco non è arrivato, consideriamo il server muto o congestionato
                Log.e(TAG, "⏳ TIMEOUT! Il server VB.NET non ha risposto entro 8 secondi.")
                disconnettiConMessaggio("Il server non risponde alla richiesta. Riprova.")
            }
        }
    }

    private fun elaboraTestoDalSocket(testoGrezzo: String) {
        try {
            val json = JSONObject(testoGrezzo)

            when (json.optString("type")) {
                "HANDSHAKE_OK" -> {
                    val nomeAssegnato = json.optString("nome_dispositivo", "Tablet Registrato")
                    onConnessioneStabilità?.invoke(true, "Connesso: $nomeAssegnato")

                    // 🎯 2. AUTOMATISMO CASCATA: L'handshake è OK, richiediamo subito l'elenco fogli!
                    Log.d(TAG, "🚀 Handshake Approvato. Richiedo l'elenco fogli autonomamente...")
                    val comandoAutonomo = "{\"type\":\"RICHIEDI_ELENCO\"}"
                    webSocket?.send(comandoAutonomo)
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
                    // 🎯 3. OBIETTIVO RAGGIUNTO: I dati dell'elenco sono entrati! Disattiviamo il Timeout
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
                                Filename = obj.getString("Filename")
                            )
                        )
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore decodifica nel Socket: ${e.message}")
        }
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
        webSocket?.close(1000, "Chiusura controllata")
        webSocket = null
        scope.launch {
            onConnessioneStabilità?.invoke(false, messaggioUtente)
        }
    }

    fun disconnetti() {
        disconnettiConMessaggio("Disconnesso.")
    }
}
