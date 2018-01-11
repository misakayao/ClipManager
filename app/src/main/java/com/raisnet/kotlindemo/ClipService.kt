package com.raisnet.kotlindemo

import android.app.*
import android.content.ClipboardManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import com.raisnet.kotlindemo.bean.ClipItem
import com.raisnet.kotlindemo.bean.ClipItemDao
import com.raisnet.kotlindemo.utils.LogUtils

/**
 * Description :
 * Copyright   : Copyright (c) 2017
 * Company     : Raisecom
 * Author      : yxl
 * Date        : 2017-12-26 17:21
 */
class ClipService : Service() {
    private var previousTime: Long = 0
    private lateinit var cm: ClipboardManager
    private lateinit var clipChangedListener: ClipboardManager.OnPrimaryClipChangedListener

    override fun onBind(p0: Intent?): IBinder {
        return ClipBinder(cm, clipChangedListener)
    }

    override fun onCreate() {
        super.onCreate()
        println("ClipService onCreate")
        val builder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel("misaka", "misaka", NotificationManager.IMPORTANCE_NONE)
            } else {
                null
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            NotificationCompat.Builder(this, channel!!.id)
        } else {
            NotificationCompat.Builder(this)
        }
        builder.setContentTitle("剪切板助手")
                .setWhen(0)
                .setContentText("剪切板助手正在监控中...")
                .setSmallIcon(R.drawable.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher))
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, Class.forName("com.raisnet.kotlindemo.MainActivity")), PendingIntent.FLAG_UPDATE_CURRENT))
        val notification: Notification = builder.build()
        startForeground(10000, notification)
        cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val dao = MyApplication.getDaoInstance()
        clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
            if (cm.hasPrimaryClip() && cm.primaryClip.itemCount > 0 && (System.currentTimeMillis() - previousTime > 200)) {
                val addedText = cm.primaryClip.getItemAt(0).text
                if (addedText != null) {
                    builder.setContentTitle("剪切板助手")
                            .setWhen(0)
                            .setContentText(addedText)
                            .setSmallIcon(R.drawable.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher))
                            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, Class.forName("com.raisnet.kotlindemo.MainActivity")), PendingIntent.FLAG_UPDATE_CURRENT))
                    val n = builder.build()
                    startForeground(10000, n)
                    Log.d(TAG, "copied text: " + addedText)
                    previousTime = System.currentTimeMillis()
                    val clipItem = ClipItem(null, addedText.toString(), previousTime)
                    val list = dao.clipItemDao.queryBuilder().where(ClipItemDao.Properties.Content.eq(addedText)).build().list()
                    if (list.size == 0) {
                        Toast.makeText(this, "剪切板助手: 已保存", Toast.LENGTH_SHORT).show()
                        dao.clipItemDao.insert(clipItem)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    inner class ClipBinder(private var cm: ClipboardManager, private var clipChangedListener: ClipboardManager.OnPrimaryClipChangedListener) : Binder() {
        var isBackground = false
            set(value) {
                field = value
                if (value) {
                    println("开启后台监听")
                    LogUtils.logToFile(this@ClipService, "主页面退出,开启后台监控")
                    cm.addPrimaryClipChangedListener(clipChangedListener)
                } else {
                    println("关闭后台监听")
                    LogUtils.logToFile(this@ClipService, "主页面开启,退出后台监控")
                    cm.removePrimaryClipChangedListener(clipChangedListener)
                }
            }
    }
}