package Connection;

import Connection.Client.SecureClient;
import Connection.Server.SecureServer;

public class Test {
    public static void main(String[] args) {
        try {
            // Uruchamianie serwera w osobnym wątku
            Thread serverThread = new Thread(() -> {
                try {
                    SecureServer server = new SecureServer();
                    server.init();

                    // Odbiór wiadomości od klienta
                    String receivedMessage = server.receiveMessage();
                    System.out.println("Serwer otrzymał: " + receivedMessage);

                    // Odpowiedź do klienta
                    server.sendMessage("Wiadomość odebrana przez serwer");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();

            Thread.sleep(5000);

            SecureClient client = new SecureClient();
            client.connect();

            // Wysyłanie wiadomości do serwera
            client.sendMessage("Cześć, serwerze!");

            // Odbiór odpowiedzi od serwera
            String response = client.receiveMessage();
            System.out.println("Klient otrzymał: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
