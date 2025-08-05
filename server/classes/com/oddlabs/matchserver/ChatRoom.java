package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.MatchmakingServerInterface;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.core.Disposable;

import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.matchmaking.ChatRoomUser;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

public final strictfp class ChatRoom {
    public static class MessageTuple {
        public final String author;
        public final String message;
        
        public MessageTuple(String author, String message) {
            this.author = author;
            this.message = message;
        }
    }
    
    private final static Map<String, ChatRoom> chat_rooms = new HashMap();
    private static final int MAX_MESSAGES = 100;
    private MessageTuple[] messages = new MessageTuple[MAX_MESSAGES];
    private int currentIndex = 0;
    private int messageCount = 0;
    private final Set<Client> users = new HashSet();
    private final String name;
    private TextChannel discordChannel = null;
    private Disposable discordSubscription = null;

    public ChatRoom(String name) {
        this.name = name;
        discordChannel = DiscordBotService.getInstance().getDiscordChannelByTTRoomNumber(Integer.parseInt(name.substring("Chatroom".length())));
        if (discordChannel == null) {
            System.err.println("Discord channel for " + name + " not found!");
        } else {
            discordSubscription = discordChannel.getClient().on(MessageCreateEvent.class, this::handleIncomingDiscordMessage).subscribe();
            System.out.println("Discord channel for " + name + " found: " + (discordChannel).getName());
        }
    }

    public final static Map getChatRooms() {
        return chat_rooms;
    }

    /**
     * Joins a standard chat room for the client. This method will find the
     * first chat room that is not more than half full and join the client to
     * it. If no such room exists, a new one will be created.
     *
     * @param client
     */
    public final static void joinStandardChatRoom(Client client) {
        // <= so that if all rooms are 1/2 full it creates a new one
        for (int i = 0; i <= chat_rooms.size(); i++) {
            String room_name = "Chatroom" + i + 1;
            ChatRoom room = getChatRoom(room_name);
            // skip room if its already half full and make another
            if (room.getUsers().size() > MatchmakingServerInterface.MAX_ROOM_USERS / 2) {
                continue;
            }

            client.joinRoom(room_name);
            // Send existing messages to the new client
            for (int j = 0; j < room.messageCount; j++) {
                int index = (room.currentIndex - room.messageCount + j + MAX_MESSAGES) % MAX_MESSAGES;
                MessageTuple message = room.messages[index];
                if (message != null) {
                    System.out.println("Sending message to client: " + message.author + ": " + message.message);
                    client.getClientInterface().receiveChatRoomMessage(message.author, message.message);
                }
            }
            return; // Client has joined a room
        }
    }

    /**
     * Gets a chat room by name, creating it if it does not exist.
     *
     * @param room_name
     * @return
     */
    public final static ChatRoom getChatRoom(String room_name) {
        ChatRoom room = (ChatRoom) chat_rooms.get(room_name);
        if (room == null) {
            room = new ChatRoom(room_name);
            chat_rooms.put(room_name, room);
        }
        return room;
    }

    public final static boolean isNameValid(String name) {
        return name != null && name.length() <= MatchmakingServerInterface.MAX_ROOM_NAME_LENGTH
                && name.length() >= MatchmakingServerInterface.MIN_ROOM_NAME_LENGTH && areCharactersValid(name);
    }

    private final static boolean areCharactersValid(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (MatchmakingServerInterface.ALLOWED_ROOM_CHARS.indexOf(name.charAt(i)) == -1) {
                return false;
            }
        }
        return true;
    }

    public final void join(Client client) {
        // TODO check for size!!!!
        users.add(client);
        sendUsers();
    }

    public final void sendUsers() {
        Iterator it = users.iterator();
        ChatRoomUser[] chat_room_users = new ChatRoomUser[users.size()];
        int i = 0;
        while (it.hasNext()) {
            Client client = (Client) it.next();
            chat_room_users[i] = new ChatRoomUser(client.getProfile().getNick(), client.isPlaying());
            i++;
        }
        it = users.iterator();
        while (it.hasNext()) {
            Client client = (Client) it.next();
            client.getClientInterface().receiveChatRoomUsers(chat_room_users);
        }
    }

    private String formatChat(String owner, String message) {
        return "<" + owner + "> " + message;
    }

    /**
     * Sends a message to all connected users in the chat room.
     *
     * @param owner
     * @param msg
     */
    public final void sendMessage(String owner, String msg) {
        addMessage(new MessageTuple(owner, msg));
        for (Client client : users) {
            MatchmakingClientInterface ci = client.getClientInterface();
            ci.receiveChatRoomMessage(owner, msg);
        }
    }

    private void addMessage(MessageTuple message) {
        messages[currentIndex] = message;
        currentIndex = (currentIndex + 1) % MAX_MESSAGES;
        if (messageCount < MAX_MESSAGES) {
            messageCount++;
        }
    }

    /**
     * Sends a discord message into the discord channel associated with this
     * tribal trouble chat room
     */
    public final void trySendDiscordMessage(String owner, String msg) {
        // Send the message to the discord channel if one is setup for this chat room
        if (this.discordChannel != null) {
            if(!msg.startsWith("<"))
                msg = formatChat(owner, msg);
            this.discordChannel.createMessage(msg).retry(3).subscribe();
        }
    }

    public final void trySendDiscordEmbed(EmbedCreateSpec embed) {
        // Send the embed to the discord channel if one is setup for this chat room
        if (this.discordChannel != null) {
            this.discordChannel.createMessage(embed).retry(3).subscribe();
        }
    }

    public final Set getUsers() {
        return users;
    }

    public final void leave(Client client) {
        if (users.contains(client)) {
            users.remove(client);
            if (users.size() == 0) {
                chat_rooms.remove(getName());
                cleanup();
            } else {
                sendUsers();
            }
        }
    }

    /**
     * Cleanup method to dispose of Discord subscription and prevent memory
     * leaks
     */
    public final void cleanup() {
        if (discordSubscription != null && !discordSubscription.isDisposed()) {
            System.out.println("Disposing Discord subscription for chat room " + name);
            discordSubscription.dispose();
            discordSubscription = null;
        }
    }

    public final String getName() {
        return name;
    }

    /**
     * Sends a message to the chat room associated with the discord channel that
     * the message was sent through
     *
     * @param event The message event containing the message details from
     * discord
     * @return
     */
    private Mono<Void> handleIncomingDiscordMessage(MessageCreateEvent event) {
        System.out.println("handling on chatroom" + name);
        if (this.discordChannel != null) {
            Message message = event.getMessage();

            if (message.getChannelId().equals(discordChannel.getId())) {
                System.out.println("Message in channel tt discord channel posted");
                // Handle the message
                String content = message.getContent();
                Instant timestamp = message.getTimestamp();
                Snowflake authorId = message.getAuthor().get().getId();
                boolean isMessageFromBot = authorId.equals(DiscordBotService.getInstance().getBotId());
                if (!isMessageFromBot) {
                    System.out.println("Message from " + authorId + ": " + content + " at " + timestamp);
                    String author = "@" + message.getAuthor().map(user -> user.getUsername()).orElse("Unknown");
                    System.out.println("Queueing Discord message for main thread processing");
                    sendMessage(author, content);
                }
            }
        }

        return Mono.empty();
    }
}
