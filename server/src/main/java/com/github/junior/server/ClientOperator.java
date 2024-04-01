package com.github.junior.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientOperator implements Runnable {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    public static ArrayList<ClientOperator> clients = new ArrayList<>();

    public ClientOperator(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clients.add(this);
            //TODO: ...
            name = bufferedReader.readLine();
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        }
        catch (Exception e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаление клиента из коллекции
     */
    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    /**
     * Отправка сообщения всем слушателям
     *
     * @param message сообщение
     */
    private void broadcastMessage(String message) {
        try {
            if (!message.contains(": @")) {
                for (ClientOperator client : clients) {
                    if (!client.equals(this) && message != null) {
                        //if (!client.name.equals(name) && message != null) {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                }
            } else {
                String senderName = message.substring(0, message.indexOf(':'));
                String sub = message.substring(message.indexOf('@'));
                String recipientName = sub.substring(1, sub.indexOf(' '));

                List<ClientOperator> senderObj = clients
                        .stream()
                        .filter(client -> client.name.equals(senderName))
                        .toList();
                List<ClientOperator> recipientObj = clients
                        .stream()
                        .filter(client -> client.name.equals(recipientName))
                        .toList();

                if (recipientObj.isEmpty()) {
                    senderObj.get(0).bufferedWriter.write("Такого пользователя нет в чате.");
                    senderObj.get(0).bufferedWriter.newLine();
                    senderObj.get(0).bufferedWriter.flush();
                } else if (senderName.equals(recipientName)) {
                    senderObj.get(0).bufferedWriter.write("Нельзя отправить сообщение себе.");
                    senderObj.get(0).bufferedWriter.newLine();
                    senderObj.get(0).bufferedWriter.flush();
                } else {
                    recipientObj.get(0).bufferedWriter.write(message);
                    recipientObj.get(0).bufferedWriter.newLine();
                    recipientObj.get(0).bufferedWriter.flush();
                }
            }
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String massageFromClient;
        while (!socket.isClosed()) {
            try {
                // Чтение данных
                massageFromClient = bufferedReader.readLine();
                /* Для Linux, MacOS
                if (massageFromClient == null){
                    closeEverything(socket, bufferedReader, BufferedWriter);
                    break;
                 }*/
                // Отправка данных всем слушателям
                broadcastMessage(massageFromClient);
            }
            catch (Exception e){
                closeEverything(socket, bufferedReader, bufferedWriter);
                //break;
            }
        }
    }
}
