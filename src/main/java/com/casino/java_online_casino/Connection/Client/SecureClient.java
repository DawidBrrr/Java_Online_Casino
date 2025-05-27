package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;

import java.io.*;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class SecureClient {
    private final String host;
    private final int port;
    private final KeyManager keyManager;

    public SecureClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.keyManager = new KeyManager();
    }

    public void start() throws IOException {
        try (Socket socket = new Socket(host, port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))
        ) {
            // 1. Odbieramy publiczny klucz serwera
            String serverPub = in.readUTF();
            byte[] serverPubBytes = Base64.getDecoder().decode(serverPub);
            PublicKey serverKey = KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(serverPubBytes));
            keyManager.setForeignPublicKey(serverKey);
            // 2. Wysyłamy nasz EC publiczny klucz
            String clientPub = Base64.getEncoder().encodeToString(
                keyManager.getEcPublicKey().getEncoded()
            );
            out.writeUTF(clientPub);

            // 3. Derive shared secret
            keyManager.deriveSharedSecret();
            System.out.println("Wspólny klucz AES wyliczony");

            // 4. Wysyłanie zaszyfrowanych wiadomości
            System.out.println("Wpisz wiadomość (exit aby zakończyć):");
            String line;
            while ((line = console.readLine()) != null) {
                String encrypted = keyManager.encryptAes(line);
                out.writeUTF(encrypted);
                if (line.equalsIgnoreCase("exit")) break;
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Błąd inicjalizacji kluczy", e);
        }
    }

    public static void main(String[] args) throws IOException {
        new SecureClient("localhost",12345).start();
    }
}
