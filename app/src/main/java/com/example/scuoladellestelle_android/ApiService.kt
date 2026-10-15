package com.example.scuoladellestelle_android

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    var onConnessioneStabilità: ((Boolean, String) -> Unit)? = null
    var onElencoRicevuto: ((List<StructElencofogliJson>) -> Unit)? = null
    var onContenutoFoglioRicevuto: ((String) -> Unit)? = null

    /**
     * 🌐 CONNETTI ED EFFETTUA HANDSHAKE SU RETE LOCALE
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
      //  this.onContenutoFoglioRicevuto = onFoglioDati

        webSocket?.close(1000, "Reset")

        // Recuperiamo l'ID hardware univoco e fisso del tablet (ANDROID_ID)
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_tablet"

        val urlWebSocket = NetworkConfig.BASE_URL.replace("http://", "ws://")
        val richiesta = Request.Builder().url(urlWebSocket).build()

        webSocket = client.newWebSocket(richiesta, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Tubo aperto. Spingo pacchetto di Handshake...")

                // 🎯 INVIA RICHIESTA REGISTRAZIONE SU SQLite AL SERVER VB
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
                scope.launch { onConnessioneStabilità?.invoke(false, "Connessione interrotta dal server PC.") }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch { onConnessioneStabilità?.invoke(false, "Server non raggiungibile. Verifica IP e porta.") }
            }
        })
    }

    private fun elaboraTestoDalSocket(testoGrezzo: String) {
        try {
            val json = JSONObject(testoGrezzo)

            when (json.optString("type")) {
                "HANDSHAKE_OK" -> {
                    // 🎯 DISPOSITIVO AUTORIZZATO: La linea è stabile e aperta.
                    // Non chiamiamo mai metodi di close qui sotto, lasciamo il tubo attivo per sempre!
                    val nomeAssegnato = json.optString("nome_dispositivo", "Tablet Registrato")
                    onConnessioneStabilità?.invoke(true, "Connessione stabilita con: $nomeAssegnato")
                }

                "HANDSHAKE_PENDING" -> {
                    // Presente nel DB ma in attesa di spunta ENABLED = 1
                    val messaggioServer = json.optString("messaggio", "In attesa di approvazione.")
                    onConnessioneStabilità?.invoke(false, messaggioServer)

                    // Chiudiamo il socket SOLO se l'amministratore ci ha esplicitamente rifiutato/messo in attesa
                    webSocket?.close(1000, "In attesa di sblocco amministratore")
                    webSocket = null
                }

                "HANDSHAKE_ERROR" -> {
                    val messaggioErrore = json.optString("messaggio", "Errore autenticazione.")
                    onConnessioneStabilità?.invoke(false, messaggioErrore)
                    webSocket?.close(1000, "Errore rifiuto")
                    webSocket = null
                }

                "ELENCO_FOGLI_RISPOSTA" -> {
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
                // 🎯 AGGIUNTA DIAGNOSTICA: Intercettiamo il file JSON del foglio inviato da VB
                // 🎯 INTERCETTAZIONE E DECODIFICA DEL FOGLIO REALE (Fase 3)
                "FOGLIO_RICEVUTO" -> {
                    try {
                        // Estraiamo l'oggetto JSON contenuto nel tag "dati" usando il motore nativo
                        val stringaOggettoDati = json.getJSONObject("dati").toString()

                        Log.d(TAG, "🟢 SUCCESS! I byte del foglio sono entrati nel telefono.")

                        // Passiamo il testo pulito alla MainActivity per sbloccare la grafica
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
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            webSocket?.send(comandoJson)
        }
    }

    fun disconnetti() {
        webSocket?.close(1000, "Chiusura utente")
        webSocket = null
        scope.launch { onConnessioneStabilità?.invoke(false, "Disconnesso.") }
    }
}
