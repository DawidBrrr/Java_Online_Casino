package Connection.Server;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.util.Properties;

public class SecureServer {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKey secretKey;
    private Properties config;
    private int serverPort;
    private String serverHost;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerSocket serverSocket;
    private Socket socket;

    public SecureServer() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator pair = KeyPairGenerator.getInstance("RSA");
        pair.initialize(1024);
        KeyPair keyPair = pair.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        if (!initProperties()) {
            throw new IOException();
        }
    }

    public boolean initProperties() {
        String path = "src/main/resources/connection.properties";
        config = new Properties();
        try {
            config.load(Files.newInputStream(Path.of(path), StandardOpenOption.READ));
            serverHost = config.getProperty("serverName");
            serverPort = Integer.parseInt(config.getProperty("serverPort"));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void init() throws Exception {
        serverSocket = new ServerSocket(serverPort);
        System.out.println("Serwer nasłuchuje na porcie " + serverPort);
        socket = serverSocket.accept();
        System.out.println("Klient połączony");

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(publicKey);
        out.flush();

        // Odbieranie klucza AES od klienta
        byte[] encryptedAESKey = (byte[]) in.readObject();
        byte[] aesKeyBytes = decryptRSA(encryptedAESKey);
        secretKey = new SecretKeySpec(aesKeyBytes, "AES");
        System.out.println("Klucz sesyjny AES otrzymany i odszyfrowany");
    }

    private byte[] decryptRSA(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    private String decryptAES(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(data));
    }

    private byte[] encryptAES(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data.getBytes());
    }

    public void sendMessage(String message) throws Exception {
        if (out == null) return;
        byte[] encryptedMessage = encryptAES(message);
        out.writeObject(encryptedMessage);
        out.flush();
    }

    public String receiveMessage() throws Exception {
        if (in == null) return null;
        byte[] encryptedMessage = (byte[]) in.readObject();
        return decryptAES(encryptedMessage);
    }

    public static void main(String[] args) throws Exception {
        SecureServer server = new SecureServer();
        server.init();

        // Przykładowa komunikacja
        String receivedMessage = server.receiveMessage();
        System.out.println("Otrzymano: " + receivedMessage);

        server.sendMessage("Wiadomość odebrana");
    }
}
