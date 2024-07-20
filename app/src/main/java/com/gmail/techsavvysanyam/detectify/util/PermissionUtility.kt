package com.gmail.techsavvysanyam.detectify.util

import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gmail.techsavvysanyam.detectify.appSettingOpen
import com.gmail.techsavvysanyam.detectify.warningPermissionDialog

object PermissionUtility {
    fun checkMultiplePermission(
        activity: Activity,
        permissions: Array<out String>,
        requestCode: Int
    ): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }
        return if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionNeeded.toTypedArray(), requestCode)
            false
        } else {
            true
        }
    }

    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        requiredRequestCode: Int,
        onAllPermissionsGranted: () -> Unit
    ) {
        if (requestCode == requiredRequestCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onAllPermissionsGranted()
            } else {
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] == PackageManager.PERMISSION_DENIED
                }
                val permanentlyDenied = deniedPermissions.any {
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }
                if (permanentlyDenied) {
                    appSettingOpen(activity)
                } else {
                    warningPermissionDialog(activity) { _: DialogInterface, which: Int ->
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            checkMultiplePermission(activity, permissions, requestCode)
                        }
                    }
                }
            }
        }
    }
}
