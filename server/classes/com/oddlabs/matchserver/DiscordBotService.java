package com.oddlabs.matchserver;

import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.Channel;

import reactor.core.publisher.Mono;
import reactor.core.Disposable;

public class DiscordBotService {

    Snowflake bot_id;
    private GatewayDiscordClient gateway;
    private static DiscordBotService instance;
    private boolean isInitialized = false;

    public static DiscordBotService getInstance() {
        if (instance == null) {
            instance = new DiscordBotService();
        }
        return instance;
    }

    public DiscordBotService() {

    }

    public void initialize(String token, long serverId) {
        DiscordClient client = DiscordClient.create(token);

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
            this.gateway = gateway;
            // Extra discord things that can be done
            bot_id = gateway.getSelfId();
            setupEventHandlers(serverId);
            return gateway.onDisconnect();
        });
        isInitialized = true;
        login.subscribe();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Gets the discord id associated with the bot user
     */
    Snowflake getBotId() {
        return bot_id;
    }

    private ArrayList<TextChannel> message_channels = new ArrayList<TextChannel>();

    /**
     * Sets up event handlers for the Discord bot
     */
    private void setupEventHandlers(long serverId) {
        initTribalTroubleTextChannels(serverId);
        gateway.on(ReadyEvent.class, this::handleReady).subscribe();
    }

    public GatewayDiscordClient getGateway() {
        return gateway;
    }

    /**
     * Gets the discord chatroom that is associated with the TT chat room based
     * off the tribal trouble chat room number
     */
    public TextChannel getDiscordChannelByTTRoomNumber(int roomNumber) {
        if(!isInitialized)
            return null;
        if (roomNumber < 1 || roomNumber > message_channels.size()) {
            return null;
        }
        return message_channels.get(roomNumber - 1);
    }

    /**
     * Initializes the message channels that can be used to relay messages to
     * and from discord and tribal trouble. Currently the discord channels must
     * be name tt_chatroom_<number> where <number> is the same as the tribal
     * trouble chat room number
     */
    private void initTribalTroubleTextChannels(long serverId) {
        if (gateway != null) {
            gateway.getGuilds()
                    .doOnNext(guild -> System.out.printf("Channels in guild: %s%n", guild.getName()))
                    .filter(guild -> {
                        System.out.printf("Checking guild ID: %s against server ID: %d%n", guild.getId().asString(), serverId);
                        return guild.getId().equals(Snowflake.of(serverId));
                    })
                    .flatMap(guild -> guild.getChannels())
                    .take(1)
                    .subscribe(channel -> {
                        System.out.printf("  - %s (ID: %s, Type: %s)%n",
                                channel.getName(),
                                channel.getId().asString(),
                                channel.getType().name());

                        if (channel.getType() == Channel.Type.GUILD_TEXT) {
                            if (channel.getName().indexOf("tt_chatroom_") != -1) {
                                message_channels.add((TextChannel) channel);
                                System.out.println("Added channel: " + channel.getName());
                            }
                        }
                    });
        }
    }

    // Event handlers
    private Mono<Void> handleReady(ReadyEvent event) {
        final User self = event.getSelf();
        System.out.printf("Logged in as %s#%n", self.getUsername());
        return Mono.empty();
    }
}
