package com.schwifty.serviceplease.Database_ORM;

import android.content.Intent;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Generated;

import java.util.Iterator;
import java.util.List;

@Entity(nameInDb = "Selected_Items")
public class Items
{
    @Id(autoincrement = true)
    private Long Id;

    @Property(nameInDb = "Item_Name")
    private String ItemName;

    @Property(nameInDb = "isPaid")
    private String isPaid;

    @Property(nameInDb = "qty")
    private int qty;

    @Property(nameInDb = "resId")
    private String resId;

    @Property(nameInDb = "hasbeenOrdered")
    private String hasBeenOrdered;

    @Property(nameInDb = "Price")
    private String price;

    @Property(nameInDb = "Table")
    private String Table;

    @Property(nameInDb = "isVeg")
    private String isVeg;

    @Property(nameInDb = "ItemUID")
    private String ItemUID;

    @Property
    private String metaData1;

    @Property
    private String metaData2;

    @Property
    private String metaData3;

    @Property
    private String metaData4;

    @Property
    private String metaData5;

    @Keep
    public Items(Long Id, String ItemName, int qty,String resId,String hasBeenOrdered,String price,String Table,String isVeg,String ItemUID,
                 ItemsDao dao,boolean flag)
    {
        if(flag) {
            long _id=1L;
            List<Items> itemsList = dao.loadAll();
            for(Items item : itemsList)
            {
                _id = item.getId() + 1L;
            }
            this.Id = _id;
        }
        else {
            this.Id = Id;
        }

        this.ItemName = ItemName;
        isPaid = "false";
        this.qty=qty;
        this.resId=resId;
        this.hasBeenOrdered = hasBeenOrdered;
        this.price = price;
        this.Table = Table;
        this.isVeg = isVeg;
        this.ItemUID = ItemUID;
        this.metaData1="1";
        this.metaData2="2";
        this.metaData3="3";
        this.metaData4="4";
        this.metaData5="5";
    }



    @Generated(hash = 1040818858)
    public Items() {
    }



    @Keep
    public Items(Long Id, String ItemName, String isPaid, int qty, String resId, String hasBeenOrdered, String price, String Table,
            String isVeg, String ItemUID, String metaData1, String metaData2, String metaData3, String metaData4, String metaData5,
                 ItemsDao dao,boolean flag)
    {

        if(flag) {
            long _id = dao.loadAll().size() + 1;
            this.Id = _id;
        }
        else
        {
            this.Id= Id;
        }
        this.ItemName = ItemName;
        this.isPaid = isPaid;
        this.qty = qty;
        this.resId = resId;
        this.hasBeenOrdered = hasBeenOrdered;
        this.price = price;
        this.Table = Table;
        this.isVeg = isVeg;
        this.ItemUID = ItemUID;
        this.metaData1 = metaData1;
        this.metaData2 = metaData2;
        this.metaData3 = metaData3;
        this.metaData4 = metaData4;
        this.metaData5 = metaData5;
    }



    @Generated(hash = 1154963054)
    public Items(Long Id, String ItemName, String isPaid, int qty, String resId, String hasBeenOrdered, String price, String Table,
            String isVeg, String ItemUID, String metaData1, String metaData2, String metaData3, String metaData4, String metaData5) {
        this.Id = Id;
        this.ItemName = ItemName;
        this.isPaid = isPaid;
        this.qty = qty;
        this.resId = resId;
        this.hasBeenOrdered = hasBeenOrdered;
        this.price = price;
        this.Table = Table;
        this.isVeg = isVeg;
        this.ItemUID = ItemUID;
        this.metaData1 = metaData1;
        this.metaData2 = metaData2;
        this.metaData3 = metaData3;
        this.metaData4 = metaData4;
        this.metaData5 = metaData5;
    }



    public Long getId() {
        return this.Id;
    }

    public void setId(Long Id) {
        this.Id = Id;
    }

    public String getItemName() {
        return this.ItemName;
    }

    public void setItemName(String ItemName) {
        this.ItemName = ItemName;
    }

    public String getIsPaid() {
        return this.isPaid;
    }

    public void setIsPaid(String isPaid) {
        this.isPaid = isPaid;
    }

    public int getQty() {
        return this.qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }



    public String getResId() {
        return this.resId;
    }



    public void setResId(String resId) {
        this.resId = resId;
    }



    public String getHasBeenOrdered() {
        return this.hasBeenOrdered;
    }



    public void setHasBeenOrdered(String hasBeenOrdered) {
        this.hasBeenOrdered = hasBeenOrdered;
    }



    public String getPrice() {
        return this.price;
    }



    public void setPrice(String price) {
        this.price = price;
    }



    public String getTable() {
        return this.Table;
    }



    public void setTable(String Table) {
        this.Table = Table;
    }



    public String getIsVeg() {
        return this.isVeg;
    }



    public void setIsVeg(String isVeg) {
        this.isVeg = isVeg;
    }



    public String getItemUID() {
        return this.ItemUID;
    }



    public void setItemUID(String ItemUID) {
        this.ItemUID = ItemUID;
    }



    public String getMetaData1() {
        return this.metaData1;
    }



    public void setMetaData1(String metaData1) {
        this.metaData1 = metaData1;
    }



    public String getMetaData2() {
        return this.metaData2;
    }



    public void setMetaData2(String metaData2) {
        this.metaData2 = metaData2;
    }



    public String getMetaData3() {
        return this.metaData3;
    }



    public void setMetaData3(String metaData3) {
        this.metaData3 = metaData3;
    }



    public String getMetaData4() {
        return this.metaData4;
    }



    public void setMetaData4(String metaData4) {
        this.metaData4 = metaData4;
    }



    public String getMetaData5() {
        return this.metaData5;
    }



    public void setMetaData5(String metaData5) {
        this.metaData5 = metaData5;
    }


}
