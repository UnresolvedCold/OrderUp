package com.schwifty.serviceplease;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.schwifty.serviceplease.Database_ORM.CurrentScannedEntity;
import com.schwifty.serviceplease.Database_ORM.CurrentScannedEntityDao;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;
import com.schwifty.serviceplease.Mall.Mall_ShopListActivity;

import java.util.List;

public class Launcher extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        CurrentScannedEntityDao dao = ((SelectedItemsApp)getApplication()).getCurrentsSession().getCurrentScannedEntityDao();

        if(dao.loadAll().size()>0) {
            List<CurrentScannedEntity> e = dao.queryBuilder()
                    .where(CurrentScannedEntityDao.Properties.Id.eq(100L))
                    .list();
            if (e.iterator().hasNext()) {

                final CurrentScannedEntity _e = e.iterator().next();
                final String type = _e.getType().toString();
                final String res = _e.getResId();
                final String Table = _e.getTable();
                Log.d("hundred_launcher",_e.getTable() + " "+ _e.getResId()+" "+_e.getType() );

                if (type.equals("Rest")) {

                    FirebaseInstanceId.getInstance().getInstanceId()
                            .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                @Override
                                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                    if (task.isSuccessful()) {
                                        final String DeviceId = task.getResult().getToken();

                                        DatabaseReference r = FirebaseDatabase.getInstance().getReference().child(Constants.OccupiedMembers).child(res).child(Table);


                                        r.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if ((dataSnapshot.exists() && dataSnapshot.getValue().toString().contains(DeviceId))) {

                                                    Log.d("hundred_launcher_status","Already, Rest");
                                                    Constants.LoadRestModule(res);
                                                    final Handler handler = new Handler();
                                                    handler.postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            //FilesUtils.SendBasicInfo(WelcomePage.this,MenuActivity.class,result);
                                                            FilesUtils.SendBasicInfo(Launcher.this,
                                                                    MenuActivity.class,
                                                                    res + "," + Table + ",0", "NA");

                                                        }
                                                    }, 100);

                                                }
                                                else
                                                {
                                                    Log.d("hundred_launcher_status","New, Rest");

                                                    ItemsDao dao = ((SelectedItemsApp) getApplication()).getSelectedItemsSession().getItemsDao();
                                                    dao.deleteAll();

                                                    final Handler handler = new Handler();
                                                    handler.postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            //FilesUtils.SendBasicInfo(WelcomePage.this,MenuActivity.class,result);
                                                            Intent intent = new Intent(Launcher.this,MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);

                                                        }
                                                    }, 100);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });


                                    }
                                }
                            });


                }
                if (type.equals("Mall")) {
                    Log.d("hundred_launcher_status","Already, Mall");
                    Constants.LoadMallModule(_e.getMallId(), res);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            //FilesUtils.SendBasicInfo(WelcomePage.this,MenuActivity.class,result);
                            Intent intent = new Intent(Launcher.this, Mall_ShopListActivity.class);
                            intent.putExtra("MallId", _e.getMallId());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        }
                    }, 100);
                }
            }
        }
        else
        {
            Log.d("hundred_launcher_status","Normal");

            ItemsDao _dao = ((SelectedItemsApp) getApplication()).getSelectedItemsSession().getItemsDao();
            _dao.deleteAll();

            //Open normally
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    //FilesUtils.SendBasicInfo(WelcomePage.this,MenuActivity.class,result);
                    Intent intent = new Intent(Launcher.this,MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }
            }, 100);
        }
    }
}
