package de.yoshlix.discordbot;

import io.javalin.Javalin;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        BotController bot = new BotController();

        Javalin app = Javalin.create().start(8081);

        app.post("/api/v1/init", ctx -> {
            BotController.ConfigData config = ctx.bodyAsClass(BotController.ConfigData.class);
            bot.init(config);
            ctx.result("Initialized");
        });

        app.post("/api/v1/stop", ctx -> {
            bot.stop();
            ctx.result("Stopped");
            // System.exit(0); // Maybe not, depends if we want the service to stay alive
        });

        app.post("/api/v1/link", ctx -> {
            LinkRequest req = ctx.bodyAsClass(LinkRequest.class);
            boolean success = bot.linkPlayer(req.uuid, req.discordId);
            if (success)
                ctx.status(200).result("Linked");
            else
                ctx.status(400).result("Invalid Discord ID");
        });

        app.post("/api/v1/round/start", ctx -> {
            StartRoundRequest req = ctx.bodyAsClass(StartRoundRequest.class);
            bot.onRoundStart(req.teams);
            ctx.result("Round Started");
        });

        app.post("/api/v1/round/end", ctx -> {
            bot.onRoundEnd();
            ctx.result("Round Ended");
        });

        System.out.println("Discord Service running on port 8081");
    }

    public static class LinkRequest {
        public UUID uuid;
        public String discordId;
    }

    public static class StartRoundRequest {
        public Map<String, List<UUID>> teams;
    }
}
