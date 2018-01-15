package com.karageageta.simpleimagepicker.ui.main

import android.content.Context
import android.provider.MediaStore
import com.karageageta.simpleimagepicker.R
import com.karageageta.simpleimagepicker.extension.createValidFile
import com.karageageta.simpleimagepicker.model.data.Album
import com.karageageta.simpleimagepicker.model.data.Config
import com.karageageta.simpleimagepicker.model.data.Image
import java.io.File

class SimpleImagePickerPresenter(
        override val view: SimpleImagePickerContract.View?,
        private val context: Context?,
        private val config: Config
) : SimpleImagePickerContract.Presenter<SimpleImagePickerContract.View> {
    private val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    private var albums = LinkedHashMap<String, Album>()

    override fun load() {
        if (albums.size > 0) {
            return
        }
        loadAlbums()
        view?.clearAlbums()
        view?.addAlbums(albums.values.toList())
        view?.clearImages()
        view?.addImages(albums.values.toList()[0].images)
    }

    override fun loadAlbums() {
        val cursor = context?.contentResolver?.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED
        )

        context?.getString(R.string.text_album_all_key)?.let { albums.put(it, Album(config.pickerAllItemTitle)) }

        if (cursor!!.moveToLast()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex(projection[0]))
                val name = cursor.getString(cursor.getColumnIndex(projection[1]))
                val path = cursor.getString(cursor.getColumnIndex(projection[2]))
                val bucket = cursor.getString(cursor.getColumnIndex(projection[3]))

                val file = File(path).createValidFile()
                if (file != null && file.exists()) {
                    if (albums[bucket] == null) {
                        albums.put(bucket, Album(bucket))
                    }
                    albums[context?.getString(R.string.text_album_all_key)]?.images?.add(Image(id, name, path))
                    albums[bucket]?.images?.add(Image(id, name, path))
                }
            } while (cursor.moveToPrevious())
        }
        cursor.close()
    }

    override fun albums(): List<Album> {
        return albums.values.toList()
    }

    override fun albumSelected(position: Int) {
        view?.clearImages()
        view?.scrollToTop()
        view?.addImages(albums.values.toList()[position].images)
    }

    override fun saveSelected(items: List<Image>) {
        if (items.isNotEmpty()) {
            view?.finishPickImages(items)
        }
        view?.finish()
    }

    override fun permissionDenied() {
        if (config.finishWhenPermissionDenied) {
            view?.finish()
            return
        }
        view?.showPermissionDenied()
    }
}
