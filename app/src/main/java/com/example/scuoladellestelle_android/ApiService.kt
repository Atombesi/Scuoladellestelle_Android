package com.example.scuoladellestelle_android

import android.util.Log
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SERVIZIO DI RETE WEB_SOCKET NATIVO (ApiService): Gestisce l'unico canale continuo verso il PC.
 * Elimina totalmente i blocchi di sicurezza HTTP (EPERM) del tablet.
 */
object ApiService {

    private const val TAG = "ApiService"

    // Il client OkHttp configurato con i tempi di attesa corretti
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Mantiene il canale aperto all'infinito
        .build()

    private var webSocket: WebSocket? = null

    // Riferimenti alle funzioni grafiche per aggiornare l'interfaccia in tempo real
    private var onConnessioneStabilità: ((Boolean) -> Unit)? = null
    private var onElencoRicevuto: ((List<StructElencofogliJson>) -> Unit)? = null
    private var onContenutoFoglioRicevuto: ((String) -> Unit)? = null

    /**
     * 🌐 APRE IL CANALE: Connette il tablet al server VB.NET (ws://IP_PC:PORTA)
     */
    fun connettiAlServer(
        onStatoConnessione: (Boolean) -> Unit,
        onElencoFogli: (List<StructElencofogliJson>) -> Unit,
        onFoglioDati: (String) -> Unit
    ) {
        // Salviamo i riferimenti per poterli usare quando arrivano i messaggi dal PC
        this.onConnessioneStabilità = onStatoConnessione
        this.onElencoRicevuto = onElencoFogli
        this.onContenutoFoglioRicevuto = onFoglioDati

        // Se c'è già un vecchio socket aperto, lo chiudiamo prima di ricominciare
        webSocket?.close(1000, "Nuovo tentativo di connessione")

        // Trasformiamo l'indirizzo HTTP standard in un URL WebSocket (ws://)
        val urlWebSocket = NetworkConfig.BASE_URL.replace("http://", "ws://")
        Log.d(TAG, "Tentativo di apertura canale su: $urlWebSocket")

        val richiesta = Request.Builder().url(urlWebSocket).build()

        // Avviamo l'ascoltatore nativo del tubo
        webSocket = client.newWebSocket(richiesta, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Tubo radio hardware aperto con successo!")
                // Nota: Non accendiamo ancora il pallino verde. Aspettiamo l'HANDSHAKE_OK dal PC per sicurezza.
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Messaggio arrivato dal server VB: $text")

                // Forza l'elaborazione del testo a saltare sul Thread della grafica
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    elaboraMessaggioDalPC(text)
                }
            }


            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Il server sta chiudendo il canale: $reason")
                onConnessioneStabilità?.invoke(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Errore di linea hardware o PC spento: ${t.message}")
                onConnessioneStabilità?.invoke(false)
            }
        })
    }

    /**
     * 📨 DECODIFICA I COMANDI IN ARRIVO DAL SERVER VB.NET
     */
    private fun elaboraMessaggioDalPC(testoJson: String) {
        try {
            val json = JSONObject(testoJson)
            val tipoMessaggio = json.optString("type")

            when (tipoMessaggio) {
                "HANDSHAKE_OK" -> {
                    // Il PC ha risposto all'apertura del tubo: la linea è libera!
                    onConnessioneStabilità?.invoke(true)
                }

                "ELENCO_FOGLI_RISPOSTA" -> {
                    // Sono arrivati i byte con i nomi dei bambini dal server VB
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

                "SALVA_FOGLIO_OK" -> {
                    onContenutoFoglioRicevuto?.invoke("OK")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore decodifica JSON dal server: ${e.message}")
        }
    }

    /**
     * 📤 INVIA UN COMANDO DENTRO IL TUBO VERSO IL PC
     */
    fun inviaComandoAlPC(comandoJson: String) {
        webSocket?.let {
            if (it.send(comandoJson)) {
                Log.d(TAG, "Inviato con successo al PC: $comandoJson")
            } else {
                Log.e(TAG, "Impossibile inviare dati, il tubo è ostruito.")
            }
        }
    }

    /**
     * 🛑 CHIUDE IL CANALE DEFINITIVAMENTE
     */
    fun disconnetti() {
        webSocket?.close(1000, "Chiusura manuale dall'applicazione")
        webSocket = null
        onConnessioneStabilità?.invoke(false)
    }
}
