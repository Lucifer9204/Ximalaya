
package bmw.ximalaya.test.media

import android.content.ContentResolver
import android.net.Uri
import java.io.File

/**
 * Returns a Content Uri for the AlbumArtContentProvider
 */
fun File.asAlbumArtContentUri(): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AUTHORITY)
        .appendPath(this.path)
        .build()
}

private const val AUTHORITY = "bmw.ximalaya.test.provider"
