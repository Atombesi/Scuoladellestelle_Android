package com.example.scuoladellestelle_android

/**
 * CONFIGURAZIONE RETE LOCALE: Modifica qui quando cambia l'IP del PC o la porta del server VB.
 * Questo file è isolato per permetterti modifiche rapide senza toccare la grafica.
 */
object NetworkConfig {
    const val IP_PC = "192.168.68.57"  // <-- Inserisci qui l'IP reale del tuo PC
    const val PORTA_PC = "8080"        // <-- Inserisci qui la porta impostata nel programma VB

    // Genera automaticamente l'URL di base per le chiamate HTTP
    const val BASE_URL = "http://$IP_PC:$PORTA_PC"
}