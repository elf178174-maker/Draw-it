package com.drawit.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.PurchaseResult
import com.drawit.app.data.StreakState
import com.drawit.app.model.Economy
import com.drawit.app.ui.art.AvatarCatalog
import com.drawit.app.ui.art.AvatarItem
import com.drawit.app.ui.art.AvatarPose
import com.drawit.app.ui.art.AvatarSlot
import com.drawit.app.ui.art.Rarity
import com.drawit.app.ui.components.AvatarView
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyCard
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Confetti
import com.drawit.app.ui.components.InkAmount
import com.drawit.app.ui.components.InkDrop
import com.drawit.app.ui.components.RollingNumber
import com.drawit.app.ui.components.ScreenHeader
import com.drawit.app.ui.components.Tag
import com.drawit.app.ui.components.burst
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.theme.DI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tabs in the order people tend to shop. */
private val tabs: List<Pair<String, AvatarSlot?>> = listOf(
    "Hats" to AvatarSlot.Hat,
    "Outfits" to AvatarSlot.Top,
    "Face" to AvatarSlot.Face,
    "Hand" to AvatarSlot.Held,
    "Back" to AvatarSlot.Back,
    "Aura" to AvatarSlot.Aura,
    "Backdrop" to AvatarSlot.Background,
    "Hair" to AvatarSlot.Hair,
    "Colour" to AvatarSlot.HairColor,
    "Eyes" to AvatarSlot.Eyes,
    "Mouth" to AvatarSlot.Mouth,
    "Skin" to AvatarSlot.Skin,
    "Power-ups" to null
)

