package com.raisnet.kotlindemo

import android.app.Application
import cn.bmob.v3.Bmob
import com.raisnet.kotlindemo.bean.DaoMaster
import com.raisnet.kotlindemo.bean.DaoSession

/**
 * Description :
 * Copyright   : Copyright (c) 2017
 * Company     : Raisecom
 * Author      : yxl
 * Date        : 2017-12-26 14:15
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //配置数据库
        setupDatabase()
        //初始化后端云
        Bmob.initialize(this, "de9f77d4be67b2be0f2dfca5c44c14f1")
    }

    private fun setupDatabase() {
        //创建数据库
        val helper = DaoMaster.DevOpenHelper(this, "clip.db", null)
        //获取可写数据库
        val db = helper.writableDatabase
        //获取数据库对象
        val daoMaster = DaoMaster(db)
        //获取Dao对象管理者
        newSession = daoMaster.newSession()
    }

    companion object {
        var newSession: DaoSession? = null
        fun getDaoInstance(): DaoSession {
            return this.newSession!!
        }
    }
}