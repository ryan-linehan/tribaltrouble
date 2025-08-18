package com.oddlabs.matchserver.discord;

import com.oddlabs.matchserver.discord.commands.DiscordCommand;
import com.oddlabs.matchserver.discord.commands.LeaderboardsCommand;
import com.oddlabs.matchserver.discord.commands.MatchupCommand;
import com.oddlabs.matchserver.discord.commands.OnlineCommand;
import com.oddlabs.matchserver.discord.commands.RegisterProfileToDiscordUserCommand;
import com.oddlabs.matchserver.discord.commands.WhoIsCommand;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class DiscordBotService {

    Snowflake bot_id;
    private GatewayDiscordClient gateway;
    private static DiscordBotService instance;
    private boolean isInitialized = false;
    private long serverId = -1;
    private DiscordChatroomCoordinator chatroomCoordinator;

    ArrayList<DiscordCommand> commands = new ArrayList<DiscordCommand>();

    public static DiscordBotService getInstance() {
        if (instance == null) {
            instance = new DiscordBotService();
        }
        return instance;
    }

    public DiscordBotService() {}

    public void initialize(String token, long serverId) {
        if (isInitialized) return;
        this.serverId = serverId;
        DiscordClient client = DiscordClient.create(token);

        Mono<Void> login =
                client.withGateway(
                        (GatewayDiscordClient gateway) -> {
                            this.gateway = gateway;
                            // Extra discord things that can be done
                            bot_id = gateway.getSelfId();
                            setupEventHandlers(serverId);
                            commands.add(new LeaderboardsCommand());
                            commands.add(new MatchupCommand());
                            commands.add(new RegisterProfileToDiscordUserCommand());
                            commands.add(new WhoIsCommand());
                            commands.add(new OnlineCommand());
                            chatroomCoordinator = new DiscordChatroomCoordinator();
                            registerCommands();
                            // deleteCommands();
                            return gateway.onDisconnect();
                        });
        isInitialized = true;
        login.subscribe();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /** Gets the discord id associated with the bot user */
    Snowflake getBotId() {
        return bot_id;
    }

    /**
     * Gets the chatroom coordinator for managing discord to tribal trouble chatrooms. Returns
     * Optional.empty() if not initialized.
     */
    public Optional<DiscordChatroomCoordinator> getChatroomCoordinator() {
        return Optional.ofNullable(chatroomCoordinator);
    }

    private ArrayList<TextChannel> message_channels = new ArrayList<TextChannel>();

    /** Sets up event handlers for the Discord bot */
    private void setupEventHandlers(long serverId) {
        initTribalTroubleTextChannels(serverId);

        gateway.on(
                        ChatInputInteractionEvent.class,
                        event -> {
                            if (event.getCommandName().equals("ping")) {
                                return event.reply("Pong!");
                            }

                            return commands.stream()
                                    .filter(
                                            cmd ->
                                                    event.getCommandName()
                                                            .equals(cmd.getCommandName()))
                                    .findFirst()
                                    .map(cmd -> cmd.executeCommand(event))
                                    .orElseGet(
                                            () -> {
                                                System.out.println(
                                                        "No matching command found for: "
                                                                + event.getCommandName());
                                                return event.reply("Unknown command")
                                                        .withEphemeral(true);
                                            });
                        })
                .subscribe();
        gateway.on(ReadyEvent.class, this::handleReady).take(1).subscribe();
    }

    public GatewayDiscordClient getGateway() {
        return gateway;
    }

    /**
     * Gets the discord chatroom that is associated with the TT chat room based off the tribal
     * trouble chat room number
     */
    public TextChannel getDiscordChannelByTTRoomNumber(int roomNumber) {
        if (!isInitialized) return null;
        if (roomNumber < 1 || roomNumber > message_channels.size()) {
            return null;
        }
        return message_channels.get(roomNumber - 1);
    }

    /**
     * Initializes the message channels that can be used to relay messages to and from discord and
     * tribal trouble. Currently the discord channels must be name tt_chatroom_<number> where
     * <number> is the same as the tribal trouble chat room number
     */
    private void initTribalTroubleTextChannels(long serverId) {
        if (gateway != null) {
            message_channels.clear(); // Clear before repopulating
            gateway.getGuilds()
                    .filter(
                            guild -> {
                                return guild.getId().equals(Snowflake.of(serverId));
                            })
                    .doOnNext(
                            guild ->
                                    System.out.printf(
                                            "Found matching guild: %s%n", guild.getName()))
                    .flatMap(guild -> guild.getChannels())
                    .subscribe(
                            channel -> {
                                System.out.printf(
                                        "  - %s (ID: %s, Type: %s)%n",
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

    /** Deletes all existing commands for the bot in the specified server. */
    private void deleteCommands() {
        long guildId = serverId; // Discord4J's server ID.

        // Get the commands from discord as a Map
        Map<String, ApplicationCommandData> discordCommands =
                gateway.getRestClient()
                        .getApplicationService()
                        .getGuildApplicationCommands(1397851204410806363L, guildId)
                        .collectMap(ApplicationCommandData::name)
                        .block();

        for (ApplicationCommandData data : discordCommands.values()) {
            System.out.println("Deleting command: " + data.name());
            gateway.getRestClient()
                    .getApplicationService()
                    .deleteGuildApplicationCommand(
                            1397851204410806363L, guildId, data.id().asLong())
                    .subscribe();
        }
    }

    /**
     * Creates or validates that each command in {@link
     * com.oddlabs.matchserver.discord.DiscordBotService#commands} is registered with Discord.
     */
    private void registerCommands() {
        gateway.getRestClient()
                .getApplicationService()
                // TODO: Do not hard code the bot app id.
                .getGuildApplicationCommands(1397851204410806363L, serverId)
                .collectList()
                .subscribe(
                        existingCommands -> {
                            for (DiscordCommand command : commands) {
                                existingCommands.stream()
                                        .filter(cmd -> cmd.name().equals(command.getCommandName()))
                                        .findFirst()
                                        .ifPresentOrElse(
                                                cmd ->
                                                        System.out.println(
                                                                "Command already registered: "
                                                                        + cmd.name()),
                                                () -> {
                                                    System.out.println(
                                                            "Registering new command: "
                                                                    + command.getCommandName());
                                                    gateway.getRestClient()
                                                            .getApplicationService()
                                                            .createGuildApplicationCommand(
                                                                    gateway.getRestClient()
                                                                            .getApplicationId()
                                                                            .block(),
                                                                    serverId,
                                                                    command.getCommand())
                                                            .subscribe();
                                                });
                            }
                        });
    }

    public void createPingCommand() {
        ApplicationCommandRequest pingCommand =
                ApplicationCommandRequest.builder().name("ping").description("Pings").build();
        // Use your gateway field
        gateway.getRestClient()
                .getApplicationService()
                .createGuildApplicationCommand(
                        gateway.getRestClient().getApplicationId().block(), serverId, pingCommand)
                .subscribe();
    }
}
