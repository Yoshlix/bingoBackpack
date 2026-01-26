package de.yoshlix.discordbot;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        BotController bot = new BotController();

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        });

        // Add exception handler for better error reporting
        app.exception(Exception.class, (e, ctx) -> {
            LOGGER.error("Unhandled exception in endpoint {}", ctx.path(), e);
            ctx.status(500).result("Internal Server Error: " + e.getMessage());
        });

        app.post("/api/v1/init", ctx -> {
            try {
                BotController.ConfigData config = ctx.bodyAsClass(BotController.ConfigData.class);
                if (config == null) {
                    ctx.status(400).result("Invalid request body");
                    return;
                }
                bot.init(config);
                ctx.result("Initialized");
            } catch (Exception e) {
                LOGGER.error("Error in /init endpoint", e);
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.post("/api/v1/stop", ctx -> {
            try {
                bot.stop();
                ctx.result("Stopped");
            } catch (Exception e) {
                LOGGER.error("Error in /stop endpoint", e);
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.post("/api/v1/link", ctx -> {
            try {
                LinkRequest req = ctx.bodyAsClass(LinkRequest.class);
                if (req == null || req.uuid == null || req.discordId == null) {
                    ctx.status(400).result("Invalid request: missing uuid or discordId");
                    return;
                }
                boolean success = bot.linkPlayer(req.uuid, req.discordId);
                if (success) {
                    ctx.status(200).result("Linked");
                } else {
                    ctx.status(400).result("Invalid Discord ID");
                }
            } catch (Exception e) {
                LOGGER.error("Error in /link endpoint", e);
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.post("/api/v1/round/start", ctx -> {
            try {
                StartRoundRequest req = ctx.bodyAsClass(StartRoundRequest.class);
                if (req == null) {
                    ctx.status(400).result("Invalid request body");
                    return;
                }
                bot.onRoundStart(req.teams);
                ctx.result("Round Started");
            } catch (Exception e) {
                LOGGER.error("Error in /round/start endpoint", e);
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.post("/api/v1/round/end", ctx -> {
            try {
                bot.onRoundEnd();
                ctx.result("Round Ended");
            } catch (Exception e) {
                LOGGER.error("Error in /round/end endpoint", e);
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        // Health check endpoint
        app.get("/health", ctx -> {
            ctx.result("OK");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Discord Service...");
            bot.stop();
        }));

        app.start(8081);
        LOGGER.info("Discord Service running on port 8081");
    }

    public static class LinkRequest {
        public UUID uuid;
        public String discordId;
    }

    public static class StartRoundRequest {
        public Map<String, List<UUID>> teams;
    }
}
