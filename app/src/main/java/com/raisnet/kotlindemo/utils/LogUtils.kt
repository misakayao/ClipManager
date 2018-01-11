package com.raisnet.kotlindemo.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Description :
 * Copyright   : Copyright (c) 2017
 * Company     : Raisecom
 * Author      : yxl
 * Date        : 2018-01-04 15:56
 */

class LogUtils {
    companion object {
        fun logToFile(context: Context, content: String) {
            val filesDir = context.filesDir
            val file = File(filesDir, "log.txt")
            file.appendText("$content --- ${SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINESE).format(Date(System.currentTimeMillis()))}\n")
        }
    }
}
