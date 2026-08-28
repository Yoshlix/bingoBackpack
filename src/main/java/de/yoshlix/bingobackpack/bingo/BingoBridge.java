package de.yoshlix.bingobackpack.bingo;

import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.IBingoApi;
import me.jfenn.bingo.api.card.ICard;
import me.jfenn.bingo.api.card.ICardService;
import me.jfenn.bingo.api.data.BingoGameStatus;
import me.jfenn.bingo.api.data.IBingoGame;
import me.jfenn.bingo.api.data.IBingoTeam;
import me.jfenn.bingo.api.ext.BingoExtApi;
import me.jfenn.bingo.api.ext.IBingoCardExt;
import me.jfenn.bingo.api.ext.IBingoExt;
import me.jfenn.bingo.api.ext.IBingoScoringService;
import me.jfenn.bingo.api.ext.ICardEntryView;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Single point of access to Yet Another Bingo.
 *
 * Everything the mod needs goes through here so that API changes on the bingo
 * side stay confined to one file. All accessors return null (or an empty list)
 * while no bingo game is loaded, rather than throwing.
 *
 * Most of this delegates straight to upstream's public API; only scoring and
 * card introspection come from the fork's extension ({@link BingoExtApi}),
 * which upstream does not provide.
 */
public final class BingoBridge {

    private BingoBridge() {
    }

    // ---------------------------------------------------------------- core

    /** The bingo API, or null if no game is loaded. */
    public static IBingoApi api() {
        return BingoApi.getINSTANCE();
    }

    /** True if bingo is loaded and ready to be queried. */
    public static boolean isAvailable() {
        return BingoApi.getINSTANCE() != null;
    }

    /** The current game, or null if bingo is not loaded. */
    public static IBingoGame game() {
        IBingoApi api = BingoApi.getINSTANCE();
        return api == null ? null : api.getGame();
    }

    /** The current game status, or null if bingo is not loaded. */
    public static BingoGameStatus status() {
        IBingoGame game = game();
        return game == null ? null : game.getStatus();
    }

    /** True if a round is currently being played. */
    public static boolean isPlaying() {
        return status() == BingoGameStatus.PLAYING;
    }

    // --------------------------------------------------------------- teams

    /**
     * The team the given player belongs to, or null if they are not on a team
     * (or bingo is not loaded).
     *
     * Upstream's IBingoTeams is only Iterable, so the lookup lives here.
     */
    public static IBingoTeam getTeamForPlayer(UUID playerId) {
        IBingoApi api = BingoApi.getINSTANCE();
        if (api == null || playerId == null) {
            return null;
        }
        for (IBingoTeam team : api.getTeams()) {
            if (team.getPlayers().contains(playerId)) {
                return team;
            }
        }
        return null;
    }

    /** The team with the given ID, or null if there is no such team. */
    public static IBingoTeam getTeamById(String teamId) {
        IBingoApi api = BingoApi.getINSTANCE();
        if (api == null || teamId == null) {
            return null;
        }
        for (IBingoTeam team : api.getTeams()) {
            if (teamId.equals(team.getId())) {
                return team;
            }
        }
        return null;
    }

    /** All registered teams; empty while bingo is not loaded. */
    public static List<IBingoTeam> getAllTeams() {
        IBingoApi api = BingoApi.getINSTANCE();
        if (api == null) {
            return Collections.emptyList();
        }
        List<IBingoTeam> result = new java.util.ArrayList<>();
        for (IBingoTeam team : api.getTeams()) {
            result.add(team);
        }
        return result;
    }

    /** All teams except the one with the given ID. */
    public static List<IBingoTeam> getEnemyTeams(String ownTeamId) {
        List<IBingoTeam> result = new java.util.ArrayList<>();
        for (IBingoTeam team : getAllTeams()) {
            if (!team.getId().equals(ownTeamId)) {
                result.add(team);
            }
        }
        return result;
    }

    // --------------------------------------------------------------- cards

