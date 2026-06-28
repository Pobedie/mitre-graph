package com.pobedie.attackgraph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatInputField(
    value: Float?,
    label: String? = null,
    onValueChange: (Float) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp),
    backgroundColor: Color = Color.White,
    shape: RoundedCornerShape = RoundedCornerShape(2.dp)
) {
    var text by remember { mutableStateOf(value?.toString() ?: "") }

    LaunchedEffect(value) {
        val currentFloat = text.toFloatOrNull()
        if (value != null && !value.isNaN()) {
            if (currentFloat != value) {
                text = value.toString()
            }
        } else if (value == null) {
            text = ""
        }
    }

    BasicTextField(
        value = text,
        onValueChange = { newText ->
            val floatValue = newText.toFloatOrNull()
            when {
                floatValue == null -> {
                    text = newText
                }
                floatValue in 0f..1f -> {
                    text = newText
                    onValueChange(floatValue)
                }
                floatValue > 1f -> {
                    text = "1"
                    onValueChange(1f)
                }
                floatValue < 0f -> {
                    text = "0"
                    onValueChange(0f)
                }
            }
        },
        modifier = modifier
            .background(backgroundColor, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        enabled = enabled,
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDismiss() }
        ),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (label != null) {
                    Text(
                        text = "$label:",
                        style = textStyle,
                        color = if (enabled) Color.Black else Color.Gray
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    innerTextField()
                }
            }
        }
    )
}
