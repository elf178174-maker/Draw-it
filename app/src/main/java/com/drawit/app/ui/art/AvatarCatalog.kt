package com.drawit.app.ui.art

import androidx.compose.ui.graphics.Color

/*
 * Every customisable piece of the player's avatar. The catalogue is plain
 * data: names, slots, prices and how each piece is unlocked. The drawing for
 * each id lives in the Avatar*.kt files.
 */

enum class AvatarSlot(val label: String, val removable: Boolean) {
    Background("Backdrop", false),
    Back("Back", true),
    Skin("Skin", false),
    Hair("Hair", false),
    HairColor("Hair colour", false),
    Eyes("Eyes", false),
    Mouth("Mouth", false),
    Top("Outfit", false),
    Face("Face", true),
    Hat("Hat", true),
    Held("Hand", true),
    Aura("Aura", true);
}

enum class Rarity(val label: String, val color: Color, val glow: Color) {
    Starter("Starter", Color(0xFFA89A8C), Color(0xFFE9E1D8)),
    Common("Common", Color(0xFF7FB800), Color(0xFFE4F6C6)),
    Rare("Rare", Color(0xFF1FA3FF), Color(0xFFCFEBFF)),
    Epic("Epic", Color(0xFFA35CFF), Color(0xFFEBDDFF)),
    Legendary("Legendary", Color(0xFFFFB300), Color(0xFFFFF0C2)),
    Streak("Streak reward", Color(0xFFFF5001), Color(0xFFFFE0CF));
}

data class AvatarItem(
    val id: String,
    val slot: AvatarSlot,
    val name: String,
    val rarity: Rarity,
    /** Ink cost. Zero for starter pieces and streak rewards. */
    val price: Int = 0,
    /** For streak rewards: the streak that unlocks it. */
    val unlockStreak: Int = 0,
    /** Animated pieces get a little "live" badge in the shop. */
    val animated: Boolean = false,
    val blurb: String = ""
) {
    val isStarter: Boolean get() = rarity == Rarity.Starter
    val isStreakReward: Boolean get() = unlockStreak > 0
}

object AvatarCatalog {

    private fun starter(id: String, slot: AvatarSlot, name: String) =
        AvatarItem(id, slot, name, Rarity.Starter)

    private fun item(
        id: String, slot: AvatarSlot, name: String, rarity: Rarity, price: Int,
        animated: Boolean = false, blurb: String = ""
    ) = AvatarItem(id, slot, name, rarity, price, animated = animated, blurb = blurb)

    private fun reward(
        id: String, slot: AvatarSlot, name: String, streak: Int,
        animated: Boolean = true, blurb: String = ""
    ) = AvatarItem(id, slot, name, Rarity.Streak, unlockStreak = streak, animated = animated, blurb = blurb)

