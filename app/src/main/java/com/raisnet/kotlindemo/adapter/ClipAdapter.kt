package com.raisnet.kotlindemo.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.raisnet.kotlindemo.R
import com.raisnet.kotlindemo.bean.ClipItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Description :
 * Copyright   : Copyright (c) 2017
 * Company     : Raisecom
 * Author      : yxl
 * Date        : 2017-12-26 15:20
 */

class ClipAdapter(private val context: Context) : RecyclerView.Adapter<ClipAdapter.ClipHolder>(), ItemTouchHelperAdapter {
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        //交换位置
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        //移除数据
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    fun restoreItem(position: Int, item: ClipItem) {
        //撤销数据
        data.add(position, item)
        notifyItemInserted(position)
    }

    var clipboardManager: ClipboardManager? = null

    var data = mutableListOf<ClipItem>()
        set(value) {
            field = value
            notifyDataSetChanged()
            println("${data.size} ${System.currentTimeMillis()}")
        }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ClipHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_clip_history, parent, false)
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return ClipHolder(view)
    }

    override fun onBindViewHolder(holder: ClipHolder?, position: Int) {
        val item = data[position]
        holder?.tvContent?.text = if (item.content.length < 500) {
            item.content
        } else {
            item.content.slice(0..500) + "..."
        }
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINESE)
        holder?.tvCreateTime?.text = sdf.format(Date(item.createTime))
        holder?.itemView?.setOnClickListener({
            /**
             * 将文字信息放到剪贴板上
             */
            val clipData = ClipData.newPlainText("text", item.content)
            clipboardManager?.primaryClip = clipData
            Toast.makeText(context, "已复制到剪切板", Toast.LENGTH_SHORT).show()
        })
    }

    class ClipHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvContent = itemView.findViewById<TextView>(R.id.textView)!!
        var tvCreateTime = itemView.findViewById<TextView>(R.id.tv_create_time)
    }
}

