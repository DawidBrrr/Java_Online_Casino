package com.casino.java_online_casino.Connection.Session;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Klasa zarządzająca sesjami kryptograficznymi (KeyManager) dla klientów identyfikowanych przez UUID.
 * Wzorzec Singleton. Wątko-bezpieczna.
 */
public class KeySessionManager {

    private static volatile KeySessionManager instance;

    private final ConcurrentHashMap<UUID, KeyManager> sessionMap;

    private KeySessionManager() {
        sessionMap = new ConcurrentHashMap<>();
    }

    /**
     * Zwraca jedyną instancję klasy KeySessionManager (Singleton, double-checked locking).
     */
    public static KeySessionManager getInstance() {
        if (instance == null) {
            synchronized (KeySessionManager.class) {
                if (instance == null) {
                    instance = new KeySessionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Zwraca obiekt KeyManager przypisany do danego UUID.
     * Jeśli nie istnieje, zostanie utworzony nowy i dodany.
     *
     * @param uuid UUID klienta
     * @return KeyManager przypisany do UUID
     */
    public KeyManager getOrCreateKeyManager(UUID uuid) {
        return sessionMap.computeIfAbsent(uuid, id -> new KeyManager());
    }

    /**
     * Ręczne dodanie instancji KeyManager (jeśli potrzebujesz customowej inicjalizacji).
     */
    public void putKeyManager(UUID uuid, KeyManager keyManager) {
        if (uuid == null || keyManager == null) {
            throw new IllegalArgumentException("UUID i KeyManager nie mogą być null.");
        }
        sessionMap.put(uuid, keyManager);
    }

    /**
     * Usunięcie KeyManagera powiązanego z UUID.
     */
    public void removeKeyManager(UUID uuid) {
        sessionMap.remove(uuid);
    }

    /**
     * Sprawdzenie, czy istnieje KeyManager dla danego UUID.
     */
    public boolean contains(UUID uuid) {
        return sessionMap.containsKey(uuid);
    }
}