    /** Upstream's card service, or null if bingo is not loaded. */
    public static ICardService cards() {
        IBingoApi api = BingoApi.getINSTANCE();
        return api == null ? null : api.getCards();
    }

    /** The active card, or null if bingo is not loaded. */
    public static ICard getActiveCard() {
        ICardService cards = cards();
        return cards == null ? null : cards.getActiveCard();
    }

    /**
     * The card a team is playing on, falling back to the active card in
     * single-card modes where teams have no card of their own.
     */
    public static ICard getCardForTeam(String teamId) {
        ICardService cards = cards();
        if (cards == null) {
            return null;
        }
        ICard teamCard = cards.getTeamCard(teamId);
        return teamCard != null ? teamCard : cards.getActiveCard();
    }

    /** All cards in play; empty while bingo is not loaded. */
    public static List<ICard> getAllCards() {
        IBingoCardExt ext = cardExt();
        return ext == null ? Collections.emptyList() : ext.getAllCards();
    }

    /** The 25 tiles of a card in row-major order; empty if unavailable. */
    public static List<ICardEntryView> getEntries(ICard card) {
        IBingoCardExt ext = cardExt();
        if (ext == null || card == null) {
            return Collections.emptyList();
        }
        return ext.getEntries(card);
    }

    /** Tiles of a card that the given team has not completed yet. */
    public static List<ICardEntryView> getIncompleteEntries(ICard card, String teamId) {
        List<ICardEntryView> result = new java.util.ArrayList<>();
        for (ICardEntryView entry : getEntries(card)) {
            if (!entry.hasTeamAchieved(teamId)) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Tiles of a card that the given team has already completed. */
    public static List<ICardEntryView> getCompletedEntries(ICard card, String teamId) {
        List<ICardEntryView> result = new java.util.ArrayList<>();
        for (ICardEntryView entry : getEntries(card)) {
            if (entry.hasTeamAchieved(teamId)) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Display name of a tile, falling back to its objective ID. */
    public static String nameOf(ICardEntryView entry) {
        if (entry == null) {
            return "?";
        }
        String name = entry.getDisplayName();
        return name != null ? name : entry.getObjectiveId();
    }

    // ------------------------------------------------------------ mutation

    /**
     * The fork's extension surface, or null if bingo is not loaded.
     *
     * Gated on the main API as well: the extension is published and cleared by
     * the same scope, and this makes a stale instance impossible to reach if the
     * two ever get out of step.
     */
    public static IBingoExt ext() {
        if (BingoApi.getINSTANCE() == null) {
            return null;
        }
        return BingoExtApi.getINSTANCE();
    }

    /** Scoring mutation, or null if bingo is not loaded. */
    public static IBingoScoringService scoring() {
        IBingoExt ext = ext();
        return ext == null ? null : ext.getScoring();
    }

    /** Card introspection, or null if bingo is not loaded. */
    public static IBingoCardExt cardExt() {
        IBingoExt ext = ext();
        return ext == null ? null : ext.getCards();
    }

    /** Mark an objective complete for a team. Returns false if unavailable. */
    public static boolean completeObjective(String objectiveId, String teamId, UUID playerId) {
        IBingoScoringService scoring = scoring();
        return scoring != null && scoring.completeObjective(objectiveId, teamId, playerId);
    }

    /** Revert a completed objective for a team. Returns false if unavailable. */
    public static boolean uncompleteObjective(String objectiveId, String teamId) {
        IBingoScoringService scoring = scoring();
        return scoring != null && scoring.uncompleteObjective(objectiveId, teamId);
    }

    /** Replace a tile with a randomly generated one; returns the new objective ID. */
    public static String rerollEntry(ICard card, int x, int y) {
        IBingoCardExt ext = cardExt();
        if (ext == null || card == null) {
            return null;
        }
        return ext.rerollEntry(card, x, y);
    }

    /** Reshuffle an entire card. Returns false if unavailable. */
    public static boolean shuffleCard(ICard card) {
        ICardService cards = cards();
        if (cards == null || card == null) {
            return false;
        }
        cards.shuffleCard(card);
        return true;
    }
}
