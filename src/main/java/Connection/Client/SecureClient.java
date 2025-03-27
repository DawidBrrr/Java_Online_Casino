package Connection.Client;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Properties;

public class SecureClient {
    private PublicKey serverPublicKey;
    private SecretKey secretKey;
    Properties config;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;

    //Właściwości połączenia
    int serverPort;
    String serverHost;


    public SecureClient() throws NoSuchAlgorithmException, IOException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        secretKey = keyGen.generateKey();
        if(!initProperties()){
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

    public void connect() throws Exception {
        socket = new Socket(serverHost, serverPort);
        System.out.println("Połączono z serwerem");

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        serverPublicKey = (PublicKey) in.readObject();
        System.out.println("Otrzymano klucz publiczny serwera");

        // Szyfrowanie i wysłanie klucza AES
        byte[] encryptedAESKey = encryptRSA(secretKey.getEncoded(), serverPublicKey);
        out.writeObject(encryptedAESKey);
        out.flush();
        System.out.println("Klucz AES wysłany do serwera");
    }

    private byte[] encryptRSA(byte[] data, PublicKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private byte[] encryptAES(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data.getBytes());
    }

    private String decryptAES(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(data));
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
        SecureClient client = new SecureClient();
        client.connect();

        // Przykładowa komunikacja
        client.sendMessage("Witaj, serwerze!");
        String response = client.receiveMessage();
        System.out.println("Odpowiedź serwera: " + response);
    }
}
