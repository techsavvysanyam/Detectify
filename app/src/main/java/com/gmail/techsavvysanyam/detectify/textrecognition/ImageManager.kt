package com.gmail.techsavvysanyam.detectify.textrecognition

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object ImageManager {

    private const val PREFS_NAME = "image_prefs"
    private const val IMAGES_KEY = "images_key"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveImage(context: Context, imagePath: String) {
        val prefs = getPreferences(context)
        val imageList = getImageList(prefs)
        if (imageList.size >= 1) {
            val oldestImage = imageList.removeAt(0)
            deleteImageFile(oldestImage)
        }
        imageList.add(imagePath)
        prefs.edit().putStringSet(IMAGES_KEY, imageList.toSet()).apply()
    }

    private fun getImageList(prefs: SharedPreferences): MutableList<String> {
        return prefs.getStringSet(IMAGES_KEY, emptySet())?.toMutableList() ?: mutableListOf()
    }

    private fun deleteImageFile(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) {
            file.delete()
        }
    }
}
