package com.schwifty.serviceplease.Database_ORM;

import android.app.Application;

public class SelectedItemsApp extends Application
{
    private DaoSession Dao_Session_SelectedItems;
    private DaoSession Dao_Session_OrderedItems;
    private DaoSession Dao_Session_History;
    private DaoSession Dao_BasicUserData;
    private DaoSession Dao_Session_Current;



    @Override
    public void onCreate() {
        super.onCreate();

        Dao_Session_SelectedItems =
                new DaoMaster(new Items_DBHelper(this, "selectedItems.db").getWritableDb()).newSession();

        Dao_Session_OrderedItems =
                new DaoMaster(new Items_DBHelper(this, "orderedItems.db").getWritableDb()).newSession();

        Dao_Session_History =
                new DaoMaster(new Items_DBHelper(this, "history.db").getWritableDb()).newSession();

        Dao_BasicUserData =
                new DaoMaster(new BasicUserData_DBHelper(this,"basicuserdata.db").getWritableDb()).newSession();

        Dao_Session_Current=
                new DaoMaster(new BasicUserData_DBHelper(this,"current.db").getWritableDb()).newSession();
    }

    public DaoSession getCurrentsSession()
    {
        return Dao_Session_Current;
    }

    public DaoSession getSelectedItemsSession() {

        return Dao_Session_SelectedItems;
    }

    public DaoSession getOrderedItemsSession() {

        return Dao_Session_OrderedItems;
    }

    public DaoSession getHistorySession()
    {
        return Dao_Session_History;
    }

    public DaoSession getBasicUserDataSession() {

        return Dao_BasicUserData;
    }
}
