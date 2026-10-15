package com.example.scuoladellestelle_android

/**
 * CONFIGURAZIONE DI RETE DINAMICA: Contiene i parametri modificabili dal tablet.
 * All'avvio dell'app vengono sovrascritti con i valori reali salvati in memoria.
 */
object NetworkConfig {
    var IP_PC: String = "192.168.68.57"  // Valore iniziale di fallback
    var PORTA_PC: String = "8080"        // Valore iniziale di fallback

    /**
     * Genera dinamicamente l'URL per le chiamate HTTP o i WebSocket.
     */
    val BASE_URL: String
        get() = "http://$IP_PC:$PORTA_PC"
}