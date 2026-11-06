package tn.fss.devappreparties;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Serveur de discussion UDP : chaque client est identifie par son adresse et son port. */
public final class ChatServer {
    public static final int DEFAULT_PORT = 1234;
    private static final int MAX_DATAGRAM_SIZE = 65_507;

    private ChatServer() { }

    public static void main(String[] args) {
        int port;
        try {
            port = args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
            if (port < 1 || port > 65_535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println("Usage : ChatServer [port entre 1 et 65535]");
            return;
        }
        Set<SocketAddress> clients = ConcurrentHashMap.newKeySet();

        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.bind(new InetSocketAddress(port));
            System.out.printf("Serveur UDP en ecoute sur le port %d...%n", port);

            byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
            while (!socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), packet.getOffset(),
                        packet.getLength(), StandardCharsets.UTF_8);
                SocketAddress sender = packet.getSocketAddress();
                System.out.printf("[%s] %s%n", sender, message);

                // L'etape 2 du TP consiste seulement a afficher le message recu.
                if ("/quit".equalsIgnoreCase(message.trim())) {
                    continue;
                }

                clients.add(sender);
                byte[] outgoing = packet.getData();
                for (SocketAddress client : clients) {
                    if (!client.equals(sender)) {
                        socket.send(new DatagramPacket(outgoing, packet.getLength(), client));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur du serveur UDP : " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Usage : ChatServer [port]");
        }
    }
}
