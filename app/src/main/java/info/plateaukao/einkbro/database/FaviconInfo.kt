package info.plateaukao.einkbro.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "favicons")
data class FaviconInfo(
   @PrimaryKey
   val domain: String,
   @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
   var icon: ByteArray?
) {
   fun getBitmap(): Bitmap? {
      val icon = icon ?: return null
      return BitmapFactory.decodeByteArray(icon, 0, icon.size)
   }
}