package com.schwifty.serviceplease.Mall;

import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.internal.Util;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.schwifty.serviceplease.Constants;
import com.schwifty.serviceplease.FilesUtils;
import com.schwifty.serviceplease.MainActivity;
import com.schwifty.serviceplease.MenuActivity;
import com.schwifty.serviceplease.OrderedItemsActivity;
import com.schwifty.serviceplease.R;
import com.schwifty.serviceplease.UtilFunctions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Mall_ShopListActivity extends AppCompatActivity {

    String MallId;
    LinearLayout shopList;
    LayoutInflater inflater;

    DatabaseReference database;
    DatabaseReference mallRef;

    List<MallShopsDetail> Shops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mall__shop_list);

        Shops = new ArrayList<>();
        inflater=LayoutInflater.from(this);
        MallId = getIntent().getStringExtra("MallId").toString();
        shopList=findViewById(R.id.mallShopList);
        database=FirebaseDatabase.getInstance().getReference().child("Mall").child(MallId).child("Shops");
        mallRef = FirebaseDatabase.getInstance().getReference().child("Mall").child(MallId);

        mallRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("Name").getValue().toString();

                if(dataSnapshot.hasChild("") && dataSnapshot.child("Payment").getValue().toString().equals("central") )
                {
                    Constants.paymentSystem = dataSnapshot.child("Payment").getValue().toString().trim();
                }

                TextView tv = (findViewById(R.id.mallDetails));
                tv.setText("Welcome to "+name);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        FetchShopList();

    }

    private void FetchShopList()
    {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                Shops.clear();

               for(DataSnapshot snapshot:dataSnapshot.getChildren())
               {
                   MallShopsDetail e = new MallShopsDetail(
                           snapshot.getKey().toString(),
                           snapshot.child("ResName").getValue().toString());

                   Shops.add(e);
               }

               DisplayShopList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void DisplayShopList()
    {
        Iterator<MallShopsDetail> iterator = Shops.iterator();

        shopList.removeAllViews();
        while(iterator.hasNext())
        {
            final MallShopsDetail e = iterator.next();
            final String ShopUID =e.getShopUID();

            View view = UtilFunctions.ViewInflater(inflater,shopList,R.layout.mall_template_shopinamall);
            TextView shopName = view.findViewById(R.id.mall_template_shopinamall_shopName);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {



                    final Dialog d = UtilFunctions.ShowLoadingBar(Mall_ShopListActivity.this);

                    String url = Constants.getOrderNo(ShopUID,MallId);

                    Ion.with(getApplicationContext())
                            .load(url)
                            .asString()
                            .setCallback(new FutureCallback<String>() {
                                @Override
                                public void onCompleted(Exception exc, String result)
                                {

                                    Log.d("HTML",result.split(":")[1]);

                                    String orderNo = result.split(":")[1];

                                    Constants.LoadMallModule(MallId,ShopUID);

                                    FilesUtils.SendBasicInfo(Mall_ShopListActivity.this,MenuActivity.class,
                                            ShopUID+","+orderNo+",0",
                                            "Mall="+MallId
                                    );
                                    d.dismiss();
                                }
                            });




                }
            });

            shopName.setText(e.getName());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();



    }

    class MallShopsDetail
    {
        String shopUID;
        String name;

        public String getName() {
            return name;
        }

        public String getShopUID() {
            return shopUID;
        }

        public MallShopsDetail(String shopUID, String name) {
            this.name = name;
            this.shopUID=shopUID;
        }
    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein,R.anim.fadeout);

    }
}
