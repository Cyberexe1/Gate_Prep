package com.gateprep.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One icon per GATE subject, chosen to look like the topic. */
fun subjectIcon(subjectId: String): ImageVector = when (subjectId) {
    "maths" -> Icons.Rounded.Calculate           // calculator
    "digital-logic" -> Icons.Rounded.DeveloperBoard  // circuit board
    "coa" -> Icons.Rounded.Memory                // CPU chip
    "pds" -> Icons.Rounded.Code                  // </>
    "algorithms" -> Icons.Rounded.AccountTree    // flow / tree
    "toc" -> Icons.Rounded.Loop                  // automata loop
    "compiler" -> Icons.Rounded.Build            // build tool
    "os" -> Icons.Rounded.Terminal               // shell prompt
    "dbms" -> Icons.Rounded.Storage              // database stack
    "networks" -> Icons.Rounded.Lan              // network nodes
    "aptitude" -> Icons.Rounded.Psychology       // head + gear
    else -> Icons.Rounded.LibraryBooks
}

/** Gradient rounded tile with a white subject icon and a soft highlight. */
@Composable
fun SubjectTile(subjectId: String, c1: Color, c2: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = size * 0.22f, y = -size * 0.22f).size(size * 0.7f).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)))
        Icon(subjectIcon(subjectId), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.54f))
    }
}
