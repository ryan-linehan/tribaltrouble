package com.oddlabs.matchserver;

import java.time.Instant;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

public class DiscordBotService {

    private GatewayDiscordClient gateway;
    private static DiscordBotService instance;

    public static DiscordBotService getInstance() {
        if (instance == null) {
            instance = new DiscordBotService();
        }
        return instance;
    }

    public DiscordBotService() {

    }

    public void initialize(String token) {
        DiscordClient client = DiscordClient.create(token);

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
            this.gateway = gateway;
            // Extra discord things that can be done

            // Get connected servers
            gateway.getGuilds().subscribe(guild -> {
                System.out.printf("Connected to guild: %s (ID: %s)%n", guild.getName(), guild.getId().asString());
            });
            setupEventHandlers();
            return gateway.onDisconnect();
        });

        login.subscribe();
    }

    private void setupEventHandlers() {

        gateway.on(ReadyEvent.class, this::handleReady).subscribe();
        gateway.on(MessageCreateEvent.class, this::handleMessage).subscribe();
        // Add more event handlers as needed
    }

    public GatewayDiscordClient getGateway() {
        return gateway;
    }

    public void sendMessage(String channelId, String message) {
        if (gateway != null) {
            gateway.getChannelById(Snowflake.of(channelId))
                    .cast(MessageChannel.class)
                    .flatMap(channel -> channel.createMessage(message))
                    .subscribe();
        }
    }

    // Event handlers
    private Mono<Void> handleReady(ReadyEvent event) {
        final User self = event.getSelf();
        System.out.printf("Logged in as %s#%n", self.getUsername());
        return Mono.empty();
    }

    private Mono<Void> handleMessage(MessageCreateEvent event) {
        Message message = event.getMessage();
        String content = message.getContent();
        Instant timestamp = message.getTimestamp();
        String author = message.getAuthor().map(user -> user.getUsername()).orElse("Unknown");
        message.getAuthorAsMember()
                .map(member -> member.getNickname().orElse(member.getUsername()))
                .subscribe(nickname -> {
                    System.out.printf("Nickname: %s%n", nickname);
                });
        System.out.printf("Message from %s: %s (at %s)%n", author, content, timestamp);

        return Mono.empty();
    }

}
