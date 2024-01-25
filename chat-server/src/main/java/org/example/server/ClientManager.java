package org.example.server;

import javax.imageio.IIOException;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;

    public static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        }
        catch (IOException e) {

        }
    }

    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    @Override
    public void run() {
        String messageFromClient;
        try {
            while (socket.isConnected()) {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                broadcastMessage(messageFromClient);
            }
        }
        catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Для отравки личного сообщения используйте синтаксис: "@Имя_пользователя Ваше сообщение".
    // Если пользователя с таким именем нет, сообщение отправляется в общий чат.
    private void broadcastMessage(String message) {
        if (message.startsWith("@")) {
            String nameOfReceiver = message.substring(1, message.indexOf(" "));
            for (ClientManager client : clients) {
                if (client.name.equals(nameOfReceiver)) {
                    try {
                        client.bufferedWriter.write("Лично от " + name + ":"
                                + message.substring(nameOfReceiver.length()+1));
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                    catch (IOException e) {
                        System.out.println("Сообщение для " + nameOfReceiver +  " не доставлено");
                    }
                    return;
                }
            }
            try {
                bufferedWriter.write("Пользователь " + nameOfReceiver + " не найден");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
            catch (IOException e) { e.getMessage(); }
        }
        else {
            for (ClientManager client : clients) {
                try {
                    if (!client.name.equals(name) && message != null) {
                        client.bufferedWriter.write((message.startsWith("Server") ? "" : name + ":") + message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }
    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClient();
        try {
            if (bufferedReader != null)
                bufferedReader.close();
            if (bufferedWriter != null)
                bufferedWriter.close();
            if (socket != null)
                socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
