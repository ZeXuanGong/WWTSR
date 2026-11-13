package com.wwtsr.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wwtsr.chest.ChestRules;
import com.wwtsr.chest.ChestSlot;
import com.wwtsr.chest.Reward;
import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.ItemType;
import com.wwtsr.collect.Loadout;
import com.wwtsr.collect.Rarity;
import com.wwtsr.collect.Role;
import com.wwtsr.collect.Synthesis;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long COOLDOWN = ChestRules.COOLDOWN_MILLIS;

    @TempDir
    Path dir;

    /** Dice that always give the same number (kept in range). */
    private static RandomGenerator always(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return Math.min(value, bound - 1);
            }
        };
    }

    /** A player who owns the given collectibles, that many of each. */
    private static Player owning(Map<Collectible, Integer> owned) {
        Inventory bag = new Inventory(owned, Set.of(), Set.of(), List.of());
        return new Player(new PlayerState(Map.of(), bag, Loadout.STARTER, List.of()));
    }

    // ---- the catalog ----

    @Test
    void everyKindHasSomethingAtEveryRarity() {
        for (ItemType type : ItemType.values()) {
            for (Rarity rarity : Rarity.values()) {
                assertFalse(Catalog.of(type, rarity).isEmpty(), type + " has nothing " + rarity);
            }
        }
    }

    @Test
    void idsAreUniqueAndFindTheirCollectible() {
        Set<String> ids = new java.util.HashSet<>();
        for (Collectible item : Catalog.all()) {
            assertTrue(ids.add(item.id()), "repeated id " + item.id());
            assertEquals(Optional.of(item), Catalog.byId(item.id()));
        }
        assertEquals(Optional.empty(), Catalog.byId("accessory.NOPE"));
        assertEquals(Optional.empty(), Catalog.byId(null));
    }

    // ---- chests ----

    @Test
    void aNewPlayerHasTheStarterRoleAndNothingElse() {
        Player player = new Player(PlayerState.NEW);
        assertEquals(Map.of(Role.STARTER, 1), player.state().bag().owned());
        assertEquals(Loadout.STARTER, player.state().look());
        assertEquals(PlayerState.PRESETS, player.state().presets().size());
    }

    @Test
    void oneChestShowsWithAThousandTapsAndBothWithTwoThousand() {
        Player player = new Player(PlayerState.NEW);
        assertFalse(player.ready(ChestSlot.FIRST, 999, NOW));
        assertTrue(player.ready(ChestSlot.FIRST, 1_000, NOW), "ready as soon as it can be paid for");
        assertFalse(player.ready(ChestSlot.SECOND, 1_999, NOW));
        assertTrue(player.ready(ChestSlot.SECOND, 2_000, NOW));
        assertFalse(player.anyReady(999, NOW));
        assertTrue(player.open(ChestSlot.FIRST, 10, NOW, always(0)).isEmpty(), "can't be paid for");
        assertEquals(PlayerState.NEW, player.state());
    }

    @Test
    void withTwoThousandTapsBothChestsCanBeOpenedOneAfterTheOther() {
        Player player = new Player(PlayerState.NEW);
        assertTrue(player.open(ChestSlot.FIRST, 2_000, NOW, always(0)).isPresent());

        // 1,000 taps are left: the first chest is waiting, so the second is the one that shows.
        assertFalse(player.shown(ChestSlot.FIRST, 1_000, NOW, true), "only one chest shows below 2,000");
        assertTrue(player.shown(ChestSlot.SECOND, 1_000, NOW, false));
        assertTrue(player.open(ChestSlot.SECOND, 1_000, NOW, always(0)).isPresent());

        // Nothing is left to pay with: neither shows, not even as a countdown.
        assertFalse(player.shown(ChestSlot.FIRST, 0, NOW, true));
        assertFalse(player.shown(ChestSlot.SECOND, 0, NOW, true));
    }

    @Test
    void aWaitingChestShowsOnlyWhenAsked() {
        Player player = new Player(PlayerState.NEW);
        player.open(ChestSlot.FIRST, 5_000, NOW, always(0)).orElseThrow();
        assertFalse(player.shown(ChestSlot.FIRST, 4_000, NOW, false));
        assertTrue(player.shown(ChestSlot.FIRST, 4_000, NOW, true), "as a countdown");
        assertTrue(player.shown(ChestSlot.SECOND, 4_000, NOW, false));

        // Both waiting, and taps for one only: the second shows its countdown, the first doesn't.
        player.open(ChestSlot.SECOND, 4_000, NOW, always(0)).orElseThrow();
        assertFalse(player.shown(ChestSlot.FIRST, 1_500, NOW, true));
        assertTrue(player.shown(ChestSlot.SECOND, 1_500, NOW, true));
        assertFalse(player.shown(ChestSlot.SECOND, 1_500, NOW, false));
    }

    @Test
    void openingGivesACollectibleAndStartsTheWait() {
        Player player = new Player(PlayerState.NEW);
        int[] heard = {0};
        player.addListener(() -> heard[0]++);

        // Dice of 0: the commonest rarity, and the first collectible of it, which is the starter role.
        Reward reward = player.open(ChestSlot.FIRST, 5_000, NOW, always(0)).orElseThrow();
        assertEquals(Role.BEAR, reward.item());
        assertEquals(2, reward.owned());
        assertFalse(reward.isFirst());
        assertEquals(1, heard[0]);
        assertTrue(player.state().bag().fresh().contains(Role.BEAR), "marked NEW");
        assertEquals(List.of(Role.BEAR), player.state().bag().recent());

        assertEquals(COOLDOWN, player.remainingMillis(ChestSlot.FIRST, NOW));
        assertTrue(player.open(ChestSlot.FIRST, 5_000, NOW + COOLDOWN - 1, always(0)).isEmpty(), "still waiting");
        assertTrue(player.ready(ChestSlot.SECOND, 5_000, NOW), "the other chest has its own wait");
        assertTrue(player.open(ChestSlot.FIRST, 5_000, NOW + COOLDOWN, always(0)).isPresent());
    }

    @Test
    void openingAChestCostsAThousandTaps() {
        Player player = new Player(PlayerState.NEW);
        assertEquals(0, player.state().spentTaps());
        player.open(ChestSlot.FIRST, 5_000, NOW, always(0)).orElseThrow();
        assertEquals(ChestRules.OPEN_COST, player.state().spentTaps());
        // With what is left after paying, the second chest can still be opened.
        player.open(ChestSlot.SECOND, 4_000, NOW, always(0)).orElseThrow();
        assertEquals(2 * ChestRules.OPEN_COST, player.state().spentTaps());
        assertEquals(1_000, ChestRules.OPEN_COST);
    }

    @Test
    void chosenMaterialsMustBeOneKindOneRarityAndSpare() {
        Player player = owning(Map.of(Emote.IDLE, 4, Emote.HAPPY, 3, Emote.ANGRY, 9));
        Inventory bag = player.state().bag();
        assertTrue(Synthesis.valid(bag, Map.of(Emote.IDLE, 3, Emote.HAPPY, 2), Synthesis.Mode.SAME_TIER));
        assertFalse(Synthesis.valid(bag, Map.of(Emote.IDLE, 3, Emote.HAPPY, 1), Synthesis.Mode.SAME_TIER), "too few");
        assertFalse(Synthesis.valid(bag, Map.of(Emote.IDLE, 4, Emote.HAPPY, 1), Synthesis.Mode.SAME_TIER),
                "the last copy can't be used");
        assertFalse(Synthesis.valid(bag, Map.of(Emote.IDLE, 3, Emote.ANGRY, 2), Synthesis.Mode.SAME_TIER),
                "two rarities");
        assertTrue(player.synthesize(Map.of(Emote.IDLE, 3, Emote.ANGRY, 2), Synthesis.Mode.SAME_TIER, always(0))
                .isEmpty());
        Reward made = player.synthesize(Map.of(Emote.IDLE, 3, Emote.HAPPY, 2), Synthesis.Mode.SAME_TIER, always(0))
                .orElseThrow();
        assertEquals(Rarity.COMMON, made.item().rarity());
        assertEquals(1, player.state().bag().count(Emote.HAPPY));
    }

    @Test
    void rarityFollowsTheWeights() {
        assertEquals(Rarity.COMMON, ChestRules.roll(always(59)).rarity());
        assertEquals(Rarity.EPIC, ChestRules.roll(always(99)).rarity());
        Map<Rarity, Integer> seen = new EnumMap<>(Rarity.class);
        Random dice = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            seen.merge(ChestRules.roll(dice).rarity(), 1, Integer::sum);
        }
        assertTrue(seen.get(Rarity.COMMON) > seen.get(Rarity.UNCOMMON));
        assertTrue(seen.get(Rarity.UNCOMMON) > seen.get(Rarity.RARE));
        assertTrue(seen.get(Rarity.RARE) > seen.get(Rarity.EPIC));
        assertTrue(seen.get(Rarity.EPIC) > 0);
    }

    @Test
    void aClockSetBackCannotLockAChestForLongerThanOneWait() {
        PlayerState farFuture = new PlayerState(Map.of(ChestSlot.FIRST, NOW + 100 * COOLDOWN),
                Inventory.STARTER, Loadout.STARTER, List.of());
        assertEquals(COOLDOWN, new Player(farFuture).remainingMillis(ChestSlot.FIRST, NOW));
    }

    // ---- the look ----

    @Test
    void usingThingsChangesTheLook() {
        Player player = owning(Map.of(Role.CAT, 1, Accessory.BOW, 1, Accessory.CROWN, 1, Emote.HAPPY, 1));
        assertEquals(Player.UseResult.NOT_OWNED, player.use(Role.FROG));

        assertEquals(Player.UseResult.CHANGED, player.use(Role.CAT));
        assertEquals(Role.CAT, player.state().look().role());
        assertTrue(player.inUse(Role.CAT));
        assertFalse(player.inUse(Role.BEAR));

        player.use(Accessory.BOW);
        assertEquals(Optional.of(Accessory.BOW), player.state().look().accessory());
        player.use(Accessory.CROWN);
        assertEquals(Optional.of(Accessory.CROWN), player.state().look().accessory(), "one accessory at a time");
        player.use(Accessory.CROWN);
        assertEquals(Optional.empty(), player.state().look().accessory(), "using the worn one takes it off");

        player.use(Emote.HAPPY);
        assertEquals(List.of(Emote.HAPPY), player.state().look().emotes());
        player.use(Emote.HAPPY);
        assertEquals(List.of(), player.state().look().emotes(), "using a set emote removes it");
    }

    @Test
    void atMostFiveEmotesCanBeSet() {
        Map<Collectible, Integer> all = new HashMap<>();
        for (Emote emote : Emote.values()) {
            all.put(emote, 1);
        }
        Player player = owning(all);
        for (int i = 0; i < Loadout.EMOTES_MAX; i++) {
            assertEquals(Player.UseResult.CHANGED, player.use(Emote.values()[i]));
        }
        assertEquals(Player.UseResult.EMOTES_FULL, player.use(Emote.values()[Loadout.EMOTES_MAX]));
        assertEquals(Loadout.EMOTES_MAX, player.state().look().emotes().size());
        assertEquals(Player.UseResult.CHANGED, player.use(Emote.values()[0]), "removing one is still allowed");
    }

    @Test
    void usingSomethingClearsItsNewMark() {
        Player player = new Player(PlayerState.NEW);
        player.open(ChestSlot.FIRST, 5_000, NOW, always(0));
        assertTrue(player.state().bag().fresh().contains(Role.BEAR));
        player.use(Role.BEAR);
        assertFalse(player.state().bag().fresh().contains(Role.BEAR));
    }

    // ---- presets and the bag ----

    @Test
    void presetsKeepAndRestoreALook() {
        Player player = owning(Map.of(Role.CAT, 1, Accessory.BOW, 1, Emote.HAPPY, 1));
        player.use(Role.CAT);
        player.use(Accessory.BOW);
        player.use(Emote.HAPPY);
        Loadout dressed = player.state().look();

        assertFalse(player.applyPreset(2), "nothing saved there yet");
        player.savePreset(2);
        player.use(Role.BEAR);
        player.use(Accessory.BOW);
        assertTrue(player.applyPreset(2));
        assertEquals(dressed, player.state().look());

        player.clearPreset(2);
        assertEquals(Optional.empty(), player.state().presets().get(2));
        player.savePreset(-1);
        player.savePreset(99);
        assertFalse(player.applyPreset(99), "slots outside the five are ignored");
    }

    @Test
    void starsToggleOnOwnedThingsOnly() {
        Player player = owning(Map.of(Accessory.BOW, 1));
        player.toggleFavourite(Accessory.BOW);
        assertTrue(player.state().bag().favourites().contains(Accessory.BOW));
        player.toggleFavourite(Accessory.BOW);
        assertFalse(player.state().bag().favourites().contains(Accessory.BOW));
        player.toggleFavourite(Accessory.CROWN);
        assertTrue(player.state().bag().favourites().isEmpty());
    }

    @Test
    void theStateNeverNamesWhatTheBagDoesNotHold() {
        Loadout wishful = new Loadout(Role.FROG, Optional.of(Accessory.CROWN), List.of(Emote.TRIUMPH));
        PlayerState state = new PlayerState(Map.of(ChestSlot.FIRST, -5L), Inventory.STARTER, wishful,
                List.of(Optional.of(wishful)));
        assertEquals(Loadout.STARTER, state.look());
        assertEquals(Optional.of(Loadout.STARTER), state.presets().get(0));
        assertEquals(0, state.readyAt(ChestSlot.FIRST));
    }

    // ---- synthesis ----

    @Test
    void tenSparesMakeOneOfTheNextRarity() {
        // Eleven leaves: one to keep, ten spare.
        Player player = owning(Map.of(Accessory.LEAF, 11));
        assertTrue(Synthesis.possible(player.state().bag(), ItemType.ACCESSORY, Rarity.COMMON, Synthesis.Mode.UPGRADE));
        Reward made = player.synthesize(ItemType.ACCESSORY, Rarity.COMMON, Synthesis.Mode.UPGRADE, always(0))
                .orElseThrow();
        assertEquals(ItemType.ACCESSORY, made.item().type());
        assertEquals(Rarity.UNCOMMON, made.item().rarity());
        assertEquals(1, player.state().bag().count(Accessory.LEAF), "the last copy is kept");
        assertEquals(1, player.state().bag().count(made.item()));
    }

    @Test
    void fiveSparesMakeOneOfTheSameRarity() {
        Player player = owning(Map.of(Emote.IDLE, 4, Emote.HAPPY, 3));
        Reward made = player.synthesize(ItemType.EMOTE, Rarity.COMMON, Synthesis.Mode.SAME_TIER, always(2))
                .orElseThrow();
        assertEquals(Rarity.COMMON, made.item().rarity());
        assertEquals(Emote.SURPRISED, made.item());
        // Five spares used: three from the emote with more spares, two from the other.
        assertEquals(1, player.state().bag().count(Emote.IDLE));
        assertEquals(1, player.state().bag().count(Emote.HAPPY));
    }

    @Test
    void synthesisNeedsEnoughSparesAndARarityAbove() {
        Player few = owning(Map.of(Accessory.LEAF, 10));
        assertTrue(few.synthesize(ItemType.ACCESSORY, Rarity.COMMON, Synthesis.Mode.UPGRADE, always(0)).isEmpty(),
                "ten copies are only nine spares");
        assertEquals(10, few.state().bag().count(Accessory.LEAF), "nothing was used");

        Player top = owning(Map.of(Accessory.CROWN, 50));
        assertTrue(top.synthesize(ItemType.ACCESSORY, Rarity.EPIC, Synthesis.Mode.UPGRADE, always(0)).isEmpty(),
                "nothing above the top rarity");
        assertTrue(top.synthesize(ItemType.ACCESSORY, Rarity.EPIC, Synthesis.Mode.SAME_TIER, always(0)).isPresent());
    }

    // ---- saving ----

    @Test
    void savedStateComesBack() throws IOException {
        Player player = owning(Map.of(Role.CAT, 2, Accessory.BOW, 3, Emote.HAPPY, 1, Emote.ANGRY, 1));
        player.use(Role.CAT);
        player.use(Accessory.BOW);
        player.use(Emote.ANGRY);
        player.use(Emote.HAPPY);
        player.savePreset(0);
        player.use(Role.BEAR);
        player.savePreset(4);
        player.toggleFavourite(Accessory.BOW);
        player.open(ChestSlot.SECOND, 5_000, NOW, always(99));

        FilePlayerStore store = new FilePlayerStore(dir.resolve("nested").resolve("player.properties"));
        store.save(player.state());
        assertEquals(player.state(), store.load());
    }

    @Test
    void damagedOrNewerFilesDoNotStopTheApp() throws IOException {
        Path file = dir.resolve("player.properties");
        FilePlayerStore store = new FilePlayerStore(file);
        assertEquals(PlayerState.NEW, store.load(), "nothing saved");

        Files.writeString(file, """
                version=1
                chest.FIRST=abc
                owned.accessory.BOW=2
                owned.accessory.NOPE=9
                favourites=accessory.BOW,accessory.NOPE,,accessory.CROWN
                look.role=role.FROG
                look.accessory=accessory.BOW
                look.emotes=emote.HAPPY,nonsense
                preset.1.role=accessory.BOW
                """);
        PlayerState loaded = store.load();
        assertEquals(0, loaded.readyAt(ChestSlot.FIRST));
        assertEquals(2, loaded.bag().count(Accessory.BOW));
        assertEquals(Set.of(Accessory.BOW), loaded.bag().favourites(), "only what is owned and known");
        assertEquals(Role.STARTER, loaded.look().role(), "can't be a role that isn't owned");
        assertEquals(Optional.of(Accessory.BOW), loaded.look().accessory());
        assertEquals(List.of(), loaded.look().emotes());
        assertEquals(Optional.empty(), loaded.presets().get(1), "a preset needs a role");

        Files.writeString(file, "version=2\nowned.accessory.BOW=2\n");
        assertEquals(PlayerState.NEW, store.load(), "a newer format isn't misread");
    }
}
