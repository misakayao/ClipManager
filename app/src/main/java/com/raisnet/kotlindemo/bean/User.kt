package com.raisnet.kotlindemo.bean

import cn.bmob.v3.BmobObject

/**
 * Description :
 * Copyright   : Copyright (c) 2017
 * Company     : Raisecom
 * Author      : yxl
 * Date        : 2018-01-03 17:13
 */
data class User(var imei: String, var clipList: List<ClipItem>) : BmobObject()