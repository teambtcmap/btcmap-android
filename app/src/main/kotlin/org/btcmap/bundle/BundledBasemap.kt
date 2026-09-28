package org.btcmap.bundle

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * The bundled low-zoom basemap produced by ``bundle_basemap.py``.
 *
 * The whole world at zoom levels 0 through [MAX_ZOOM] is shipped as a single
 * PMTiles archive in ``assets``. MapLibre can read a PMTiles archive over its
 * ``pmtiles://`` scheme, but on the pinned MapLibre the asset file source cannot
 * perform the byte-range reads the format needs, so the archive is first copied
 * into the app's private storage and MapLibre reads it from there with
 * ``pmtiles://file://``. Online the map draws these zooms from the archive and
 * above [MAX_ZOOM] from the hosted source; offline it uncaps the archive's
 * layers so the z[MAX_ZOOM] tiles overzoom to any zoom. See
 * ``BundledBasemapStyle.kt`` and ``MapSetupController`` for the split.
 *
 * Everything here is best-effort. When the asset is absent (a build made
 * without running the bundler) or extraction fails, [ensure] returns null and
 * the map falls back to the hosted basemap exactly as before.
 */
object BundledBasemap {

    private const val ASSET_NAME = "basemap.pmtiles"
    private const val FILE_NAME = "basemap.pmtiles"

    /** The highest zoom the archive holds; its tiles overzoom above it. */
    internal const val MAX_ZOOM = 4

    internal fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /** The `pmtiles://` URL MapLibre reads the extracted archive from. */
    internal fun pmtilesUrl(file: File): String = "pmtiles://file://${file.absolutePath}"

    /**
     * Returns the extracted archive, copying it out of assets when it is missing
     * or stale. Returns null when the asset is unavailable or the copy fails.
     */
    internal fun ensure(context: Context): File? {
        val target = file(context)
        val expectedSize = assetSize(context) ?: return null

        // A same-size archive is reused as-is: the asset only changes when the
        // bundler is re-run, which also changes its size. A stale extraction is
        // therefore detected on the next launch after an update.
        if (target.exists() && target.length() == expectedSize) return target

        return try {
            copy(context, target)
            if (target.length() != expectedSize) {
                target.delete()
                null
            } else {
                target
            }
        } catch (_: IOException) {
            target.delete()
            null
        }
    }

    private fun assetSize(context: Context): Long? =
        // openFd only works for uncompressed assets; `basemap.pmtiles` is listed
        // in `androidResources.noCompress`, so this reads the stored size.
        try {
            context.assets.openFd(ASSET_NAME).use { it.length }
        } catch (_: IOException) {
            null
        }

    private fun copy(context: Context, target: File) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        try {
            context.assets.open(ASSET_NAME).use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            target.delete()
            if (!tmp.renameTo(target)) throw IOException("could not move ${tmp.name} into place")
        } finally {
            tmp.delete()
        }
    }
}
