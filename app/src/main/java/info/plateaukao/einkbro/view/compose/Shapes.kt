package info.plateaukao.einkbro.view.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dashedBorder(width: Dp, radius: Dp, color: Color) =
    drawBehind {
        drawIntoCanvas {
            val paint = Paint()
                .apply {
                    strokeWidth = width.toPx()
                    this.color = color
                    style = PaintingStyle.Stroke
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
            it.drawRoundRect(
                width.toPx(),
                width.toPx(),
                size.width - width.toPx(),
                size.height - width.toPx(),
                radius.toPx(),
                radius.toPx(),
                paint
            )
        }
    }

fun Modifier.horizontalBorder(
    drawTop: Boolean = false,
    drawBottom: Boolean = false,
    color: Color = Color.Black,
    width: Dp = 1.dp,
) = this.then(
    Modifier.drawBehind {
        val strokeWidth = width.toPx()
        if (drawTop) {
            drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = strokeWidth
            )
        }
        if (drawBottom) {
            drawLine(
                color = color,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = strokeWidth
            )
        }
    }
)