    val items: List<AvatarItem> = listOf(
        // -- skin -----------------------------------------------------------
        starter("skin_1", AvatarSlot.Skin, "Porcelain"),
        starter("skin_2", AvatarSlot.Skin, "Peach"),
        starter("skin_3", AvatarSlot.Skin, "Honey"),
        starter("skin_4", AvatarSlot.Skin, "Caramel"),
        starter("skin_5", AvatarSlot.Skin, "Bronze"),
        starter("skin_6", AvatarSlot.Skin, "Cocoa"),
        starter("skin_7", AvatarSlot.Skin, "Espresso"),
        item("skin_mint", AvatarSlot.Skin, "Alien Mint", Rarity.Rare, 260, blurb = "Visiting from a very green planet."),
        item("skin_ocean", AvatarSlot.Skin, "Ocean Spirit", Rarity.Epic, 520, blurb = "Cool, calm and very blue."),
        item("skin_galaxy", AvatarSlot.Skin, "Stardust", Rarity.Legendary, 1600, animated = true, blurb = "Made of actual space. Stars twinkle across your face."),

        // -- hair -------------------------------------------------------------
        starter("hair_short", AvatarSlot.Hair, "Classic"),
        starter("hair_long", AvatarSlot.Hair, "Long"),
        starter("hair_bob", AvatarSlot.Hair, "Bob"),
        starter("hair_curly", AvatarSlot.Hair, "Curls"),
        starter("hair_buzz", AvatarSlot.Hair, "Buzz"),
        starter("hair_bald", AvatarSlot.Hair, "Shaved"),
        item("hair_spiky", AvatarSlot.Hair, "Spikes", Rarity.Common, 90),
        item("hair_ponytail", AvatarSlot.Hair, "Ponytail", Rarity.Common, 90),
        item("hair_afro", AvatarSlot.Hair, "Afro", Rarity.Common, 110),
        item("hair_buns", AvatarSlot.Hair, "Space Buns", Rarity.Rare, 220),
        item("hair_pigtails", AvatarSlot.Hair, "Pigtails", Rarity.Rare, 220),
        item("hair_mohawk", AvatarSlot.Hair, "Mohawk", Rarity.Epic, 420),

        // -- hair colour --------------------------------------------------------
        starter("hc_black", AvatarSlot.HairColor, "Ink Black"),
        starter("hc_brown", AvatarSlot.HairColor, "Chestnut"),
        starter("hc_auburn", AvatarSlot.HairColor, "Auburn"),
        starter("hc_blonde", AvatarSlot.HairColor, "Golden"),
        starter("hc_ginger", AvatarSlot.HairColor, "Ginger"),
        starter("hc_silver", AvatarSlot.HairColor, "Silver"),
        item("hc_pink", AvatarSlot.HairColor, "Bubblegum", Rarity.Common, 80),
        item("hc_blue", AvatarSlot.HairColor, "Electric Blue", Rarity.Common, 80),
        item("hc_mint", AvatarSlot.HairColor, "Mint", Rarity.Common, 80),
        item("hc_purple", AvatarSlot.HairColor, "Ultraviolet", Rarity.Rare, 180),
        item("hc_sunset", AvatarSlot.HairColor, "Sunset Fade", Rarity.Epic, 450),
        item("hc_rainbow", AvatarSlot.HairColor, "Prism", Rarity.Legendary, 1400, animated = true, blurb = "The colours never stop moving."),
        reward("hc_flame", AvatarSlot.HairColor, "Living Flame", 50, blurb = "Hair that burns as bright as your streak."),

        // -- eyes -----------------------------------------------------------------
        starter("eyes_round", AvatarSlot.Eyes, "Bright"),
        starter("eyes_happy", AvatarSlot.Eyes, "Joyful"),
        starter("eyes_sleepy", AvatarSlot.Eyes, "Chill"),
        starter("eyes_lashes", AvatarSlot.Eyes, "Lashes"),
        item("eyes_wink", AvatarSlot.Eyes, "Wink", Rarity.Common, 70),
        item("eyes_determined", AvatarSlot.Eyes, "Determined", Rarity.Common, 70),
        item("eyes_star", AvatarSlot.Eyes, "Starstruck", Rarity.Rare, 240, animated = true),
        item("eyes_heart", AvatarSlot.Eyes, "Smitten", Rarity.Rare, 240, animated = true),
        item("eyes_anime", AvatarSlot.Eyes, "Sparkle", Rarity.Epic, 480),

        // -- mouth ----------------------------------------------------------------
        starter("mouth_smile", AvatarSlot.Mouth, "Smile"),
        starter("mouth_grin", AvatarSlot.Mouth, "Grin"),
        starter("mouth_small", AvatarSlot.Mouth, "Calm"),
        item("mouth_open", AvatarSlot.Mouth, "Excited", Rarity.Common, 60),
        item("mouth_tongue", AvatarSlot.Mouth, "Cheeky", Rarity.Common, 70),
        item("mouth_cat", AvatarSlot.Mouth, "Kitty", Rarity.Rare, 160),
        item("mouth_fangs", AvatarSlot.Mouth, "Vampire", Rarity.Epic, 380),

        // -- outfit ---------------------------------------------------------------
        starter("top_tee", AvatarSlot.Top, "Cosy Tee"),
        starter("top_tee_navy", AvatarSlot.Top, "Navy Tee"),
        item("top_hoodie", AvatarSlot.Top, "Draw it Hoodie", Rarity.Common, 120),
        item("top_stripes", AvatarSlot.Top, "Breton Stripes", Rarity.Common, 120),
        item("top_smock", AvatarSlot.Top, "Artist Smock", Rarity.Rare, 260, blurb = "Every splash is a story."),
        item("top_overalls", AvatarSlot.Top, "Overalls", Rarity.Rare, 240),
        item("top_varsity", AvatarSlot.Top, "Varsity Jacket", Rarity.Epic, 520),
        item("top_tux", AvatarSlot.Top, "Tuxedo", Rarity.Epic, 600),
        item("top_space", AvatarSlot.Top, "Space Suit", Rarity.Legendary, 1500, animated = true, blurb = "Blinking console lights included."),
        reward("top_flame", AvatarSlot.Top, "Ember Hoodie", 28, blurb = "Earned by drawing 28 days in a row."),
        reward("top_royal", AvatarSlot.Top, "Royal Robe", 365, blurb = "A whole year. Wear it with pride."),

        // -- hat ------------------------------------------------------------------
        item("hat_beret", AvatarSlot.Hat, "Artist Beret", Rarity.Common, 100),
        item("hat_cap", AvatarSlot.Hat, "Ball Cap", Rarity.Common, 100),
        item("hat_beanie", AvatarSlot.Hat, "Pom Beanie", Rarity.Common, 120),
        item("hat_party", AvatarSlot.Hat, "Party Hat", Rarity.Common, 90),
        item("hat_headphones", AvatarSlot.Hat, "Headphones", Rarity.Rare, 260),
        item("hat_cat", AvatarSlot.Hat, "Cat Ears", Rarity.Rare, 240),
        item("hat_flowers", AvatarSlot.Hat, "Flower Crown", Rarity.Rare, 300),
        item("hat_horns", AvatarSlot.Hat, "Little Horns", Rarity.Rare, 260),
        item("hat_propeller", AvatarSlot.Hat, "Propeller Cap", Rarity.Epic, 460, animated = true),
        item("hat_wizard", AvatarSlot.Hat, "Wizard Hat", Rarity.Epic, 620, animated = true),
        item("hat_tophat", AvatarSlot.Hat, "Top Hat", Rarity.Epic, 540),
        item("hat_halo", AvatarSlot.Hat, "Halo", Rarity.Legendary, 1200, animated = true),
        item("hat_crown", AvatarSlot.Hat, "Royal Crown", Rarity.Legendary, 1800, animated = true, blurb = "Gold, gems and a glint that catches the light."),
        reward("hat_flamecrown", AvatarSlot.Hat, "Crown of Fire", 100, blurb = "Only for 100-day drawers."),

        // -- face -----------------------------------------------------------------
        item("face_glasses", AvatarSlot.Face, "Round Specs", Rarity.Common, 90),
        item("face_freckles", AvatarSlot.Face, "Freckles", Rarity.Common, 60),
        item("face_smudge", AvatarSlot.Face, "Paint Smudge", Rarity.Common, 60),
        item("face_shades", AvatarSlot.Face, "Shades", Rarity.Rare, 220),
        item("face_hearts", AvatarSlot.Face, "Heart Shades", Rarity.Rare, 260),
        item("face_monocle", AvatarSlot.Face, "Monocle", Rarity.Epic, 420),
        item("face_mask", AvatarSlot.Face, "Masquerade", Rarity.Epic, 520),
        item("face_visor", AvatarSlot.Face, "Cyber Visor", Rarity.Legendary, 1300, animated = true),

        // -- held -----------------------------------------------------------------
        item("held_brush", AvatarSlot.Held, "Paintbrush", Rarity.Common, 80),
        item("held_pencil", AvatarSlot.Held, "Giant Pencil", Rarity.Common, 90),
        item("held_mug", AvatarSlot.Held, "Cocoa Mug", Rarity.Common, 90, animated = true),
        item("held_palette", AvatarSlot.Held, "Palette", Rarity.Rare, 220),
        item("held_balloon", AvatarSlot.Held, "Balloon", Rarity.Rare, 200, animated = true),
        item("held_wand", AvatarSlot.Held, "Magic Wand", Rarity.Epic, 480, animated = true),
        item("held_sparkler", AvatarSlot.Held, "Sparkler", Rarity.Epic, 440, animated = true),
        item("held_glowbrush", AvatarSlot.Held, "Neon Brush", Rarity.Legendary, 1300, animated = true, blurb = "Paints with pure light."),
        reward("held_trophy", AvatarSlot.Held, "Golden Trophy", 14, animated = false, blurb = "For two straight weeks of drawing."),

        // -- back -----------------------------------------------------------------
        item("back_backpack", AvatarSlot.Back, "Sketch Pack", Rarity.Common, 120),
        item("back_cape", AvatarSlot.Back, "Hero Cape", Rarity.Rare, 300, animated = true),
        item("back_batwings", AvatarSlot.Back, "Bat Wings", Rarity.Epic, 560, animated = true),
        item("back_angel", AvatarSlot.Back, "Angel Wings", Rarity.Epic, 640, animated = true),
        item("back_butterfly", AvatarSlot.Back, "Butterfly Wings", Rarity.Legendary, 1500, animated = true),
        reward("back_phoenix", AvatarSlot.Back, "Phoenix Wings", 200, blurb = "Two hundred days. You rose from the ashes."),

        // -- aura -----------------------------------------------------------------
        item("aura_sparkles", AvatarSlot.Aura, "Sparkles", Rarity.Rare, 280, animated = true),
        item("aura_hearts", AvatarSlot.Aura, "Love Bubbles", Rarity.Rare, 280, animated = true),
        item("aura_notes", AvatarSlot.Aura, "Good Vibes", Rarity.Epic, 460, animated = true),
        item("aura_snow", AvatarSlot.Aura, "Snowfall", Rarity.Epic, 460, animated = true),
        item("aura_orbit", AvatarSlot.Aura, "Rainbow Orbit", Rarity.Legendary, 1500, animated = true),
        reward("aura_fire", AvatarSlot.Aura, "Fire Aura", 7, blurb = "Your first week on fire."),

        // -- backdrop -------------------------------------------------------------
        starter("bg_paper", AvatarSlot.Background, "Sketch Paper"),
        starter("bg_peach", AvatarSlot.Background, "Peach"),
        item("bg_sky", AvatarSlot.Background, "Sunny Sky", Rarity.Common, 80, animated = true),
        item("bg_mint", AvatarSlot.Background, "Mint Dots", Rarity.Common, 80),
        item("bg_sunset", AvatarSlot.Background, "Sunset", Rarity.Rare, 220),
        item("bg_grid", AvatarSlot.Background, "Sketchbook", Rarity.Rare, 200),
        item("bg_splash", AvatarSlot.Background, "Paint Splash", Rarity.Epic, 450),
        item("bg_night", AvatarSlot.Background, "Starry Night", Rarity.Epic, 520, animated = true),
        item("bg_aurora", AvatarSlot.Background, "Aurora", Rarity.Legendary, 1300, animated = true),
        item("bg_rays", AvatarSlot.Background, "Rainbow Burst", Rarity.Legendary, 1100, animated = true),
        reward("bg_embers", AvatarSlot.Background, "Ember Storm", 150, blurb = "A hundred and fifty days of heat.")
    )

