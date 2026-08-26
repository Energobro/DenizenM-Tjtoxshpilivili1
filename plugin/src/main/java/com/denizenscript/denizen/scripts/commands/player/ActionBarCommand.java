package com.denizenscript.denizen.scripts.commands.player;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.containers.core.FormatScriptContainer;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.Collections;
import java.util.List;

public class ActionBarCommand extends AbstractCommand {

    public ActionBarCommand() {
        setName("actionbar");
        setSyntax("actionbar [<text>] (targets:<player>|...) (format:<script>) (per_player)");
        setRequiredArguments(1, 4);
        setParseArgs(false);
        isProcedural = false;
        setAsyncDeferrable(true);
    }

    @Override
    public boolean isAsyncDeferrable(ScriptEntry scriptEntry) {
        // 'per_player' parses the text once per target, inside execute - handing that over would move the parsing away from the moment the script asked for it.
        return asyncDeferrable && !scriptEntry.hasRawArgument("per_player");
    }

    @Override
    public boolean isAsyncSafe(ScriptEntry scriptEntry) {
        // The text and format are settled in parseArgs, the targets are already PlayerTags, and what is left is looking each one up and sending them an
        // action bar packet - which Paper supports off-thread, queueing it when the sender is not the main thread.
        // 'per_player' is excluded for cost rather than safety: it reparses the text per target, and those tags are typically main-thread-only,
        // so running it here would buy one hand-off per target where going over once buys exactly one.
        return !scriptEntry.hasRawArgument("per_player");
    }

    // <--[command]
    // @Name ActionBar
    // @Syntax actionbar [<text>] (targets:<player>|...) (format:<script>) (per_player)
    // @Required 1
    // @Maximum 4
    // @Short Sends a message to a player's action bar.
    // @group player
    //
    // @Description
    // Sends a message to the target's action bar area.
    // If no target is specified it will default to the attached player.
    // Accepts the 'format:<name>' argument, which will reformat the text according to the specified format script. See <@link language Format Script Containers>.
    //
    // Optionally use 'per_player' with a list of player targets, to have the tags in the text input be reparsed for each and every player.
    // So, for example, "- actionbar 'hello <player.name>' targets:<server.online_players>"
    // would normally show "hello bob" to every player (every player sees the exact same name in the text, ie bob sees "hello bob", steve also sees "hello bob", etc)
    // but if you use "per_player", each player online would see their own name (so bob sees "hello bob", steve sees "hello steve", etc).
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to send a message to the player's action bar.
    // - actionbar "Hey there <player.name>!"
    //
    // @Usage
    // Use to send a message to a list of players.
    // - actionbar "Hey, welcome to the server!" targets:<[thatplayer]>|<[player]>|<[someplayer]>
    //
    // @Usage
    // Use to send a message to a list of players, with a formatted message.
    // - actionbar "Hey there!" targets:<[thatplayer]>|<[player]> format:ServerChat
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("format")
                    && arg.matchesPrefix("format", "f")) {
                String formatStr = TagManager.tag(arg.getValue(), scriptEntry.getContext());
                FormatScriptContainer format = ScriptRegistry.getScriptContainer(formatStr);
                if (format == null) {
                    Debug.echoError("Could not find format script matching '" + formatStr + "'");
                }
                scriptEntry.addObject("format", new ScriptTag(format));
            }
            else if (!scriptEntry.hasObject("targets")
                    && arg.matchesPrefix("targets", "target")) {
                scriptEntry.addObject("targets", ListTag.getListFor(TagManager.tagObject(arg.getValue(), scriptEntry.getContext()), scriptEntry.getContext()).filter(PlayerTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("per_player")
                    && arg.matches("per_player")) {
                scriptEntry.addObject("per_player", new ElementTag(true));
            }
            else if (!scriptEntry.hasObject("text")) {
                scriptEntry.addObject("text", arg.getRawElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("text")) {
            throw new InvalidArgumentsException("Must specify a message!");
        }
        if (!scriptEntry.hasObject("targets") && !Utilities.entryHasPlayer(scriptEntry)) {
            throw new InvalidArgumentsException("Must specify target(s).");
        }
        if (!scriptEntry.hasObject("targets")) {
            if (!Utilities.entryHasPlayer(scriptEntry)) {
                throw new InvalidArgumentsException("Must specify valid player Targets!");
            }
            else {
                scriptEntry.addObject("targets", Collections.singletonList(Utilities.getEntryPlayer(scriptEntry)));
            }
        }
        // The text and the format are resolved here rather than inside execute, so that an async script can hand this actionbar to the main thread
        // and carry on: the message is then the one the script had at this moment, rather than whatever the tags read whenever the main thread got to it.
        // 'per_player' is the exception - it deliberately reparses the text for each target, so it keeps its parsing in execute.
        if (!scriptEntry.hasObject("per_player")) {
            String text = TagManager.tag(scriptEntry.getElement("text").asString(), scriptEntry.getContext());
            ElementTag parsedText = new ElementTag(text, true);
            scriptEntry.addObject("parsed_text", parsedText);
            ScriptTag formatObj = scriptEntry.getObjectTag("format");
            FormatScriptContainer format = formatObj == null ? null : (FormatScriptContainer) formatObj.getContainer();
            // Formatting once here also saves repeating it per target below - the format only varies per target when 'per_player' is used.
            scriptEntry.addObject("formatted_text", format != null ? new ElementTag(format.getFormattedText(text, scriptEntry), true) : parsedText);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        List<PlayerTag> targets = (List<PlayerTag>) scriptEntry.getObject("targets");
        String text = scriptEntry.getElement("text").asString();
        // Set unless this is a 'per_player' actionbar - see parseArgs.
        ElementTag parsedText = scriptEntry.getElement("parsed_text");
        ElementTag formattedText = scriptEntry.getElement("formatted_text");
        ScriptTag formatObj = scriptEntry.getObjectTag("format");
        ElementTag perPlayerObj = scriptEntry.getElement("per_player");
        BukkitTagContext context = (BukkitTagContext) scriptEntry.getContext();
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), db("message", parsedText != null ? parsedText.asString() : text), db("targets", targets), formatObj, perPlayerObj);
        }
        FormatScriptContainer format = formattedText != null || formatObj == null ? null : (FormatScriptContainer) formatObj.getContainer();
        for (PlayerTag player : targets) {
            if (player != null) {
                if (!player.isOnline()) {
                    Debug.echoDebug(scriptEntry, "Player is offline, can't send actionbar to them. Skipping.");
                    continue;
                }
                String personalText;
                if (formattedText != null) {
                    personalText = formattedText.asString();
                }
                else {
                    context.player = player;
                    personalText = TagManager.tag(text, context);
                    personalText = format != null ? format.getFormattedText(personalText, scriptEntry) : personalText;
                }
                PaperAPITools.instance.sendActionBar(player.getPlayerEntity(), personalText);
            }
            else {
                Debug.echoError("Sent actionbar to non-existent player!?");
            }
        }
    }
}