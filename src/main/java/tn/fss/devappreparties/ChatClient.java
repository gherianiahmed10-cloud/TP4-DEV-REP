package tn.fss.devappreparties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatClient {
    private static final int DEFAULT_PORT = ChatServer.DEFAULT_PORT;
    private static final int MAX_DATAGRAM_SIZE = 65_507;

    private ChatClient() { }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port;
        try {
            port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
            if (port < 1 || port > 65_535) {
                throw new NumberFormatException();
            }
            run(host, port);
        } catch (NumberFormatException e) {
            System.err.println("Usage : ChatClient [hote] [port]");
        } catch (IOException e) {
            System.err.println("Erreur du client UDP : " + e.getMessage());
        }
    }

    private static void run(String host, int port) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.print("Nom d'utilisateur : ");
        String username = console.readLine();
        if (username == null || username.isBlank()) {
            System.out.println("Un nom d'utilisateur est requis.");
            return;
        }

        InetAddress address = InetAddress.getByName(host);
        SocketAddress server = new InetSocketAddress(address, port);
        AtomicBoolean running = new AtomicBoolean(true);

        try (DatagramSocket socket = new DatagramSocket()) {
            Thread receiver = new Thread(() -> receiveMessages(socket, running), "chat-receiver");
            receiver.setDaemon(true);
            receiver.start();

            System.out.println("Chat UDP vers " + server + " (tapez /quit pour quitter).");
            while (running.get()) {
                String line = console.readLine();
                if (line == null) {
                    line = "/quit";
                }
                String message = "[" + username + "] : " + line;
                byte[] data = message.getBytes(StandardCharsets.UTF_8);
                if (data.length > MAX_DATAGRAM_SIZE) {
                    System.out.println("Message trop long pour un datagramme UDP.");
                    continue;
                }
                socket.send(new DatagramPacket(data, data.length, server));
                if ("/quit".equals(line.trim().toLowerCase(Locale.ROOT))) {
                    running.set(false);
                    break;
                }
            }
        }
    }

    private static void receiveMessages(DatagramSocket socket, AtomicBoolean running) {
        byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
        while (running.get() && !socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), packet.getOffset(),
                        packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("\n" + message);
                System.out.print("> ");
            } catch (IOException e) {
                if (running.get() && !socket.isClosed()) {
                    System.err.println("Reception interrompue : " + e.getMessage());
                }
                return;
            }
        }
    }
}
