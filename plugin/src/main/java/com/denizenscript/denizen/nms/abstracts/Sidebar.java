package com.denizenscript.denizen.nms.abstracts;

import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Sidebar {

    public static class SidebarLine {

        public SidebarLine(String _text, int _score) {
            text = CoreUtilities.clearNBSPs(_text);
            score = _score;
        }

        public String text;

        public int score;
    }

    /**
     * Everything one sidebar currently shows, published as a whole.
     * Nothing in here is edited once it has been handed to {@link #display}: a change builds a new one and swaps it in with that single write,
     * so a reader on another thread - <player.sidebar_lines>, <player.sidebar_title>, <player.sidebar_scores> - always sees a title,
     * lines and scores that belong to each other, rather than a half-written set.
     */
    public static class Display {

        public Display(String title, String[] lines, int[] scores, int count) {
            this.title = title;
            this.lines = lines;
            this.scores = scores;
            this.count = count;
        }

        public final String title;

        /** MAX_LENGTH long, null past 'count'. */
        public final String[] lines;

        /** MAX_LENGTH long, zero past 'count'. */
        public final int[] scores;

        /** How many of the arrays above are actually set. */
        public final int count;
    }

    public static final int MAX_LENGTH = 15;
    public static final String[] firstIds = new String[MAX_LENGTH];
    public static final String[] secondIds = new String[MAX_LENGTH];

    static {
        for (int i = 0; i < MAX_LENGTH; i++) {
            firstIds[i] = Utilities.generateRandomColors(8);
            secondIds[i] = Utilities.generateRandomColors(8);
        }
    }

    /**
     * Held for a whole 'read the lines - edit them - set them - send the update' sequence by whoever runs one.
     * {@link #sendUpdate} is not reentrant for a single player: it alternates between the two shared id arrays and swaps its own objectives,
     * so two overlapping updates would take the same ids and send each other's team-remove packets, blanking the client's sidebar until the next update.
     * Readers never take this - they read {@link #display} instead - so the only thing it makes wait is a second writer for the same player.
     * One thing has to stay true while it is held: the packets sent under it must not hop to the main thread and wait there.
     * None of the scoreboard packet types has a Denizen packet handler at all today, which is why they are listed as async-interceptable;
     * giving one a handler that waits for the main thread the way the actionbar handler does would deadlock against a main-thread sidebar line.
     */
    public final Object updateLock = new Object();

    protected final Player player;

    /** Volatile because the '<player.sidebar_...>' tags read it off the main thread. Only ever replaced whole - see {@link Display}. */
    protected volatile Display display = new Display("", new String[MAX_LENGTH], new int[MAX_LENGTH], 0);

    protected String[] currentIds = null;

    public Sidebar(Player player) {
        this.player = player;
    }

    public String[] getIds() {
        currentIds = currentIds == firstIds ? secondIds : firstIds;
        return currentIds;
    }

    /** The whole current state at once - what an implementation's sendUpdate should read, so its lines and scores can't come from different sets. */
    public Display getDisplay() {
        return display;
    }

    public String getTitle() {
        return display.title;
    }

    public List<SidebarLine> getLines() {
        Display current = display;
        List<SidebarLine> toReturn = new ArrayList<>(MAX_LENGTH);
        for (int i = 0; i < current.count; i++) {
            toReturn.add(new SidebarLine(current.lines[i], current.scores[i]));
        }
        return toReturn;
    }

    public List<String> getLinesText() {
        return new ArrayList<>(Arrays.asList(display.lines));
    }

    /** A copy, because the stored array is part of a published Display and must not be written to. */
    public int[] getScores() {
        return display.scores.clone();
    }

    public final void setTitle(String title) {
        Display current = display;
        if (current.title.equals(title)) {
            return;
        }
        display = new Display(title, current.lines, current.scores, current.count);
        setDisplayName(title);
    }

    protected abstract void setDisplayName(String title);

    public void setLines(List<SidebarLine> lines) {
        String[] newLines = new String[MAX_LENGTH];
        int[] newScores = new int[MAX_LENGTH];
        int count = Math.min(lines.size(), MAX_LENGTH);
        for (int i = 0; i < count; i++) {
            newLines[i] = lines.get(i).text;
            newScores[i] = lines.get(i).score;
        }
        display = new Display(display.title, newLines, newScores, count);
    }

    public abstract void sendUpdate();

    public abstract void remove();
}
