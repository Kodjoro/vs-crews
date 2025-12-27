# VS-Crews Test Checklist

Use this list to verify behavior across configs, commands, persistence, and helm interactions. Check each item after testing.

## Config flags (run/config/vs-crews-common.toml)
- [x] onlyOneCrewPerPlayer: blocks creating/joining a second crew; enables no-arg forms for info/delete/leave
- [ ] helmWithoutCrewUsableByEveryone: no-crew (or missing-crew) helms usable by all vs. placer-only
- [ ] allowNonCrewBreakHelm: non-members can break helms when true
- [ ] allowNonOwnerManageMembers: members can add/remove when true (owner-only when false)

## Commands (vscrew)
- [ ] create <name>: denies when already in a crew (one-crew rule); denies duplicate name (case-insensitive); success persists
- [ ] list: shows all crews with member counts; updates after create/add/remove/leave/delete
- [ ] info <crew>: shows owner and members with names (online -> cached -> UUID fallback)
- [ ] info (auto): works only with one-crew rule; shows caller’s crew or “You are not in a crew.”
- [ ] add <crew> <player>: permission based on config (owner vs member); resolves online name, UUID, cached offline name; denies already member; persists
- [ ] remove <crew> <player>: permission based on config; denies non-member; denies removing owner; resolves online/UUID/cached; persists
- [ ] delete <crew>: owner-only; success removes crew; members become crewless; persists
- [ ] delete (auto): works only with one-crew rule; deletes caller’s owned crew; denies when caller doesn’t own
- [ ] leave <crew>: non-owner leaves; owner denied (use delete); persists
- [ ] leave (auto): works only with one-crew rule; leaves caller’s current crew; denies when not in a crew

## Tab completion (Brigadier)
- [ ] Crew arg suggests existing crew names; updates immediately after create/delete
- [ ] Player arg suggests online names and cached offline names; updates after login and membership changes
- [ ] No duplicate or stale suggestions (verify after add/remove/leave/delete)

## Helm placement/use/break
- [ ] Placement: owner has crew -> helm stores owner UUID + crew name; owner without crew -> stores owner UUID, no crew name
- [ ] Use with crew: members allowed; non-members denied; denial message does not reveal crew name
- [ ] Use with no/missing crew: respects helmWithoutCrewUsableByEveryone (everyone vs placer-only)
- [ ] Break with crew: respects allowNonCrewBreakHelm (anyone vs members-only)
- [ ] Break with no/missing crew: respects allowNonCrewBreakHelm (anyone vs owner-only when false)
- [ ] After crew deletion: helms referencing deleted crew behave as no-crew per config

## Persistence & cache
- [ ] World load initializes crews from saved data; list/info match previous state
- [ ] Add/remove/leave/delete persist across server restart
- [ ] Player login caches names; info refreshes cache from online names
- [ ] Offline resolution: cached name (case-insensitive exact match) resolves; invalid UUID input handled

## Data integrity & edge cases
- [ ] Owner is included in members on creation; owner cannot be removed
- [ ] Case-insensitive name conflicts for create are denied
- [ ] One-crew rule: cannot own/join multiple crews; delete then create works
- [ ] Helm denial messages do not leak crew names to non-members
- [ ] Commands handle empty lists (no crews), unknown crew names, and unknown players gracefully

## Logging (optional verification)
- [ ] Helm events log allow/deny paths without exposing crew names to players
- [ ] Command outcomes log clearly (success/denial), useful for debugging

---
Tips:
- Restart the game/server after changing COMMON config values to ensure they apply.
- For offline player testing, ensure the player has joined at least once to populate the name cache (or use their UUID directly).

