package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.DBInterface;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

public class RegisterProfileToDiscordUserCommand extends DiscordCommand {
    private String command_name = "register-user";
    private String command_description =
            "Registers a tribal trouble user profile to the Discord user. TT user must reply in"
                + " game.";
    private String command_option_profile_name = "profile_name";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        return event.deferReply().then(methodThatTakesALongTime(event));
    }

    /**
     * Static dicitonary of profiles awaiting to be processed by a user response. TODO: Should give
     * up after 1 minute?
     */
    private class ProfileRegistrationTimeout {
        long discord_user_id;
        String profile_name;

        public ProfileRegistrationTimeout(long discord_user_id, String profile_name) {
            this.discord_user_id = discord_user_id;
            this.profile_name = profile_name;
            // TODO: Add timeout
        }

        public String getNick() {
            return profile_name;
        }

        public long getDiscordUserId() {
            return discord_user_id;
        }
    }

    public static ConcurrentHashMap<String, Runnable> processingProfiles =
            new ConcurrentHashMap<>();

    public Mono<Void> methodThatTakesALongTime(ChatInputInteractionEvent event) {
        String profileToRegisterName =
                event.getOption(command_option_profile_name)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse(null);
        if (profileToRegisterName == null)
            return event.createFollowup("Unable to retrieve profile name to register.").then();
        profileToRegisterName = profileToRegisterName.toLowerCase();
        long discord_user_id = event.getInteraction().getUser().getId().asLong();
        if (DBInterface.isProfileRegisteredToDiscord(profileToRegisterName)) {
            return event.createFollowup("Profile is already registered to a Discord user.").then();
        }
        com.oddlabs.matchserver.Client client =
                (com.oddlabs.matchserver.Client)
                        com.oddlabs.matchserver.Client.getActiveClients()
                                .get(profileToRegisterName.toLowerCase());
        if (client != null) {
            // Found the client with the matching profile name
            // You can now send a private message or perform other actions
            System.out.println("Found active client with profile name: " + profileToRegisterName);
            final ProfileRegistrationTimeout registration =
                    new ProfileRegistrationTimeout(discord_user_id, profileToRegisterName);
            processingProfiles.put(
                    profileToRegisterName,
                    () -> {
                        DBInterface.registerProfileToDiscordUser(
                                registration.getNick(), registration.getDiscordUserId());
                        event.createFollowup(
                                        "Successfully registered profile: "
                                                + registration.getNick())
                                .then()
                                .subscribe();
                        processingProfiles.remove(registration.getNick());
                    });
            client.sendPrivateMessage(
                    profileToRegisterName,
                    "A discord user is requesting to register this profile. Allow this? Reply with"
                        + " y/n");
            return event.createFollowup(
                            "Sent registration request to profile: "
                                    + profileToRegisterName
                                    + ". Please respond in-game.")
                    .then();
        } else {
            // Handle not found
            System.out.println(
                    "Could not find active client with profile name: " + profileToRegisterName);
            return event.createFollowup("Failed to register profile: " + profileToRegisterName)
                    .then();
        }
        // Client.getActiveClients().entrySet().stream().filter(entry -> {
        // String profileName = entry.getKey();
        // Client client = entry.getValue();
        // // Add your filtering logic here
        // return false;
        // });
        // return Mono.delay(Duration.ofSeconds(30))
        //         .then(event.createFollowup("Long-running task completed!"))
        //         .then();
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        ApplicationCommandRequest registerUserCommand =
                ApplicationCommandRequest.builder()
                        .name(command_name)
                        .description(command_description)
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_profile_name)
                                        .description(
                                                "The in-game profile name to register to your"
                                                    + " Discord user")
                                        .type(
                                                ApplicationCommandOption.Type.STRING
                                                        .getValue()) // 3 is STRING type
                                        .required(true)
                                        .build())
                        .build();

        return registerUserCommand;
    }
}
