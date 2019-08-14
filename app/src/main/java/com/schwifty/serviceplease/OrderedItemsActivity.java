package com.schwifty.serviceplease;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.schwifty.serviceplease.Database_ORM.BasicUserData;
import com.schwifty.serviceplease.Database_ORM.BasicUserDataDao;
import com.schwifty.serviceplease.Database_ORM.Items;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;

import org.greenrobot.greendao.query.QueryBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tourguide.tourguide.TourGuide;

public class OrderedItemsActivity extends AppCompatActivity {

    ItemsDao selectedItemsDao;
    ItemsDao historyDao;

    LinearLayout OrderedItemsList;

    LayoutInflater inflater;

    View AddMoreItems;

    View Pay;

    DatabaseReference OrdersRef;
    DatabaseReference WaiterRef;
    DatabaseReference RestaurantRef;

    DatabaseReference OrderHistory;

    String ResId;
    String Table;

    Double totalCost=0.0;
    Double grandTotal = 0.0;

    String InvoiceNo;

    private Toolbar mToolbar;

    boolean trigger = true;
    Dialog _loading;

    TourGuide t;

    View _root;

    Boolean speedCheck;
    int nQtyForSpeedCheck=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordered_items);

        _root = findViewById(R.id.root_order);
        _root.setVisibility(View.GONE);



        _loading = UtilFunctions.ShowLoadingBar(this);
        Intent intent = getIntent();

        ResId = intent.getStringExtra("ResId");
        Table = intent.getStringExtra("Table");
        selectedItemsDao = ((SelectedItemsApp) getApplication()).getSelectedItemsSession().getItemsDao();
        historyDao = ((SelectedItemsApp) getApplication()).getHistorySession().getItemsDao();
        OrderedItemsList = findViewById(R.id.orderedItemsList);
        inflater = LayoutInflater.from(this);

        OrdersRef = FirebaseDatabase.getInstance().getReference().child(Constants.Order);
        WaiterRef = FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter);
        RestaurantRef = FirebaseDatabase.getInstance().getReference().child(Constants.RestaurantDetails).child(ResId);
        OrderHistory = FirebaseDatabase.getInstance().getReference().child(Constants.OrderHistoryRef).child(ResId);

        ConfigToolbar("Ordered Items", ResId, Table, this);
        AddMoreItems = findViewById(R.id.template_ordered_Add);
        Pay = findViewById(R.id.orderedItems_proceed);


        //If not loaded in 500ms then
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                if(selectedItemsDao.loadAll().size()/2 > nQtyForSpeedCheck)
                {
                    if(!isFinishing()) {
                        final Dialog dialog = new Dialog(OrderedItemsActivity.this);
                        dialog.setContentView(R.layout.dialog_orderconfirmed_);
                        dialog.setCanceledOnTouchOutside(false);

                        dialog.findViewById(R.id.orderconfirm_ok).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        });

                        dialog.show();
                    }
                }
            }
        }, 500);


        t = UtilFunctions.showGuide(106L, Pay, this, "Click 'Pay'",
                "to call the waiter for finishing your payment", "#e54d26", Gravity.TOP,
                ((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(), TourGuide.Technique.Click);

        AddMoreItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilesUtils.SendBasicInfo(OrderedItemsActivity.this, MenuActivity.class, ResId + "," + Table + ",0", Constants.res);

            }
        });

        Pay.setOnClickListener(payment);


        final DatabaseReference _database =  FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table);
                _database.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            LinearLayout textView = findViewById(R.id.orderedItems_proceed);
                            ViewGroup.LayoutParams params = textView.getLayoutParams();

                            if (dataSnapshot.toString().contains("Purpose=Pay")) {
                                if (t != null) t.cleanUp();

                                if (params.height > 0 && Constants.WaiterCalledForPayment) {
                                    UtilFunctions.animateHeight(textView, params.height, 0, 300, View.VISIBLE);
                                    Toast.makeText(OrderedItemsActivity.this, "Waiter has been called to complete the payment", Toast.LENGTH_LONG).show();
                                    Constants.WaiterCalledForPayment=true;
                                }
                                //params.height = 0;
                                //textView.setLayoutParams(params);
                            } else {
                                int h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics());
                                if (params.height <= 0)
                                    UtilFunctions.animateHeight(textView, 0, h, 300, View.VISIBLE);

                                //textView.setLayoutParams(params);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


        NavigationalView((FloatingNavigationView) findViewById(R.id.order_floating_navigation_view));



        //Sync to online then call Populate
        LoadModuleData();

        if(Constants.Type.equals("Rest"))
        {
            _root.setVisibility(View.VISIBLE);
           SetRestView();
        }

        if(Constants.Type.equals("Mall"))
        {
            _root.setVisibility(View.GONE);
            Log.d("_Order_","onCreate, ResDetails : "+Constants.RestaurantDetails);
            SetMallView();
        }

    }

    private void SetRestView() {

        _loading.dismiss();
    }


    private void SetMallView()
    {

        SortLocalDatabase();

        CallWaiterToCompleteThePayment("__",InvoiceNo);

    }

    @Override
    protected void onStart() {
        super.onStart();
        LoadModuleData();
        UtilFunctions.checkValidity(this,ResId,Table,trigger);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadModuleData();
    }


    String deviceId;
    private void CallWaiterToCompleteThePayment(final String option, final String invoice)
    {
        //Show Loading bar
        final Dialog d =UtilFunctions.ShowLoadingBar(OrderedItemsActivity.this);

        //Send payment req
        final String id = WaiterRef.child(ResId).child(Table).push().getKey().toString();

        //Sort Local Database
        SortLocalDatabase();

        //Update Waiter
        WaiterRef.child(ResId).child(Table).child(id).child("Purpose").setValue("Pay");

        List<Items> selItems = historyDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                .list();

        //Calculate Total cost
        totalCost = 0.0;
        grandTotal =0.0;

        for(Items s:selItems)
        {
            double cost =

                    Math.round
                            (
                                    ((s.getQty()*Double.parseDouble(s.getPrice()))*100.0)
                            )/100.0;
            totalCost+=cost;
        }

        totalCost = Math.round(totalCost*100.0)/100.0;

        //Retriving GST
        RestaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                final String GSTValue = dataSnapshot.child("GSTValue").getValue().toString();
                final String ServiceCharge = dataSnapshot.child("ServiceCharge").getValue().toString();
                InvoiceNo = invoice;

                Double GST = Math.round((totalCost*Double.parseDouble(GSTValue))*100.0)/100.0;
                Double ServiceCost = Math.round((totalCost*Double.parseDouble(ServiceCharge))*100.0)/100.0;
                grandTotal = totalCost+GST+ServiceCost;
                WaiterRef.child(ResId).child(Table).child(id).child("Extra").setValue(grandTotal);
                WaiterRef.child(ResId).child(Table).child(id).child("Method").setValue(option);

                //After retriving invoice

                //retrive device id
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task)
                            {
                                if(task.isSuccessful())
                                {
                                    deviceId = task.getResult().getToken().toString();

                                }
                            }
                        });

                //Set pending bills
                if(Constants.Type.equals("Rest")) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(Constants.OccupiedMembers).child(ResId).child(Table);
                            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                        if(snapshot.hasChild("DeviceId"))
                                        if (snapshot.child("DeviceId").getValue().toString().equals(deviceId)) {
                                        } else {
                                            String l = snapshot.child("DeviceId").getValue().toString();

                                            String key = FirebaseDatabase.getInstance().getReference().child(Constants.PendingSync)
                                                    .child(l).push().getKey().toString();

                                            FirebaseDatabase.getInstance().getReference().child(Constants.PendingSync)
                                                    .child(l).child(key).child("Invoice").setValue(invoice);

                                            FirebaseDatabase.getInstance().getReference().child(Constants.PendingSync)
                                                    .child(l).child(key).child("ResId").setValue(ResId);

                                            FirebaseDatabase.getInstance().getReference().child(Constants.PendingSync)
                                                    .child(l).child(key).child("Table").setValue(Table);
                                        }
                                    }

                                    if (d != null) d.dismiss();
                                    UpdateItemStatusToCompleted(invoice);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    if (d != null) d.dismiss();
                                }
                            });


                   // Snackbar.make(findViewById(R.id.root_order), "Waiter has been called", Snackbar.LENGTH_LONG).show();
                   // Toast.makeText(OrderedItemsActivity.this, "Waiter has been called to complete the payment", Toast.LENGTH_LONG).show();

                }
                else
                {
                    UpdateItemStatusToCompleted(getIntent().getStringExtra("Invoice").toString());

                    ShowBill(getIntent().getStringExtra("Invoice").toString());

                    _loading.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if(d!=null)d.dismiss();
            }
        });


    }

    private void SortLocalDatabase()
    {

        long countedId=0;
        QueryBuilder<Items> selItems = historyDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                .orderDesc(ItemsDao.Properties.Id)
                .where(ItemsDao.Properties.Id.ge(countedId))
               ;

        for(Items item : selItems.list())
        {

            List<Items> duplicate = historyDao.queryBuilder()
                    .where(ItemsDao.Properties.IsPaid.eq("false"))
                    .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                    .where(ItemsDao.Properties.ItemName.eq(item.getItemName()))
                    .list();
            int qty=0;

            Log.d("_Insert_","Inside OrderedItemsActivity, SortLocalDatabase, ItemName : "+item.getItemName());

            Items combined = new Items
                    (item.getId(),item.getItemName(),item.getIsPaid(),item.getQty(),item.getResId(),item.getHasBeenOrdered()
                            ,item.getPrice(),item.getTable(),item.getIsVeg(),item.getItemUID()
                            ,"","","","","");

            for(Items i : duplicate)
            {
                qty += i.getQty();

                historyDao.deleteByKey(i.getId());
            }

            Log.d("hundred_2","yo man 1");
            combined.setQty(qty);
            historyDao.insert(combined);

        }
    }

    private void ShowBill(String InvoiceNo)
    {
        Intent intent = new Intent (OrderedItemsActivity.this,BillActivity.class);

        intent.putExtra("ResId",ResId);
        intent.putExtra("Table",Table);
        intent.putExtra("Invoice",InvoiceNo);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);


        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    //Update Local Database as well as Firebase
    /*OrderHistory-ResId-UID-
                            Invoice
                            GrandTotal
                            Items - <Name>-qty
     */
    private void UpdateItemStatusToCompleted( final String InvoiceNo)
    {
        final Dialog d =UtilFunctions.ShowLoadingBar(OrderedItemsActivity.this);
        selectedItemsDao.deleteAll();

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        final String _date = dateFormat.format(date);


        String UID =  OrderHistory.push().getKey().toString();

        OrderHistory.child(UID).child("Invoice").setValue(InvoiceNo+"");
        OrderHistory.child(UID).child("GrandTotal").setValue(GrandTotal+"");
        OrderHistory.child(UID).child("Date").setValue(_date+"");


        List<Items> selItems = historyDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                .list();

        //As a correction for removing other Schwifty markers
        if(Constants.paymentSystem.equals("central"))
        {

            Log.d("for_malls_check_central","Payment Method is "+Constants.paymentSystem);
            List<Items> _selItems = historyDao.queryBuilder()
                    .where(ItemsDao.Properties.IsPaid.eq("false"))
                    .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                    .list();

            for(Items s:_selItems) {
                final Items item = new Items(s.getId(), s.getItemName(), "true", s.getQty(), s.getResId(), s.getHasBeenOrdered(), s.getPrice()
                        , s.getTable(), s.getIsVeg(), s.getItemUID(), "", "", "", "", "");

                if (s.getItemName().equals("Schwifty"))
                {
                    Log.d("for_malls_check_central","Schwifty is deleted, ItemName= "+s.getItemName());
                    historyDao.deleteByKey(s.getId());
                    break;
                }
            }
        }

        for(Items s:selItems)
        {
            final Items item = new Items(s.getId(),s.getItemName(),"true",s.getQty(),s.getResId(),s.getHasBeenOrdered(),s.getPrice()
                    ,s.getTable(),s.getIsVeg(),s.getItemUID(),"","","","","");

            if(s.getItemName().equals("Schwifty"))
            {
                break;
            }


            Log.d("hundred_100",s.getItemName()+" - "+s.getQty());

            String key = OrderHistory.child(UID).child("Items").push().getKey().toString();
            OrderHistory.child(UID).child("Items").child(key).child("Item").setValue(s.getItemName()+"");
            OrderHistory.child(UID).child("Items").child(key).child("qty").setValue(s.getQty()+"");
            OrderHistory.child(UID).child("Items").child(key).child("price").setValue(s.getPrice()+"");
            OrderHistory.child(UID).child("Items").child(key).child("isVeg").setValue(s.getIsVeg()+"");
            OrderHistory.child(UID).child("Items").child(key).child("UID").setValue(s.getItemUID()+"")
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            historyDao.deleteByKey(item.getId());
                            historyDao.insert(item);

                        }
                    });




        }

        //Schwifty,GST,_,ResId,_,Invoice,Table,_.

        if(Constants.paymentSystem.equals("central"))
        {
            //remove previous Schwifty marker

        }

        FirebaseDatabase.getInstance().getReference().child(Constants.RestaurantDetails).child(ResId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String ResName = dataSnapshot.child("ResName").getValue().toString();
                historyDao.insert(new Items(System.currentTimeMillis(),"Schwifty","true",
                        100,ResName+","+ResId,"true",InvoiceNo,Table,Constants.getInvoice_para2,Constants.getInvoice_para3
                        ,"","","","",""));
                d.dismiss();
                ShowBill(InvoiceNo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                d.dismiss();
            }
        });


    }

    private Double TotalCost = 0.0;
    private Double GrandTotal = 0.0;
    private int nQty =0;


    List<String> orderRef = new ArrayList<>();

    private void LoadModuleData()
    {
        Log.d("_OrderedItems","Type = "+Constants.Type);
        if(Constants.Type.equals("Rest"))
        {
            SyncOnlineData();
        }

        if(Constants.Type.equals("Mall"))
        {
            LoadDataSimply();
        }
    }

    private void LoadDataSimply()
    {
        PopulateView();
    }

    private void SyncOnlineData() {

        final Dialog loader = UtilFunctions.ShowLoadingBar(this);

        final DatabaseReference onlineItemsRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.Order).child(ResId);

        Query query = onlineItemsRef.orderByChild("__Table__").startAt(Table).endAt(Table);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //delete current database

                if(dataSnapshot.exists()) {
                    List<Items> tobesyncSel = selectedItemsDao.queryBuilder()
                            .where(ItemsDao.Properties.IsPaid.eq("false"))
                            .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                            .list();
                    for (Items s : tobesyncSel) {
                        selectedItemsDao.deleteByKey(s.getId());
                    }

                    List<Items> tobesyncOrd = historyDao.queryBuilder()
                            .where(ItemsDao.Properties.IsPaid.eq("false"))
                            .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                            .list();
                    for (Items s : tobesyncOrd) {
                        historyDao.deleteByKey(s.getId());
                    }

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        //new data
                        if (snapshot.hasChild("__Table__")) {
                            Log.d("_retrive_data", "" + snapshot.child("Items").getChildrenCount() +
                                    " Table : " + snapshot.child("__Table__").getValue().toString());

                            for (DataSnapshot itemSnapshot : snapshot.child("Items").getChildren()) {

                                if (itemSnapshot.hasChild("qty") &&
                                        itemSnapshot.hasChild("UID") &&
                                        itemSnapshot.hasChild("PriceEach") &&
                                        itemSnapshot.hasChild("isVeg")) {
                                    String itemName = itemSnapshot.getKey().toString();
                                    String qty = itemSnapshot.child("qty").getValue().toString();
                                    String itemUID = itemSnapshot.child("UID").getValue().toString();
                                    String eachPrice = itemSnapshot.child("PriceEach").getValue().toString();
                                    String isVeg = itemSnapshot.child("isVeg").getValue().toString();

                                    Log.d("_hundred", "Items = " + itemName + " " + qty);

                                    Log.d("hundred_hu", System.currentTimeMillis() + " " + itemName + " " + Integer.parseInt(qty) + " " + ResId + " " + "true" + " " + eachPrice + " " + Table + " " + isVeg + " " + itemUID);

                                    //  Items item = new Items(System.currentTimeMillis() , itemName, Integer.parseInt(qty), ResId, "true", eachPrice, Table, isVeg, itemUID);

                                    long __id = selectedItemsDao.loadAll().size() + 1;

                                    Items item = new Items(__id, itemName, Integer.parseInt(qty), ResId, "true",
                                            eachPrice, Table, isVeg, itemUID, selectedItemsDao, true);

                                    Items _item = new Items((long) historyDao.loadAll().size(), itemName, Integer.parseInt(qty), ResId, "true",
                                            eachPrice, Table, isVeg, itemUID, historyDao, true);

                                    selectedItemsDao.insert(item);
                                    historyDao.insert(_item);

                                    if (loader != null) loader.dismiss();

                                }
                            }

                        }
                    }

                    PopulateView();
                    if (!isFinishing() && loader != null) loader.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
               if(loader!=null) loader.dismiss();
            }
        });
    }

    private void PopulateView()
    {
        View vAnimEmpty = findViewById(R.id.animation_orderedItems_emptylist);
        View proc=findViewById(R.id.orderedItems_proceed);
        ViewGroup.LayoutParams params = proc.getLayoutParams();
        int origHeight=params.height;

        List<Items> selItems = selectedItemsDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                .list();

        Log.d("_debug_hundred","selItems Size = "+selItems.size());
        if(selItems.size()<=0)
        {
            vAnimEmpty.setVisibility(View.VISIBLE);

            params.height = 0;
            proc.setLayoutParams(params);
        }
        else {
            vAnimEmpty.setVisibility(View.GONE);
            params.height = origHeight;
            proc.setLayoutParams(params);


            OrderedItemsList.removeAllViews();
            TotalCost = 0.0;
            GrandTotal = 0.0;
            nQty = 0;

            UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.template_ordereditems);
            UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.blankspace_horizontalline);
            for (Items s : selItems) {
                View view = UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.template_ordereditems);

                TextView vName = view.findViewById(R.id.template_orderedItems_ItemName);
                TextView vQty = view.findViewById(R.id.template_orderedItems_Quantity);
                TextView vPrice = view.findViewById(R.id.template_orderedItems_Total);
                double cost =

                        Math.round
                                (
                                        ((s.getQty() * Double.parseDouble(s.getPrice())) * 100.0)
                                ) / 100.0;

                TotalCost += cost;
                vName.setText(s.getItemName());
                vQty.setText("" + s.getQty() + "");
                vPrice.setText("\u20B9" + cost + "");
                nQty += s.getQty();

            }

            UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.blankspace_horizontalline);

            //Items Total
            View view1 = UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.template_ordereditems);
            TextView vName1 = view1.findViewById(R.id.template_orderedItems_ItemName);
            TextView vQty1 = view1.findViewById(R.id.template_orderedItems_Quantity);
            TextView vPrice1 = view1.findViewById(R.id.template_orderedItems_Total);
            vName1.setText("Items Total : ");
            vQty1.setText(nQty + "");
            vPrice1.setText("\u20B9" + TotalCost);
            vName1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            vQty1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            vPrice1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

            //GST
            View view3 = UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.template_ordereditems);
            TextView vName3 = view3.findViewById(R.id.template_orderedItems_ItemName);
            TextView vQty3 = view3.findViewById(R.id.template_orderedItems_Quantity);
            final TextView vPrice3 = view3.findViewById(R.id.template_orderedItems_Total);
            vName3.setText("GST : ");
            vQty3.setText("");
            vName3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            vQty3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            vPrice3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            vPrice3.setText("...");

            //Service
            View view4 = UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.template_ordereditems);
            TextView vName4 = view4.findViewById(R.id.template_orderedItems_ItemName);
            TextView vQty4 = view4.findViewById(R.id.template_orderedItems_Quantity);
            final TextView vPrice4 = view4.findViewById(R.id.template_orderedItems_Total);
            vName4.setText("Service Charges : ");
            vQty4.setText("");
            vName4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            vQty4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            vPrice4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            vPrice4.setText("...");

            //Grand Total
            View view2 = UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.template_ordereditems);
            TextView vName2 = view2.findViewById(R.id.template_orderedItems_ItemName);
            TextView vQty2 = view2.findViewById(R.id.template_orderedItems_Quantity);
            final TextView vPrice2 = view2.findViewById(R.id.template_orderedItems_Total);
            vName2.setText("Grand Total : ");
            vQty2.setText("");
            vName2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            vQty2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            vPrice2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            vPrice2.setText("...");


            UtilFunctions.ViewInflater(inflater, OrderedItemsList, R.layout.blankspace_horizontalline);


            //Calculate GST and other costs
            final Dialog d = UtilFunctions.ShowLoadingBar(OrderedItemsActivity.this);
            RestaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String GSTValue = dataSnapshot.child("GSTValue").getValue().toString();
                    String ServiceCharge = dataSnapshot.child("ServiceCharge").getValue().toString();
                    Double GST = Math.round((TotalCost * Double.parseDouble(GSTValue)) * 100.0) / 100.0;
                    Double ServiceCost = Math.round((TotalCost * Double.parseDouble(ServiceCharge)) * 100.0) / 100.0;
                    GrandTotal = Math.round((TotalCost + GST + ServiceCost)*100.0)/100.0;
                    Log.d("hundred_ou",ServiceCharge+" "+ServiceCost+" "+GrandTotal);

                    vPrice3.setText("\u20B9" + GST);
                    vPrice4.setText("\u20B9" +ServiceCost);
                    vPrice2.setText("\u20B9" + GrandTotal);

                    //Update Pay btn
                    TextView payBtn = findViewById(R.id.template_ordered_Pay);
                    payBtn.setText("Pay \u20B9" + GrandTotal);
                    if(!isFinishing())
                    if(d!=null)d.dismiss();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if(!isFinishing())
                        if(d!=null) d.dismiss();
                }
            });
        }

    }

    @Override
    public void onBackPressed() {

        //FilesUtils.SendBasicInfo(OrderedItemsActivity.this,MenuActivity.class,ResId+","+Table+",0");

        Intent intent =new Intent (OrderedItemsActivity.this,SelectedItemsActivity.class);

        intent.putExtra("Res",ResId);
        intent.putExtra("Table",Table);
        intent.putExtra("Chair","0");

        startActivity(intent);
        overridePendingTransition(R.anim.fadein,R.anim.fadeout);

    }

    private void NavigationalView(final FloatingNavigationView mFloatingNavigationView) {

        mFloatingNavigationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mFloatingNavigationView!=null)mFloatingNavigationView.open();
            }
        });
        mFloatingNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem menuItem)
            {
                // Snackbar.make((View) mFloatingNavigationView.getParent(), menuItem.getTitle() + " Selected!", Snackbar.LENGTH_LONG).show();
                mFloatingNavigationView.close();

                String Option = menuItem.getTitle().toString();

                if(Option.equals("Home"))
                {
                    Home();
                }

                if(Option.equals("Call Waiter"))
                {
                    CallWaiter(ResId,Table,R.id.root_order);
                }
                if(Option.equals("Change Email"))
                {
                    ChangeEmail(OrderedItemsActivity.this);
                }

                if(Option.equals("About"))
                {
                    About();
                }


                return true;
            }

        });
    }

    private void Home()
    {
        Intent intent =new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
    }

    private void About()
    {
        final Dialog dialogb = new Dialog(this);
        dialogb.setContentView(R.layout.dialog_about);
        dialogb.setTitle("Menu");
        //  dialogb.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogb.show();

        View v = dialogb.findViewById(R.id.about_ok);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogb.dismiss();
            }
        });


    }

    private void CallWaiter(final String ResId,final String Table,int root)
    {
        String uid = FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table).push().getKey().toString();
        FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table).child(uid).child("Purpose").setValue("Help");
        //Toast.makeText(MenuActivity.this, "Waiter has been called", Toast.LENGTH_SHORT).show();
        Snackbar.make(findViewById(root), "Waiter has been called", Snackbar.LENGTH_LONG).show();


    }

    private void ChangeEmail(final Activity activity)
    {
        final BasicUserDataDao dao = ((SelectedItemsApp)getApplication()).getBasicUserDataSession().getBasicUserDataDao();

        if(dao.loadAll().size()>0)
        {
            final BasicUserData d = dao.queryBuilder().where(BasicUserDataDao.Properties.Id.eq(1L)).list().iterator().next();

            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_get_email);
            dialog.setTitle("Email");
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            View vokBtn = dialog.findViewById(R.id.email_okbtn);
            final EditText vemail = dialog.findViewById(R.id.email_address);

            String email="";

            if(!d.getEmail().equals("null"))
            {
                email = d.getEmail().trim().toString();
                vemail.setText("");
                vemail.setHint(email);
            }

            vokBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    if(TextUtils.isEmpty(vemail.getText().toString()))
                    {
                        Toast.makeText(activity, "email can't be empty", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        dao.deleteByKey(1L);
                        dao.insert(new BasicUserData(1L, vemail.getText().toString()));
                        Toast.makeText(activity, "E-Mail changed", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
            dialog.show();

        }
    }

    private void ConfigToolbar(String title, final String ResId, final String Table, final Activity activity)
    {

        View backBtn = findViewById(R.id.appbar_backhbtn);
        View moreBtn = findViewById(R.id.appbar_more);

        TextView Title = findViewById(R.id.appbar_title);
        Title.setText(title);

        backBtn.setVisibility(View.VISIBLE);

        if(selectedItemsDao.queryBuilder().where(ItemsDao.Properties.IsPaid.eq(false))
                .where(ItemsDao.Properties.HasBeenOrdered.eq(false)).list().size()<=0)
        {
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    FilesUtils.SendBasicInfo(OrderedItemsActivity.this,MenuActivity.class,ResId+","+Table+","+"0",Constants.res);
                }
            });
        }
        else
        {
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    onBackPressed();
                }
            });
        }



        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                final Dialog dialog = new Dialog(activity);
                dialog.setContentView(R.layout.dialog_menugroups);
                dialog.setTitle("Menu");
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
                wmlp.gravity = Gravity.TOP | Gravity.END;
                wmlp.x = 10;   //x position
                wmlp.y = 10;   //y position


                // set the custom dialog components - text, image and button
                LinearLayout vContainer = dialog.findViewById(R.id.MenuGroupContainer);
                vContainer.removeAllViews();


                View v = UtilFunctions.ViewInflater(inflater, vContainer, R.layout.template_menu_items);
                //Add click listners
                View callWaiter = v.findViewById(R.id.callwaiter);
                View about = v.findViewById(R.id.about);
                View exit = v.findViewById(R.id.exit);
                View alredyOrdered = v.findViewById(R.id.viewordered);
                View cart = v.findViewById(R.id.viewcart);
                View changeMail=v.findViewById(R.id.changemail);

                View visitMenu = v.findViewById(R.id.viewmenu);
                visitMenu.setVisibility(View.VISIBLE);
                visitMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FilesUtils.SendBasicInfo(OrderedItemsActivity.this,MenuActivity.class,ResId+","+Table+","+0,Constants.res);
                    }
                });

                cart.setVisibility(View.VISIBLE);
                alredyOrdered.setVisibility(View.VISIBLE);

                changeMail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final BasicUserDataDao dao = ((SelectedItemsApp)getApplication()).getBasicUserDataSession().getBasicUserDataDao();

                        if(dao.loadAll().size()>0)
                        {
                            final BasicUserData d = dao.queryBuilder().where(BasicUserDataDao.Properties.Id.eq(1L)).list().iterator().next();

                            final Dialog dialog = new Dialog(OrderedItemsActivity.this);
                            dialog.setContentView(R.layout.dialog_get_email);
                            dialog.setTitle("Email");
                            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                            View vokBtn = dialog.findViewById(R.id.email_okbtn);
                            final EditText vemail = dialog.findViewById(R.id.email_address);

                            String email="";

                            if(!d.getEmail().equals("null"))
                            {
                                email = d.getEmail().trim().toString();
                                vemail.setText("");
                                vemail.setHint(email);
                            }

                            vokBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view)
                                {
                                    if(TextUtils.isEmpty(vemail.getText().toString()))
                                    {
                                        Toast.makeText(OrderedItemsActivity.this, "email can't be empty", Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        dao.deleteByKey(1L);
                                        dao.insert(new BasicUserData(1L, vemail.getText().toString()));
                                        Toast.makeText(OrderedItemsActivity.this, "E-Mail changed", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    }
                                }
                            });
                            dialog.show();

                        }
                    }
                });

                alredyOrdered.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent =new Intent (activity,OrderedItemsActivity.class);

                        intent.putExtra("ResId",ResId);
                        intent.putExtra("Table",Table);
                        intent.putExtra("Chair",0);

                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    }
                });

                cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent =new Intent (activity,SelectedItemsActivity.class);

                        intent.putExtra("Res",ResId);
                        intent.putExtra("Table",Table);
                        intent.putExtra("Chair",0);

                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    }
                });

                exit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(activity,MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    }
                });


                callWaiter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String uid = FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table).push().getKey().toString();
                        FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table).child(uid).child("Purpose").setValue("Help");
                        Toast.makeText(activity, "Waiter has been called", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });

                about.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Show dialog

                        final Dialog dialogb = new Dialog(activity);
                        dialogb.setContentView(R.layout.dialog_about);
                        dialogb.setTitle("Menu");
                        //  dialogb.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialogb.show();

                        View v = dialogb.findViewById(R.id.about_ok);
                        v.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialogb.dismiss();
                            }
                        });

                        dialog.dismiss();
                    }
                });


                dialog.show();

            }
        });

    }

    //Pay btn listners
    View.OnClickListener payment = new View.OnClickListener() {
        @Override
        public void onClick(View view)
        {

            if(t!=null)t.cleanUp();


            final Dialog dialog = new Dialog(OrderedItemsActivity.this);
            dialog.setContentView(R.layout.dialog_paymentmethod);
            dialog.setTitle("Payment Method");
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // set the custom dialog components - text, image and button
            View vCash = dialog.findViewById(R.id.PayByCash);
            View vCard =  dialog.findViewById(R.id.PayByCard);
            View vCancel = dialog.findViewById(R.id.CancelPayment);

            TextView tvCard = dialog.findViewById(R.id.pay2);
            tvCard.setText("Pay By Card");

            vCash.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View view) {

                    final Dialog d = UtilFunctions.ShowLoadingBar(OrderedItemsActivity.this);

                    String url=Constants.getInvoiceURL(
                            Constants.getInvoice_para1,
                            Constants.getInvoice_para2,
                            Constants.getInvoice_para3);


                    Ion.with(getApplicationContext())
                            .load(url)
                            .asString()
                            .setCallback(new FutureCallback<String>() {
                                @Override
                                public void onCompleted(Exception e, String result)
                                {

                                    if(result.contains(":")) {
                                        String invoice = result.split(":")[1];

                                        if(d!=null)d.dismiss();

                                        CallWaiterToCompleteThePayment("Cash", invoice);
                                    }
                                    else
                                    {
                                        Toast.makeText(OrderedItemsActivity.this, "Something went wrong\nPlease restart the app to complete the payment", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });


                }
            });

            vCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final Dialog d = UtilFunctions.ShowLoadingBar(OrderedItemsActivity.this);

                    String url=Constants.getInvoiceURL(
                            Constants.getInvoice_para1,
                            Constants.getInvoice_para2,
                            Constants.getInvoice_para3);

                    Ion.with(getApplicationContext()).load(url)
                            .asString()
                            .setCallback(new FutureCallback<String>() {
                                @Override
                                public void onCompleted(Exception e, String result) {

                                    if(result.contains(":")) {
                                        String invoice = result.split(":")[1];
                                        d.dismiss();

                                        CallWaiterToCompleteThePayment("Card", invoice);
                                    }
                                    else
                                    {
                                        Toast.makeText(OrderedItemsActivity.this, "Something went wrong\nPlease restart the app to complete the payment", Toast.LENGTH_LONG).show();

                                    }
                                }
                            });

                }
            });

            vCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(dialog!=null)dialog.dismiss();
                }
            });

            dialog.show();
        }
    };
}
