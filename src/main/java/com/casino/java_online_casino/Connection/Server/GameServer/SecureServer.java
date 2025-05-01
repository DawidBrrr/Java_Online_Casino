    package com.casino.java_online_casino.Connection.Server.GameServer;

    import com.casino.java_online_casino.Connection.Server.ServerConfig;
    import com.casino.java_online_casino.Experimental;

    import javax.crypto.SecretKey;
    import javax.crypto.spec.SecretKeySpec;
    import java.io.*;
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
@Experimental

    public class SecureServer {
        private final ExecutorService executor;

        public SecureServer() {
            this.executor = Executors.newCachedThreadPool();
        }

        public void start() {
            try ( ServerSocket serverSocket = new ServerSocket(ServerConfig.getGameServerPort())) {
                System.out.println("Serwer nasłuchuje na porcie: " + ServerConfig.getGameServerPort());

                Socket clientSocket = serverSocket.accept();
                System.out.println("Połączono z klientem: " + clientSocket.getInetAddress());

                try (
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
                ) {
//                    // Wysyłanie klucza publicznego do klienta
//                    sendPublicKey();
//
//                    // Odbiór i deszyfracja klucza AES
//                    receiveAESKey();



//
//                    // Komunikacja z klientem
//                    handleCommunication();

                } catch (Exception e) {
                    System.err.println("Błąd w obsłudze klienta: " + e.getMessage());
                }

            } catch (IOException e) {
                System.err.println("Nie udało się uruchomić serwera: " + e.getMessage());
            }
        }

//        private void sendPublicKey() throws IOException {
//            out.writeObject(keyManager.getPublicKey().getEncoded());
//            out.flush();
//            System.out.println("Wysłano klucz publiczny RSA do klienta.");
//        }
//
//        private void receiveAESKey() throws Exception {
//            String encryptedAESKey = (String)in.readObject();
//            byte[] aesKeyBytes = keyManager.decryptRSA(encryptedAESKey);
//            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
//            keyManager.setAESKey(aesKey);
//            System.out.println("Otrzymano i odszyfrowano klucz AES.");
//        }
//
//        private void handleCommunication() throws Exception {
//            String received = receiveMessage();
//            System.out.println("Odebrano: " + received);
//
//            sendMessage("Wiadomość odebrana");
//        }
//
//        public void sendMessage(String message) throws Exception {
//            byte[] encrypted = keyManager.encryptAES(message);
//            out.writeObject(encrypted);
//            out.flush();
//        }
//
//        public String receiveMessage() throws Exception {
//            byte[] encrypted = (byte[]) in.readObject();
//            return keyManager.decryptAES(encrypted);
//        }
    }
