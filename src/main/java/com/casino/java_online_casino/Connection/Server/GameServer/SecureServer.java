package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Prosty serwer obsługujący ECDH handshake i wymianę zaszyfrowanych wiadomości.
 */
public class SecureServer {
    private final int port;
    private final KeyManager keyManager;

    public SecureServer(int port) {
        this.port = port;
        this.keyManager = new KeyManager();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serwer nasłuchuje na porcie " + port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Połączenie od " + clientSocket.getInetAddress());

            try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
            ) {
                // 1. Wysyłamy nasz EC publiczny klucz
                String serverPub = Base64.getEncoder().encodeToString(
                    keyManager.getEcPublicKey().getEncoded()
                );
                out.writeUTF(serverPub);

                // 2. Odbieramy publiczny klucz klienta
                String clientPub = in.readUTF();
                byte[] clientPubBytes = Base64.getDecoder().decode(clientPub);
                PublicKey clientKey = KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(clientPubBytes));

                // 3. Derive shared secret
                keyManager.setForeignPublicKey(clientKey);
                keyManager.deriveSharedSecret();
                System.out.println("Wspólny klucz AES wyliczony");

                // 4. Odbiór i deszyfrowanie komunikatów
                while (true) {
                    String encrypted = in.readUTF();
                    String message = keyManager.decryptAes(encrypted);
                    System.out.println("Odebrano: " + message);
                    if (message.equalsIgnoreCase("exit")) break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws IOException {
        new SecureServer(12345).start();
    }
}