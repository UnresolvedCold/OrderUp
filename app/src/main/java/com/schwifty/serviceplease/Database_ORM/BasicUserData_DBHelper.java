package com.schwifty.serviceplease.Database_ORM;

import android.content.Context;
import android.util.Log;

import org.greenrobot.greendao.database.Database;

public class BasicUserData_DBHelper extends DaoMaster.OpenHelper {

    public BasicUserData_DBHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);
        Log.d("DEBUG", "DB_OLD_VERSION : " + oldVersion + ", DB_NEW_VERSION : " + newVersion);
        switch (oldVersion) {
            case 1:
            case 2:
                //db.execSQL("ALTER TABLE " + UserDao.TABLENAME + " ADD COLUMN " + UserDao.Properties.Name.columnName + " TEXT DEFAULT 'DEFAULT_VAL'");
        }
    }

    {
    }
}
