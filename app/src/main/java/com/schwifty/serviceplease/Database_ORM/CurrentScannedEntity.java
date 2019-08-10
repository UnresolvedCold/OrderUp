package com.schwifty.serviceplease.Database_ORM;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Generated;

@Entity(nameInDb = "CurrentScannedEntity")
public class CurrentScannedEntity
{
    @Id(autoincrement = true)
    private Long Id;

    @Property(nameInDb = "type")
    private String type;

    @Property(nameInDb = "ResId")
    private String resId;

    @Property(nameInDb = "MallId")
    private String mallId;

    @Property(nameInDb = "Table")
    private String Table;

    @Keep
    public CurrentScannedEntity(Long Id, String type, String resId) {
        this.Id = Id;
        this.type = type;
        this.resId = resId;
        this.mallId = "na";
        this.Table="";
    }

    @Generated(hash = 1993926481)
    public CurrentScannedEntity(Long Id, String type, String resId, String mallId,
            String Table) {
        this.Id = Id;
        this.type = type;
        this.resId = resId;
        this.mallId = mallId;
        this.Table = Table;
    }

    @Generated(hash = 1883726435)
    public CurrentScannedEntity() {
    }

    public Long getId() {
        return this.Id;
    }

    public void setId(Long Id) {
        this.Id = Id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResId() {
        return this.resId;
    }

    public void setResId(String resId) {
        this.resId = resId;
    }

    public String getMallId() {
        return this.mallId;
    }

    public void setMallId(String mallId) {
        this.mallId = mallId;
    }

    public String getTable() {
        return this.Table;
    }

    public void setTable(String Table) {
        this.Table = Table;
    }

}
