package org.valkyrienskies.vscrews.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.StringTextComponent;
import org.valkyrienskies.vscrews.VSCrewsConfig;
import org.valkyrienskies.vscrews.crew.Crew;
import org.valkyrienskies.vscrews.crew.CrewManager;

import java.util.Locale;
import java.util.UUID;

public class VSCrewCommand {

    private static final SuggestionProvider<CommandSource> CREW_SUGGEST = (ctx, builder) -> {
        for (Crew c : CrewManager.listCrews()) {
            builder.suggest(c.getName());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> PLAYER_SUGGEST = (ctx, builder) -> {
        // Online players
        PlayerList list = ctx.getSource().getServer().getPlayerList();
        for (ServerPlayerEntity p : list.getPlayers()) {
            builder.suggest(p.getGameProfile().getName());
        }
        // Cached names from crews
        for (Crew c : CrewManager.listCrews()) {
            // owner
            String ownerName = CrewManager.getCachedName(c.getOwner());
            if (ownerName != null && !ownerName.isEmpty()) builder.suggest(ownerName);
            // members
            for (UUID id : c.getMembers()) {
                String nm = CrewManager.getCachedName(id);
                if (nm != null && !nm.isEmpty()) builder.suggest(nm);
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSource> root = Commands.literal("vscrew");

        if (VSCrewsConfig.ALLOW_NON_OWNER_MANAGE_MEMBERS.get()) {
            root = root.then(
                    Commands.literal("add")
                            .then(Commands.argument("crew", StringArgumentType.word()).suggests(CREW_SUGGEST)
                                    .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYER_SUGGEST)
                                            .executes(ctx -> add(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "crew"),
                                                    StringArgumentType.getString(ctx, "player"))))));}

        // create
        root = root.then(
                Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    // Enforce config: only one crew per player (if enabled)
                                    if (VSCrewsConfig.ONLY_ONE_CREW_PER_PLAYER.get()) {
                                        if (CrewManager.findCrewByOwner(player.getUUID()) != null || CrewManager.findCrewByMember(player.getUUID()) != null) {
                                            player.sendMessage(new StringTextComponent("You are already in a crew."), player.getUUID());
                                            return 0;
                                        }
                                    }
                                    if (CrewManager.isNameTaken(name)) {
                                        player.sendMessage(new StringTextComponent("Crew name is already taken."), player.getUUID());
                                        return 0;
                                    }
                                    if (CrewManager.createCrew(name, player.getUUID())) {
                                        player.sendMessage(new StringTextComponent("Crew created: " + name), player.getUUID());
                                        return 1;
                                    } else {
                                        player.sendMessage(new StringTextComponent("Could not create crew."), player.getUUID());
                                        return 0;}})));

        // delete
        root = root.then(
                Commands.literal("delete")
                        .then(Commands.argument("crew", StringArgumentType.word()).suggests(CREW_SUGGEST)
                                .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, "crew")))
                        )
                        .executes(ctx -> deleteAuto(ctx.getSource())));

        // info
        root = root.then(
                Commands.literal("info")
                        .then(Commands.argument("crew", StringArgumentType.word()).suggests(CREW_SUGGEST)
                                .executes(ctx ->  info(ctx.getSource(), StringArgumentType.getString(ctx, "crew")))
                        )
                        .executes(ctx -> infoAuto(ctx.getSource())));

        // leave
        root = root.then(
                Commands.literal("leave")
                        .then(Commands.argument("crew", StringArgumentType.word()).suggests(CREW_SUGGEST)
                                .executes(ctx -> leave(ctx.getSource(), StringArgumentType.getString(ctx, "crew")))
                        )
                        .executes(ctx -> leaveAuto(ctx.getSource())));

        // remove (conditional)
        if (VSCrewsConfig.ALLOW_NON_OWNER_MANAGE_MEMBERS.get()) {
            root = root.then(
                    Commands.literal("remove")
                            .then(Commands.argument("crew", StringArgumentType.word()).suggests(CREW_SUGGEST)
                                    .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYER_SUGGEST)
                                            .executes(ctx -> remove(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "crew"),
                                                    StringArgumentType.getString(ctx, "player"))))));}

        // rename
        root = root.then(
                Commands.literal("rename")
                        .then(Commands.argument("crew", StringArgumentType.word()).suggests(CREW_SUGGEST)
                                .then(Commands.argument("newName", StringArgumentType.word())
                                        .executes(ctx -> rename(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "crew"),
                                                StringArgumentType.getString(ctx, "newName"))))));

        dispatcher.register(root);
    }

    private static int add(CommandSource src, String crew, String target) {
        Crew c = CrewManager.getCrew(crew);
        if (c == null) {
            src.sendSuccess(new StringTextComponent("Crew not found: " + crew), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        boolean canManage = c.getOwner().equals(caller.getUUID())
                || (VSCrewsConfig.ALLOW_NON_OWNER_MANAGE_MEMBERS.get() && c.isMember(caller.getUUID()));
        if (!canManage) {
            src.sendSuccess(new StringTextComponent("You don't have permission to add members."), false);
            return 0;
        }
        UUID targetId = resolvePlayerId(src, target);
        if (targetId == null) {
            src.sendSuccess(new StringTextComponent("Player not found: " + target), false);
            return 0;
        }
        if (c.isMember(targetId)) {
            src.sendSuccess(new StringTextComponent("Player is already a member."), false);
            return 0;
        }
        CrewManager.addMember(crew, targetId);
        src.sendSuccess(new StringTextComponent("Added player to crew: " + target), true);
        return 1;
    }

    private static int remove(CommandSource src, String crew, String target) {
        Crew c = CrewManager.getCrew(crew);
        if (c == null) {
            src.sendSuccess(new StringTextComponent("Crew not found: " + crew), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        boolean canManage = c.getOwner().equals(caller.getUUID())
                || (VSCrewsConfig.ALLOW_NON_OWNER_MANAGE_MEMBERS.get() && c.isMember(caller.getUUID()));
        if (!canManage) {
            src.sendSuccess(new StringTextComponent("You don't have permission to remove members."), false);
            return 0;
        }
        UUID targetId = resolvePlayerId(src, target);
        if (targetId == null) {
            src.sendSuccess(new StringTextComponent("Player not found: " + target), false);
            return 0;
        }
        if (!c.isMember(targetId)) {
            src.sendSuccess(new StringTextComponent("Player is not a member."), false);
            return 0;
        }
        if (c.getOwner().equals(targetId)) {
            src.sendSuccess(new StringTextComponent("You cannot remove the crew owner."), false);
            return 0;
        }
        CrewManager.removeMember(crew, targetId);
        src.sendSuccess(new StringTextComponent("Removed player from crew: " + target), true);
        return 1;
    }

    private static int info(CommandSource src, String crew) {
        Crew c = CrewManager.getCrew(crew);
        if (c == null) {
            src.sendSuccess(new StringTextComponent("Crew not found: " + crew), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Crew: ").append(c.getName()).append('\n');
        sb.append("Owner: ").append(resolvePlayerName(src, c.getOwner())).append('\n');
        sb.append("Members:\n");
        for (UUID id : c.getMembers()) {
            sb.append(" - ").append(resolvePlayerName(src, id)).append('\n');
        }
        src.sendSuccess(new StringTextComponent(sb.toString()), false);
        return 1;
    }

    private static int infoAuto(CommandSource src) {
        // If only-one-crew rule is enabled, show info for the caller's crew without requiring a name
        if (!VSCrewsConfig.ONLY_ONE_CREW_PER_PLAYER.get()) {
            src.sendSuccess(new StringTextComponent("Usage: /vscrew info <crew>"), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        Crew c = CrewManager.findCrewByOwner(caller.getUUID());
        if (c == null) {
            c = CrewManager.findCrewByMember(caller.getUUID());
        }
        if (c == null) {
            src.sendSuccess(new StringTextComponent("You are not in a crew."), false);
            return 0;
        }
        return info(src, c.getName());
    }

    private static int list(CommandSource src) {
        if (CrewManager.listCrews().isEmpty()) {
            src.sendSuccess(new StringTextComponent("No crews exist."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Crews:\n");
        for (Crew c : CrewManager.listCrews()) {
            sb.append(" - ").append(c.getName()).append(" (members: ").append(c.getMembers().size()).append(")\n");
        }
        src.sendSuccess(new StringTextComponent(sb.toString()), false);
        return 1;
    }

    private static int delete(CommandSource src, String crewName) {
        Crew c = CrewManager.getCrew(crewName);
        if (c == null) {
            src.sendSuccess(new StringTextComponent("Crew not found: " + crewName), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        if (!c.getOwner().equals(caller.getUUID())) {
            src.sendSuccess(new StringTextComponent("Only the crew owner can delete the crew."), false);
            return 0;
        }
        boolean ok = CrewManager.deleteCrew(crewName, caller.getUUID());
        if (ok) {
            src.sendSuccess(new StringTextComponent("Deleted crew: " + crewName), true);
            return 1;
        } else {
            src.sendSuccess(new StringTextComponent("Could not delete crew."), false);
            return 0;
        }
    }

    private static int deleteAuto(CommandSource src) {
        if (!VSCrewsConfig.ONLY_ONE_CREW_PER_PLAYER.get()) {
            src.sendSuccess(new StringTextComponent("Usage: /vscrew delete <crew>"), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        Crew c = CrewManager.findCrewByOwner(caller.getUUID());
        if (c == null) {
            src.sendSuccess(new StringTextComponent("You don't own a crew."), false);
            return 0;
        }
        return delete(src, c.getName());
    }

    private static int leave(CommandSource src, String crewName) {
        Crew c = CrewManager.getCrew(crewName);
        if (c == null) {
            src.sendSuccess(new StringTextComponent("Crew not found: " + crewName), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        if (c.getOwner().equals(caller.getUUID())) {
            src.sendSuccess(new StringTextComponent("Crew owners must use /vscrew delete."), false);
            return 0;
        }
        boolean ok = CrewManager.leaveCrew(crewName, caller.getUUID());
        if (ok) {
            src.sendSuccess(new StringTextComponent("You left the crew: " + crewName), true);
            return 1;
        } else {
            src.sendSuccess(new StringTextComponent("Could not leave crew."), false);
            return 0;
        }
    }

    private static int leaveAuto(CommandSource src) {
        if (!VSCrewsConfig.ONLY_ONE_CREW_PER_PLAYER.get()) {
            src.sendSuccess(new StringTextComponent("Usage: /vscrew leave <crew>"), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        Crew c = CrewManager.findCrewByMember(caller.getUUID());
        if (c == null) {
            src.sendSuccess(new StringTextComponent("You are not in a crew."), false);
            return 0;
        }
        return leave(src, c.getName());
    }

    private static int rename(CommandSource src, String crewName, String newName) {
        Crew c = CrewManager.getCrew(crewName);
        if (c == null) {
            src.sendSuccess(new StringTextComponent("Crew not found: " + crewName), false);
            return 0;
        }
        ServerPlayerEntity caller = getCaller(src);
        if (caller == null) {
            src.sendSuccess(new StringTextComponent("This command must be run by a player."), false);
            return 0;
        }
        if (!c.getOwner().equals(caller.getUUID())) {
            src.sendSuccess(new StringTextComponent("Only the crew owner can rename the crew."), false);
            return 0;
        }
        if (CrewManager.isNameTaken(newName)) {
            src.sendSuccess(new StringTextComponent("Crew name is already taken."), false);
            return 0;
        }
        boolean ok = CrewManager.renameCrew(crewName, newName, caller.getUUID());
        if (ok) {
            src.sendSuccess(new StringTextComponent("Renamed crew: " + crewName + " -> " + newName), true);
            return 1;
        } else {
            src.sendSuccess(new StringTextComponent("Could not rename crew."), false);
            return 0;
        }
    }

    private static UUID resolveOnlinePlayerUUID(CommandSource src, String name) {
        PlayerList list = src.getServer().getPlayerList();
        ServerPlayerEntity target = list.getPlayerByName(name);
        return target != null ? target.getUUID() : null;
    }

    private static ServerPlayerEntity getCaller(CommandSource src) {
        try {
            return src.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static String resolvePlayerName(CommandSource src, UUID id) {
        if (id == null) return "Unknown";
        // First try the cached name
        String cached = CrewManager.getCachedName(id);
        if (cached != null && !cached.isEmpty()) return cached;
        // Fallback to UUID string (should not happen often)
        return id.toString();
    }

    private static UUID resolvePlayerId(CommandSource src, String nameOrId) {
        // Check online players first
        UUID id = resolveOnlinePlayerUUID(src, nameOrId);
        if (id != null) return id;
        // Not online, check UUID format (for offline players or if the server is using UUIDs)
        try {
            return UUID.fromString(nameOrId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
