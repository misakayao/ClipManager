package com.raisnet.kotlindemo.adapter

/**
 * Description :
 * Copyright   : Copyright (c) 2017
 * Company     : Raisecom
 * Author      : yxl
 * Date        : 2017-11-26 21:56
 */

interface ItemTouchHelperAdapter {
    //数据交换
    fun onItemMove(fromPosition: Int, toPosition: Int)

    //数据删除
    fun onItemDismiss(position: Int)
}
