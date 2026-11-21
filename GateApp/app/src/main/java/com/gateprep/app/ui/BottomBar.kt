package com.gateprep.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gateprep.app.ui.theme.LocalExtra

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Home", Icons.Rounded.Home),
    TabItem("Mock Test", Icons.Rounded.Quiz),
    TabItem("AI Chat", Icons.Rounded.SmartToy),
    TabItem("Profile", Icons.Rounded.Person),
)

/** Floating footer with four icons. The selected tab grows, fills with the brand gradient and shows its label. */
@Composable
fun GateBottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ex = LocalExtra.current
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = ex.card,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, ex.outline),
    ) {
        Row(Modifier.padding(8.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, tab ->
                val sel = i == selected
                val weight by animateFloatAsState(
                    if (sel) 2.2f else 1f,
                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow), label = "tab-weight",
                )
                val fill by animateFloatAsState(if (sel) 1f else 0f, spring(stiffness = Spring.StiffnessMedium), label = "tab-fill")
                val tint by animateColorAsState(if (sel) Color.White else ex.muted, label = "tab-tint")
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .drawBehind { drawRoundRect(brush = ex.brand, cornerRadius = CornerRadius(24.dp.toPx()), alpha = fill) }
                        .clickable(interactionSource = source, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                        AnimatedVisibility(
                            visible = sel,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally(),
                        ) {
                            Text(
                                tab.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