@Composable
fun ShopScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val look by viewModel.look.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<AvatarItem?>(null) }
    val scope = rememberCoroutineScope()
    val hop = remember { Animatable(0f) }
    val joy = remember { Animatable(0f) }
    val sparkle = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }

    val slot = tabs[tab].second
    val items = remember(slot) { slot?.let { AvatarCatalog.forSlot(it) } ?: emptyList() }
    val preview = selected?.let { look.with(it) } ?: look

    // Leaving the shop clears "new" badges, but only on the tabs that were opened.
    val unseen by rememberUpdatedState(profile.unseen)
    val viewed = remember { mutableSetOf<AvatarSlot>() }
    LaunchedEffect(slot) { slot?.let { viewed += it } }
    DisposableEffect(Unit) {
        onDispose { viewModel.markSeen(unseen.filter { id -> AvatarCatalog.get(id)?.slot in viewed }.toSet()) }
    }

    fun celebratePurchase() {
        scope.launch {
            launch { sparkle.snapTo(0f); sparkle.animateTo(1f, tween(700)) }
            launch { confetti.snapTo(0f); confetti.animateTo(1f, tween(1800, easing = LinearEasing)) }
            launch { joy.snapTo(1f); delay(1200); joy.animateTo(0f, tween(250)) }
            hop.animateTo(22f, tween(170))
            hop.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Shop", onBack) {
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(DI.colors.inkSoft).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InkDrop(Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                RollingNumber(wallet.balance, DI.type.number, DI.colors.ink)
            }
        }

        // Try-on preview.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(230.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, DI.colors.line, RoundedCornerShape(24.dp))
        ) {
            AvatarView(preview, Modifier.fillMaxSize()) { AvatarPose(hop = hop.value, joy = joy.value) }
            val gold = DI.colors.gold
            Canvas(Modifier.fillMaxSize()) { burst(sparkle.value, gold, rays = 14) }
            Confetti(progress = { confetti.value }, modifier = Modifier.fillMaxSize(), seed = 3, count = 40, origin = androidx.compose.ui.geometry.Offset(0.5f, 0.5f))
            selected?.let { item ->
                Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Tag(item.rarity.label, item.rarity.color)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.name, style = DI.type.title, color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x99000000)).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // What you can do with the selection.
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            val item = selected
            when {
                slot == null -> Text("Power-ups help keep your streak alive.", style = DI.type.bodyStrong, color = DI.colors.textMuted)
                item == null -> Text("Tap anything to try it on.", style = DI.type.bodyStrong, color = DI.colors.textMuted)
                else -> ActionBar(
                    item = item,
                    owned = viewModel.owns(item),
                    equipped = look[item.slot] == item.id,
                    balance = wallet.balance,
                    onBuy = {
                        if (viewModel.buy(item) is PurchaseResult.Bought) celebratePurchase()
                    },
                    onEquip = {
                        viewModel.equip(item)
                        scope.launch { hop.animateTo(10f, tween(120)); hop.animateTo(0f, spring(dampingRatio = 0.4f)) }
                    },
                    onRemove = { viewModel.unequip(item.slot); selected = null }
                )
            }
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs.indices.toList()) { i ->
                val chunk = if (i == tab) Chunks.soft() else Chunks.plain()
                val newCount = tabs[i].second?.let { s -> AvatarCatalog.forSlot(s).count { it.id in profile.unseen } } ?: 0
                Row(
                    Modifier.chunky(chunk, shape = RoundedCornerShape(14.dp), corner = 14.dp, lip = 3.dp, onClick = {
                        if (tab != i) viewModel.feedback.select()
                        tab = i
                        selected = null
                    }).padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tabs[i].first.uppercase(), style = DI.type.label, color = chunk.content)
                    if (newCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(DI.colors.red))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (slot == null) {
            PowerUps(streak, wallet.balance) { viewModel.buyFreeze() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (slot.removable) {
                    item(span = { GridItemSpan(1) }) {
                        NoneCard(selected = look[slot] == null) {
                            viewModel.unequip(slot)
                            selected = null
                        }
                    }
                }
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        look = look.with(item),
                        owned = viewModel.owns(item),
                        equipped = look[item.slot] == item.id,
                        isNew = item.id in profile.unseen,
                        isSelected = selected?.id == item.id,
                        best = streak.best,
                        onClick = {
                            viewModel.feedback.select()
                            selected = item
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    item: AvatarItem,
    owned: Boolean,
    equipped: Boolean,
    balance: Int,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onRemove: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (item.blurb.isNotBlank()) {
            Text(item.blurb, style = DI.type.small, color = DI.colors.textMuted)
            Spacer(Modifier.height(8.dp))
        }
        when {
            equipped && item.slot.removable -> ChunkyButton("Take it off", onRemove, chunk = Chunks.plain(), modifier = Modifier.fillMaxWidth())
            equipped -> ChunkyButton("Wearing it", {}, chunk = Chunks.plain(), enabled = false, modifier = Modifier.fillMaxWidth())
            owned -> ChunkyButton("Equip", onEquip, chunk = Chunks.green(), modifier = Modifier.fillMaxWidth())
            item.isStreakReward -> ChunkyButton(
                "Reach a ${item.unlockStreak}-day streak", {}, enabled = false,
                icon = painterResource(R.drawable.ic_lock), modifier = Modifier.fillMaxWidth()
            )
            balance >= item.price -> ChunkyButton(
                "Buy", onBuy, chunk = Chunks.ink(), modifier = Modifier.fillMaxWidth(),
                trailing = { InkAmount(item.price, color = Color.White) }
            )
            else -> ChunkyButton(
                "Need ${item.price - balance} more ink", {}, enabled = false, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: AvatarItem,
    look: com.drawit.app.ui.art.AvatarLook,
    owned: Boolean,
    equipped: Boolean,
    isNew: Boolean,
    isSelected: Boolean,
    best: Int,
    onClick: () -> Unit
) {
    val chunk = if (isSelected) Chunks.soft() else Chunks.plain()
    Column(
        Modifier.chunky(chunk, corner = 16.dp, onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            // Only the selected card animates; the rest stay still to keep the grid light.
            AvatarView(look, Modifier.fillMaxSize(), animate = isSelected)
            Box(
                Modifier.align(Alignment.TopStart).padding(4.dp).size(10.dp).clip(CircleShape)
                    .background(item.rarity.color).border(2.dp, Color.White, CircleShape)
            )
            if (isNew) Tag("New", DI.colors.red, Modifier.align(Alignment.TopEnd).padding(4.dp))
            if (item.animated && !isNew) Tag("Live", DI.colors.purple, Modifier.align(Alignment.TopEnd).padding(4.dp))
            if (equipped) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(DI.colors.green),
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.name, style = DI.type.small, color = DI.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        when {
            owned -> Text(if (equipped) "Wearing" else "Owned", style = DI.type.label, color = DI.colors.green)
            item.isStreakReward -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_flame), null, tint = if (best >= item.unlockStreak) DI.colors.flame else DI.colors.textFaint, modifier = Modifier.size(12.dp))
                Text(" ${item.unlockStreak} days", style = DI.type.label, color = DI.colors.textMuted)
            }
            else -> InkAmount(item.price, size = 14.dp)
        }
    }
}

@Composable
private fun NoneCard(selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.chunky(if (selected) Chunks.soft() else Chunks.plain(), corner = 16.dp, onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(DI.colors.sunken), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_close), null, tint = DI.colors.textFaint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("None", style = DI.type.small, color = DI.colors.text)
        Text(" ", style = DI.type.label)
    }
}

@Composable
private fun PowerUps(streak: StreakState, balance: Int, onBuyFreeze: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
        ChunkyCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(DI.colors.iceSoft), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_snowflake), null, tint = DI.colors.ice, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Streak freeze", style = DI.type.title, color = DI.colors.text)
                    Text(
                        "Covers one missed day automatically. You hold ${streak.freezes} of ${StreakState.MAX_FREEZES}.",
                        style = DI.type.small, color = DI.colors.textMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val full = streak.freezes >= StreakState.MAX_FREEZES
            ChunkyButton(
                if (full) "You're full" else "Buy",
                onBuyFreeze,
                chunk = Chunks.ice(),
                enabled = !full && balance >= Economy.FREEZE_PRICE,
                modifier = Modifier.fillMaxWidth(),
                trailing = if (full) null else ({ InkAmount(Economy.FREEZE_PRICE, color = Color.White) })
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Rarer pieces are marked by the dot on each card: green common, blue rare, purple epic, gold legendary, orange for streak rewards you can only earn by drawing.",
            style = DI.type.small,
            color = DI.colors.textMuted
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Rarity.Common, Rarity.Rare, Rarity.Epic, Rarity.Legendary, Rarity.Streak).forEach { Tag(it.label, it.color) }
        }
    }
}
