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
import com.denizenscript.denizencore.scripts.ScriptFormattingContext;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.FormatScriptContainer;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class NarrateCommand extends AbstractCommand {

    public static final String NARRATE_FORMAT_TYPE = ScriptFormattingContext.registerFormatType("narrate");

    public NarrateCommand() {
        setName("narrate");
        setSyntax("narrate [<text>] (targets:<player>|...) (format:<script>) (per_player) (from:<uuid>)");
        setRequiredArguments(1, 5);
        setParseArgs(false);
        isProcedural = true;
        setAsyncDeferrable(true);
    }

    @Override
    public boolean isAsyncDeferrable(ScriptEntry scriptEntry) {
        // 'per_player' parses the text once per target, inside execute - handing that over would move the parsing away from the moment the script asked for it.
        return asyncDeferrable && !scriptEntry.hasRawArgument("per_player");
    }

    @Override
    public boolean isAsyncSafe(ScriptEntry scriptEntry) {
        // The text and format are settled in parseArgs, the targets are already PlayerTags, and what is left is looking each one up and sending them a
        // chat packet - which Paper supports off-thread, queueing it when the sender is not the main thread. The console route reads a stored field.
        // 'per_player' is excluded for cost rather than safety: it reparses the text per target, and those tags are typically main-thread-only,
        // so running it here would buy one hand-off per target where going over once buys exactly one.
        // Worth knowing about the cost on a server that handles 'player receives message': Denizen intercepts the chat packet, and the
        // interception hops to the main thread and waits when it is not already there. So the send is free here, but the event is not -
        // and that wait is invisible to the hand-off counter, since it happens below the command rather than in place of it.
        return !scriptEntry.hasRawArgument("per_player");
    }

    /** The formats this narrate should use, from its own 'format:' argument if it has one, or else from the script it's in. */
    public static ScriptFormattingContext getFormattingContext(ScriptEntry scriptEntry) {
        ScriptTag formatObj = scriptEntry.getObjectTag("format");
        if (formatObj != null) {
            return ((FormatScriptContainer) formatObj.getContainer()).getAsFormattingContext();
        }
        ScriptContainer scriptContainer = scriptEntry.getScriptContainer();
        return scriptContainer != null ? scriptContainer.getFormattingContext() : null;
    }

    // <--[command]
    // @Name Narrate
    // @Syntax narrate [<text>] (targets:<player>|...) (format:<script>) (per_player) (from:<uuid>)
    // @Required 1
    // @Maximum 5
    // @Short Shows some text to the player.
    // @Group player
    //
    // @Description
    // Prints some text into the target's chat area. If no target is specified it will default to the attached player or the console.
    //
    // You can format the text with <@link language Format Script Containers> using the 'format' argument, or with the "narrate" format type (see <@link language Script Formats>).
    //
    // Optionally use 'per_player' with a list of player targets, to have the tags in the text input be reparsed for each and every player.
    // So, for example, "- narrate 'hello <player.name>' targets:<server.online_players>"
    // would normally say "hello bob" to every player (every player sees the exact same name in the text, ie bob sees "hello bob", steve also sees "hello bob", etc)
    // but if you use "per_player", each player online would see their own name (so bob sees "hello bob", steve sees "hello steve", etc).
    //
    // Optionally, specify 'from:<uuid>' to indicate that message came from a specific UUID (used for things like the vanilla client social interaction block option).
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to narrate text to the player.
    // - narrate "Hello World!"
    //
    // @Usage
    // Use to narrate text to a list of players.
    // - narrate "Hello there." targets:<[player]>|<[someplayer]>|<[thatplayer]>
    //
    // @Usage
    // Use to narrate text to a unique message to every player on the server.
    // - narrate "Hello <player.name>, your secret code is <util.random.duuid>." targets:<server.online_players> per_player
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
                    return;
                }
                scriptEntry.addObject("format", new ScriptTag(format));
            }
            else if (!scriptEntry.hasObject("targets")
                    && arg.matchesPrefix("target", "targets", "t")) {
                scriptEntry.addObject("targets", ListTag.getListFor(TagManager.tagObject(arg.getValue(), scriptEntry.getContext()), scriptEntry.getContext()).filter(PlayerTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("from")
                    && arg.matchesPrefix("from")) {
                scriptEntry.addObject("from", TagManager.tagObject(arg.getValue(), scriptEntry.getContext()));
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
        if (!scriptEntry.hasObject("targets")) {
            scriptEntry.addObject("targets", (Utilities.entryHasPlayer(scriptEntry) ? Collections.singletonList(Utilities.getEntryPlayer(scriptEntry)) : null));
        }
        if (!scriptEntry.hasObject("text")) {
            throw new InvalidArgumentsException("Missing any text!");
        }
        // The text and the format are resolved here rather than inside execute, so that an async script can hand this narrate to the main thread
        // and carry on: the message is then the one the script had at this moment, rather than whatever the tags read whenever the main thread got to it.
        // 'per_player' is the exception - it deliberately reparses the text for each target, so it keeps its parsing in execute.
        if (!scriptEntry.hasObject("per_player") || scriptEntry.getObject("targets") == null) {
            String text = TagManager.tag(scriptEntry.getElement("text").asString(), scriptEntry.getContext());
            ElementTag parsedText = new ElementTag(text, true);
            scriptEntry.addObject("parsed_text", parsedText);
            ScriptFormattingContext formattingContext = getFormattingContext(scriptEntry);
            // Formatting once here also saves repeating it per target below - the format only varies per target when 'per_player' is used.
            scriptEntry.addObject("formatted_text", formattingContext != null ? new ElementTag(formattingContext.format(NARRATE_FORMAT_TYPE, text, scriptEntry), true) : parsedText);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        if (scriptEntry.getResidingQueue().procedural) {
            Debug.echoError("'Narrate' should not be used in a procedure script. Consider the 'debug' command instead.");
        }
        List<PlayerTag> targets = (List<PlayerTag>) scriptEntry.getObject("targets");
        String text = scriptEntry.getElement("text").asString();
        // Set unless this is a 'per_player' narrate with targets - see parseArgs.
        ElementTag parsedText = scriptEntry.getElement("parsed_text");
        ElementTag formattedText = scriptEntry.getElement("formatted_text");
        ScriptTag formatObj = scriptEntry.getObjectTag("format");
        ElementTag perPlayerObj = scriptEntry.getElement("per_player");
        ElementTag from = scriptEntry.getElement("from");
        BukkitTagContext context = (BukkitTagContext) scriptEntry.getContext();
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), db("Narrating", parsedText != null ? parsedText.asString() : text), db("Targets", targets), formatObj, perPlayerObj, from);
        }
        // TODO: as of signed chat, this has no effect. Either add proper signed chat support or deprecate.
        UUID fromId = null;
        if (from != null) {
            if (from.asString().startsWith("p@")) {
                fromId = UUID.fromString(from.asString().substring("p@".length()));
            }
            else {
                fromId = UUID.fromString(from.asString());
            }
        }
        if (targets == null) {
            PaperAPITools.instance.sendMessage(Bukkit.getServer().getConsoleSender(), formattedText.asString());
            return;
        }
        ScriptFormattingContext formattingContext = formattedText == null ? getFormattingContext(scriptEntry) : null;
        for (PlayerTag player : targets) {
            if (player != null) {
                // One lookup, not the two this used to do - and off the main thread that lookup is a walk, so it is worth asking only once.
                Player playerEntity = player.getPlayerEntity();
                if (playerEntity == null) {
                    Debug.echoDebug(scriptEntry, "Player is offline, can't narrate to them. Skipping.");
                    continue;
                }
                String personalText;
                if (formattedText != null) {
                    personalText = formattedText.asString();
                }
                else {
                    context.player = player;
                    personalText = TagManager.tag(text, context);
                    personalText = formattingContext != null ? formattingContext.format(NARRATE_FORMAT_TYPE, personalText, scriptEntry) : personalText;
                }
                if (fromId == null) {
                    PaperAPITools.instance.sendMessage(playerEntity, personalText);
                }
                else {
                    PaperAPITools.instance.sendMessage(playerEntity, personalText, fromId);
                }
            }
            else {
                Debug.echoError("Narrated to non-existent player!?");
            }
        }
    }
}