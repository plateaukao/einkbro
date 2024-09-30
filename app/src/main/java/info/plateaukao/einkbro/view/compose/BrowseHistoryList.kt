package info.plateaukao.einkbro.view.compose

import android.graphics.Bitmap
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordType
import info.plateaukao.einkbro.unit.ViewUnit
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseHistoryList(
    modifier: Modifier,
    records: List<Record>,
    shouldReverse: Boolean,
    shouldShowTwoColumns: Boolean,
    bookmarkManager: BookmarkManager? = null,
    onClick: (Record) -> Unit,
    onLongClick: (Record) -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(if (shouldShowTwoColumns) 2 else 1),
        reverseLayout = shouldReverse
    ) {
        itemsIndexed(records) { index, record ->
            RecordItem(
                record = record,
                bitmap = bookmarkManager?.findFaviconBy(record.url)?.getBitmap(),
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(record) },
                    onLongClick = { onLongClick(record) }
                )
            )
        }
    }
}

@Composable
private fun RecordItem(
    modifier: Modifier,
    bitmap: Bitmap? = null,
    record: Record,
) {
    val timeString =
        if (record.type == RecordType.History) SimpleDateFormat(
            "MM/dd",
            Locale.getDefault()
        ).format(record.time)
        else ""

    Row(
        modifier = modifier
            .height(60.dp)
            .padding(5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        if (record.type == RecordType.Bookmark) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(30.dp)
                    .padding(end = 5.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.icon_bookmark),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground
            )
        } else if (bitmap != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(30.dp)
                    .padding(end = 5.dp),
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
            )
        } else {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(30.dp)
                    .padding(end = 5.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_history),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground
            )
        }
        Column(
            Modifier
                .weight(1F)
                .align(Alignment.CenterVertically)
        ) {
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        textSize = ViewUnit.dpToPixel(6)
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.MIDDLE
                    }
                },
                update = { it.text = record.title ?: "Unknown" }
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                AndroidView(
                    modifier = Modifier
                        .weight(1F)
                        .align(Alignment.Top),
                    factory = { context ->
                        TextView(context).apply {
                            textSize = ViewUnit.dpToPixel(5)
                            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.MIDDLE
                        }
                    },
                    update = { it.text = record.url }
                )
                // alight to end of row
                Text(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .align(Alignment.Top),
                    text = timeString,
                    textAlign = TextAlign.End,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun previewItem() {
    MyTheme {
        RecordItem(
            modifier = Modifier,
            record = Record(
                title = "Hello",
                url = "123ddddddddddddddddddddddddd",
                time = System.currentTimeMillis()
            )
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun previewHistoryList() {
    val list = listOf(
        Record(
            title = "Hello aaa aaa aaa aa aa aaa aa a aa a a a aa a a a a a a a a a aa a a ",
            url = "123",
            time = System.currentTimeMillis()
        ),
        Record(
            title = "Hello 2",
            url = "123 dddddddddddddddddddddddddddddddddddddddd",
            time = System.currentTimeMillis()
        ),
        Record(title = "Hello 3", url = "123", time = System.currentTimeMillis()),
    )
    MyTheme {
        BrowseHistoryList(
            modifier = Modifier,
            records = list,
            shouldReverse = true,
            shouldShowTwoColumns = true,
            onClick = {},
            onLongClick = {})
    }
}