    private val byId = items.associateBy { it.id }

    fun get(id: String?): AvatarItem? = id?.let { byId[it] }

    fun forSlot(slot: AvatarSlot): List<AvatarItem> = items.filter { it.slot == slot }

    /** What a brand-new avatar is wearing. */
    val defaultLook: Map<AvatarSlot, String> = mapOf(
        AvatarSlot.Background to "bg_peach",
        AvatarSlot.Skin to "skin_2",
        AvatarSlot.Hair to "hair_short",
        AvatarSlot.HairColor to "hc_brown",
        AvatarSlot.Eyes to "eyes_round",
        AvatarSlot.Mouth to "mouth_smile",
        AvatarSlot.Top to "top_tee"
    )

    /** Pieces everyone owns from the start. */
    val starterIds: Set<String> = items.filter { it.isStarter }.map { it.id }.toSet()

    /** Streak rewards unlocked by reaching [streak]. */
    fun rewardsUpTo(streak: Int): List<AvatarItem> =
        items.filter { it.isStreakReward && it.unlockStreak <= streak }

    fun rewardsAt(streak: Int): List<AvatarItem> =
        items.filter { it.isStreakReward && it.unlockStreak == streak }
}

/** A resolved outfit: what is in every slot, falling back to defaults. */
data class AvatarLook(val equipped: Map<AvatarSlot, String>) {
    operator fun get(slot: AvatarSlot): String? =
        equipped[slot] ?: AvatarCatalog.defaultLook[slot]

    fun with(item: AvatarItem): AvatarLook = AvatarLook(equipped + (item.slot to item.id))

    fun without(slot: AvatarSlot): AvatarLook = AvatarLook(equipped - slot)

    companion object {
        val Default = AvatarLook(AvatarCatalog.defaultLook)
    }
}
