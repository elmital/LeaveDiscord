package be.elmital.leaveDiscord;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;


public class leaveDiscord extends ListenerAdapter {
    private final JDABuilder jdaBuilder;
    private JDA jda;

    public static void main(String[] args) {
        LoggerFactory.getLogger(leaveDiscord.class);
        System.out.println(">>>>> Leave Discord <<<<<<");

        final boolean[] running = {true};
        while (running[0]) {
            var token = scanString("Enter the bot token : ");
            System.out.println("Bot launching!");
            CompletableFuture.supplyAsync(() -> new leaveDiscord(token))
                    .thenApplyAsync(bot -> {
                        try {
                            Thread.sleep(5000);
                            return bot;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            running[0] = false;
                            return null;
                        }
                    }).thenAcceptAsync(bot -> {
                        if (bot.jda == null || !bot.jda.getStatus().isInit()) {
                            System.out.println("Bot isn't init properly");
                            running[0] = false;
                        } else {
                            System.out.println();
                            System.out.println("Bot is actually connected to : ");
                            for (Guild guild : bot.jda.getGuilds()) {
                                System.out.println(guild.getIdLong());
                            }

                            while (true) {
                                String stringID = scanString("Enter guild ID to disconnect from or \"quit\" to stop the bot : ");
                                if (stringID.matches("^quit")) {
                                    running[0] = false;
                                    break;
                                }

                                long longId;
                                try {
                                    longId = Long.parseLong(stringID);
                                } catch (NumberFormatException nfe) {
                                    System.out.println("invalid ID");
                                    continue;
                                }

                                var guild = bot.jda.getGuildById(longId);
                                if (guild != null) {
                                    guild.leave().queue();
                                    System.out.println("Bot should be disconnected soon!");
                                } else {
                                    System.out.println("Can't find a guild with id " + longId);
                                }
                            }

                            bot.stop();
                        }
                    }).join();
        }
    }

    /**
     * Method for build and setup the bot
     */
    public leaveDiscord(String botToken) {
        System.out.println("Bot activation...");
        //Build JDA
        jdaBuilder = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .addEventListeners(this)
                .setActivity(Activity.playing("watching you"));

        build();
    }

    /*
        JDA events
     */
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Bot ready !");
        start();
    }

    @Override
    //the event can be fired by a 4900 close code which is a reconnection of the bot see resumedEvent
    public void onReconnected(@NotNull ReconnectedEvent event) {
        System.out.println("Bot is reconnected ");
    }

    @Override
    //the event is fired after a 4900 close code so re-enable the bot
    public void onResumed(@NotNull ResumedEvent event) {
        System.out.println("Bot connection resumed ");
    }

    @Override
    //Discord disconnect and reconnect(4900) at many times see onResume for the 4900 close code
    public void onDisconnect(@NotNull DisconnectEvent event) {
        if(event.getCloseCode() == null) {
            if(event.isClosedByServer())
                System.out.println("Disconnected: connection was closed by the server");
            else
                System.out.println("Disconnected: connection was closed for an unknown reason");
            return;
        }

        if(event.getCloseCode().getCode() == 4900)
            System.out.println("Disconnected: " + event.getCloseCode());
        else
            System.out.println("Disconnected: " + event.getCloseCode());
    }

    @Override
    //Shutdown doesn't terminate JVM instance and need to be shutting down manually
    public void onShutdown(@NotNull ShutdownEvent event) {
        System.out.println("Shutdown old JVM instance");

        if (jda != null) {
            OkHttpClient client = jda.getHttpClient();
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
        }
    }

    /*
        Bot management
     */
    /**
     * Method use to build the JDA
     */
    private void build() {
        System.out.println("Building a new jda logging in...");
        try {
            jda = jdaBuilder.build();
            System.out.println("Success build");
        } catch (LoginException | IllegalArgumentException e) {
            System.out.println("Unable to authenticate the bot, check the token !");
        }
    }

    /**
     * Method use to start the bot fired when bot ready
     * @see #onReady(ReadyEvent) event
     */
    private void start() {
        synchronized (this) {
            System.out.println("Starting the bot...");

            if (jda.getGuilds().isEmpty()) {
                System.out.println("Guilds are empty, bot isn't not connected to a guild");
                return;
            }
        }

        loadFeatures();
        System.out.println("Bot started");
    }

    /**
     * Restart the bot, stop it and rebuild the JDA
     */
    public void restart() {
        System.out.println("Restarting...");

        stop();
        build();
    }

    /**
     * Stopping the bot
     */
    public void stop() {
        System.out.println("Stopping DiscordBot...");

        jda.shutdown();
        System.out.println("Bot stopped");
    }

    /**
     * Features loading, some features use cache and may need to be loaded after the bot is totally setup
     */
    private void loadFeatures() {
        //NOTHING FOR THE MOMENT
    }

    /*
        Utils
     */
    public static String scanString(String output) {
        String toReturn = null;
        Scanner sc = new Scanner(System.in);
        System.out.println();

        while (toReturn == null) {
            System.out.print(output);
            try {
                toReturn = sc.nextLine();
            } catch (InputMismatchException ignored) {
                System.out.println("\r" + "invalid parameter");
                sc = new Scanner(System.in);
            }
        }

        return toReturn;
    }
}
