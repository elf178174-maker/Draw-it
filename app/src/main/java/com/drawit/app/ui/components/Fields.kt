package com.drawit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.drawit.app.ui.theme.DI

/** Label above a chunky, focus-aware text field. */
@Composable
fun LabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    Column(modifier) {
        if (label.isNotEmpty()) {
            Eyebrow(label)
            Spacer(Modifier.height(8.dp))
        }
        ChunkyTextField(value, onValueChange, placeholder = placeholder, minLines = minLines, imeAction = imeAction, onImeAction = onImeAction)
    }
}

@Composable
fun ChunkyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val borderColor by animateColorAsState(
        if (focused) DI.colors.ice else DI.colors.line,
        tween(180),
        label = "fieldBorder"
    )
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) DI.colors.surface else DI.colors.sunken)
            .border(2.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = DI.type.body, color = DI.colors.textFaint)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            interactionSource = interaction,
            singleLine = minLines == 1,
            textStyle = DI.type.bodyStrong.copy(color = DI.colors.text),
            cursorBrush = SolidColor(DI.colors.ice),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            modifier = Modifier.fillMaxWidth().heightIn(min = if (minLines > 1) (minLines * 22).dp else 0.dp)
        )
    }
}
