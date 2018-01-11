package com.raisnet.kotlindemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.raisnet.kotlindemo.adapter.ClipAdapter
import com.raisnet.kotlindemo.adapter.SimpleItemTouchHelperCallback
import com.raisnet.kotlindemo.bean.ClipItem
import com.raisnet.kotlindemo.bean.ClipItemDao
import com.raisnet.kotlindemo.bean.DaoSession
import com.raisnet.kotlindemo.bean.User
import com.raisnet.kotlindemo.utils.LogUtils
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class MainActivity : AppCompatActivity() {
    private val TAG: String = "MainActivity"
    private var previousTime: Long = 0
    private var clipAdapter: ClipAdapter? = null
    private lateinit var cm: ClipboardManager
    private lateinit var clipChangedListener: ClipboardManager.OnPrimaryClipChangedListener
    var clipBinder: ClipService.ClipBinder? = null
    private val conn = ClipServiceConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val dao = MyApplication.getDaoInstance()
        clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
            if (cm.hasPrimaryClip() && cm.primaryClip.itemCount > 0 && (System.currentTimeMillis() - previousTime > 200)) {
                val addedText = cm.primaryClip.getItemAt(0).text
                if (addedText != null) {
                    Log.d(TAG, "copied text: " + addedText)
                    previousTime = System.currentTimeMillis()
                    val clipItem = ClipItem(null, addedText.toString(), previousTime)
                    val list = dao.clipItemDao.queryBuilder().where(ClipItemDao.Properties.Content.eq(addedText)).build().list()
                    if (list.size == 0) {
                        Toast.makeText(this, "剪切板助手: 已保存", Toast.LENGTH_SHORT).show()
                        dao.clipItemDao.insert(clipItem)
                        clipAdapter?.data?.add(clipItem)
                        clipAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }

        initView(dao)
        initData(dao)
    }

    private fun initView(dao: DaoSession) {
        val clipList = findViewById<RecyclerView>(R.id.clipList)
        clipList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        clipList.layoutManager = LinearLayoutManager(this)
        clipAdapter = ClipAdapter(this)
        clipList.adapter = clipAdapter
        val llContent = findViewById<CoordinatorLayout>(R.id.ll_content)

        //先实例化Callback
        val callback = object : SimpleItemTouchHelperCallback(clipAdapter!!) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = clipAdapter?.data?.get(position)
                clipAdapter?.onItemDismiss(viewHolder.adapterPosition)
                dao.clipItemDao.delete(item)
                Snackbar.make(llContent, "删除了1条剪切板记录", Snackbar.LENGTH_LONG).setAction("撤销", {
                    clipAdapter?.restoreItem(position, item!!)
                    dao.clipItemDao.insert(item)
                }).show()
            }
        }
        //用Callback构造ItemtouchHelper
        val touchHelper = ItemTouchHelper(callback)
        //调用ItemTouchHelper的attachToRecyclerView方法建立联系
        touchHelper.attachToRecyclerView(clipList)
    }

    private fun initData(dao: DaoSession) {
        Observable.create<MutableList<ClipItem>> {
            Log.d(TAG, "开始查询")
            val queryBuilder = dao.clipItemDao.queryBuilder()
            Log.d(TAG, "查询完成")
            it.onNext(queryBuilder.build().list())
        }
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    clipAdapter?.data = it
                    for (clipItem in it) {
                        Log.d(TAG, clipItem.content)
                    }
                })

        val intent = Intent(this, Class.forName("com.raisnet.kotlindemo.ClipService"))
        startService(intent)
        bindService(intent, conn, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "页面退出,开启后台监控")
        LogUtils.logToFile(this, "页面退出,开启后台监控")
        cm.removePrimaryClipChangedListener(clipChangedListener)
        clipBinder?.isBackground = true
        unbindService(conn)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private val REQUEST_PHONE_STATE: Int = 10086

    @SuppressLint("HardwareIds")
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var IMEI = ""
        //Android6.0需要动态获取权限
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            //toast("需要动态获取权限");
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE)
        } else {
            //toast("不需要动态获取权限");
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            IMEI = telephonyManager.deviceId
            if (IMEI == null) {
                Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        //val intent = Intent(this, Class.forName("com.raisnet.kotlindemo.ClipService"))
        when (item?.itemId) {
            R.id.backup -> {
                backup(IMEI)
            }
            R.id.restore -> {
                val query: BmobQuery<User> = BmobQuery()
                query.addWhereEqualTo("imei", IMEI).findObjects(object : FindListener<User>() {
                    override fun done(list: MutableList<User>?, e: BmobException?) {
                        Log.d(TAG, "查询完成")
                        if (e != null) {
                            Log.e(TAG, e.message)
                            Toast.makeText(applicationContext, "没有记录", Toast.LENGTH_SHORT).show()
                        } else {
                            val daoInstance = MyApplication.getDaoInstance()
                            list?.forEach {
                                for (clipItem in it.clipList) {
                                    val result = daoInstance.clipItemDao.queryBuilder().where(ClipItemDao.Properties.Content.eq(clipItem.content)).build().list()
                                    if (result.size == 0) {
                                        daoInstance.clipItemDao.insert(clipItem)
                                        clipAdapter?.data?.add(clipItem)
                                        clipAdapter?.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun backup(IMEI: String) {
        val user = User(IMEI, clipAdapter?.data?.toList()!!)
        val query: BmobQuery<User> = BmobQuery()
        query.addWhereEqualTo("imei", IMEI).findObjects(object : FindListener<User>() {
            override fun done(list: MutableList<User>?, e: BmobException?) {
                Log.d(TAG, "查询完成")
                if (e != null) {
                    Log.e(TAG, e.message)
                    user.save(object : SaveListener<String>() {
                        override fun done(objectId: String?, e: BmobException?) {
                            if (e == null) Toast.makeText(applicationContext, "备份成功", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "插入完成")
                            Log.d(TAG, objectId)
                        }
                    })
                } else {
                    if (list?.size!! > 0) {
                        user.update(list[0].objectId, object : UpdateListener() {
                            override fun done(e: BmobException?) {
                                Log.d(TAG, "更新完成")
                                if (e == null) {
                                    Toast.makeText(applicationContext, "备份成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.e(TAG, e.message)
                                }
                            }
                        })
                    } else {
                        user.save(object : SaveListener<String>() {
                            override fun done(objectId: String?, p1: BmobException?) {
                                Log.d(TAG, "插入完成")
                                Log.d(TAG, objectId)
                            }
                        })
                    }
                }
            }
        })
    }

    /**
     * 加个获取权限的监听
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PHONE_STATE && grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "授权成功，继续下一步操作", Toast.LENGTH_SHORT).show()
        }
    }

    inner class ClipServiceConnection : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            LogUtils.logToFile(this@MainActivity, "后台服务意外退出")
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            clipBinder = p1 as ClipService.ClipBinder?
            clipBinder?.isBackground = false
            LogUtils.logToFile(this@MainActivity, "绑定后台服务,添加前台监听")
            cm.addPrimaryClipChangedListener(clipChangedListener)
        }
    }
}
