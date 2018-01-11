package com.raisnet.kotlindemo.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Description :
 * Copyright   : Copyright (c) 2017
 * Company     : Raisecom
 * Author      : yxl
 * Date        : 2017-12-26 14:10
 */
@Entity
public class ClipItem {
    @Id(autoincrement = true)
    private Long id;
    private String content;
    private Long createTime;
    @Generated(hash = 8303691)
    public ClipItem(Long id, String content, Long createTime) {
        this.id = id;
        this.content = content;
        this.createTime = createTime;
    }
    @Generated(hash = 794116089)
    public ClipItem() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getContent() {
        return this.content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public Long getCreateTime() {
        return this.createTime;
    }
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
