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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import com.schwifty.serviceplease.Database_ORM.BasicUserData;
import com.schwifty.serviceplease.Database_ORM.BasicUserDataDao;
import com.schwifty.serviceplease.Database_ORM.Items;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;

import org.json.JSONObject;

import java.util.List;

import tourguide.tourguide.TourGuide;

public class SelectedItemsActivity extends AppCompatActivity implements PaymentResultListener {

    private String Res;
    private String Table;

    private View confirmOrder;
    private DatabaseReference OrderReference;

    static ItemsDao selectedItemsDao;
    static ItemsDao historyDao;
    static ItemsDao basicDao;
    LinearLayout selectedItemsLayout;
    LayoutInflater inflater;

    Boolean trigger = true;

    Double totalCost=0.0;

    private Double GSTper=0.0;
    private Double __ServiceCharge=0.0;

    private String GSTDetails="";
    private String ServiceCharge="";

    private Double GrandTotal=0.0;

    private Toolbar mToolbar;

    int proceedBtnHeight=0;
    TourGuide t;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_items);

       /* mToolbar = findViewById(R.id.selectedItems_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Selected Items");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);*/

       Constants.methodOfPayment="na";
       Constants.orderUID="na";

        selectedItemsDao = ((SelectedItemsApp)getApplication()).getSelectedItemsSession().getItemsDao();
        historyDao = ((SelectedItemsApp) getApplication()).getHistorySession().getItemsDao();
        basicDao = ((SelectedItemsApp) getApplication()).getBasicUserDataSession().getItemsDao();


        selectedItemsLayout = findViewById(R.id.selectedItemsList);
        inflater = LayoutInflater.from(this);

        Intent intent = getIntent();
        Res = intent.getStringExtra("Res");
        Table = intent.getStringExtra("Table");

        OrderReference = FirebaseDatabase.getInstance().getReference().child(Constants.Order).child(Res);

        //Updating Proceed Button
        FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(Res).child(Table)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            LinearLayout textView = findViewById(R.id.confirmOrder);
                            ViewGroup.LayoutParams params = textView.getLayoutParams();

                            if(dataSnapshot.toString().contains("Purpose=Pay"))
                            {

                                params.height = 0;
                                textView.setLayoutParams(params);
                            }
                            else
                            {
                                params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
                                textView.setLayoutParams(params);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


        //Get GST
        FirebaseDatabase.getInstance().getReference().child(Constants.RestaurantDetails).child(Res).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GSTper = Double.parseDouble(dataSnapshot.child("GSTValue").getValue().toString());
                GSTDetails=dataSnapshot.child("GSTDetails").getValue().toString();
                ServiceCharge=dataSnapshot.child("ServiceCharge").getValue().toString();
                __ServiceCharge = Double.parseDouble(ServiceCharge);
                PopulateSelectedItemsView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        confirmOrder = findViewById(R.id.confirmOrder);

        //Show Guide after showing the button
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isFinishing())
                {
                    t =UtilFunctions.showGuide(105L,confirmOrder,SelectedItemsActivity.this,"Click 'Palce Order'",
                            "to order your food\nyour request will be sent to the kitchen","#e54d26", Gravity.TOP,
                            ((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(),TourGuide.Technique.Click);
                }
            }
        }, 400);



        if(Constants.Type.equals("Rest")) {
            confirmOrder.setOnClickListener(confirmOrder_rest_listener);
        }

        if(Constants.Type.equals("Mall"))
        {
            Checkout.preload(getApplicationContext());
            confirmOrder.setOnClickListener(getConfirmOrder_mall_listner);
        }

        UpdateSelectedItemsList();

        ConfigToolbar("Cart",Res,Table,this);

        NavigationalView((FloatingNavigationView) findViewById(R.id.appbar_floating_navigation_view));

    }


    private void GoToOrderedItemsPage(String invoice)
    {
        trigger = false;

        Intent intent = new Intent(SelectedItemsActivity.this,OrderedItemsActivity.class);

        Constants.InvoiceNo=invoice;
        intent.putExtra("ResId",Res);
        intent.putExtra("Table",Table);
        intent.putExtra("Invoice",invoice);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    private void SaveOrderToHistory()
    {
        List<Items> selItems = selectedItemsDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                .list();

        for(Items s:selItems)
        {
            if(s.getQty()>0)
            {
                Log.d("_Insert_","Inside SelectedItemsActivity, SaveOrderToHistory, ItemName : "+s.getItemName());
                long _id = s.getId();

                Items item = new Items(s.getId(),s.getItemName(),s.getQty(),s.getResId(),"true"
                        ,s.getPrice(),s.getTable(),s.getIsVeg(),s.getItemUID(),historyDao,true);
                Items _item = new Items(s.getId(),s.getItemName(),s.getQty(),s.getResId(),"true"
                        ,s.getPrice(),s.getTable(),s.getIsVeg(),s.getItemUID(),selectedItemsDao,true);

                Log.d("hundred_hie",s.getId()+" "+s.getItemName()+" "+s.getQty()+" "+historyDao.loadAll().size());

                historyDao.insert(item);
                selectedItemsDao.deleteByKey(_id);
                selectedItemsDao.insert(_item);

            }
        }

    }

    private void SendOrderToKitchen(String _para_)
    {
        final Dialog d =UtilFunctions.ShowLoadingBar(SelectedItemsActivity.this);
        List<Items> selItems = selectedItemsDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                //add one more condition for ordered == no
                .list();

        String OrderUID = OrderReference.push().getKey().toString();
        Constants.orderUID=OrderUID;

        for(Items s:selItems)
        {
            OrderReference.child(OrderUID).child("Items").child(s.getItemName()).child("qty").setValue(s.getQty());
            OrderReference.child(OrderUID).child("Items").child(s.getItemName()).child("UID").setValue(s.getItemUID());
            OrderReference.child(OrderUID).child("Items").child(s.getItemName()).child("PriceEach").setValue(s.getPrice());
            OrderReference.child(OrderUID).child("Items").child(s.getItemName()).child("isVeg").setValue(s.getIsVeg());

        }

        EditText extraRequest = findViewById(R.id.ExtraDetailsToAdd);
        String s=extraRequest.getText().toString().trim();

        if(_para_.contains("Cash"))
        {
            OrderReference.child(OrderUID).child("_Status_").setValue("WaitForCash");
            OrderReference.child(OrderUID).child("__PendingTable__").setValue(""+Table);
            OrderReference.child(OrderUID).child("__Table__").setValue("Your order will be placed once you pay the cash at the counter");
        }
        else
        {
            OrderReference.child(OrderUID).child("__Table__").setValue(""+Table);
        }

        if(!TextUtils.isEmpty(s))
        {
            OrderReference.child(OrderUID).child("__ExtraRequests__").setValue(s);
        }



        d.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UpdateSelectedItemsList();
    }

    private void UpdateSelectedItemsList()
    {
        PopulateSelectedItemsView();
    }

    private void PopulateSelectedItemsView()
    {
        View vAnimEmpty = findViewById(R.id.animation_selectedItems_emptylist);
        final View proc=findViewById(R.id.confirmOrder);
        ViewGroup.LayoutParams params = proc.getLayoutParams();
        int origHeight=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.proceedBtnHeight, getResources().getDisplayMetrics());

        List<Items> selectedItemsList = selectedItemsDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                .list();


        if(selectedItemsList.size()<=0)
        {
            vAnimEmpty.setVisibility(View.VISIBLE);
           // params.height = 0;
           // proc.setLayoutParams(params);

            if(proc.getHeight()>0)
            UtilFunctions.animateHeight(proc,origHeight,0,300,View.VISIBLE);

        }
        else {

            vAnimEmpty.setVisibility(View.GONE);

           // params.height = origHeight;
           // proc.setLayoutParams(params);

            if(proc.getHeight()<=0)
                UtilFunctions.animateHeight(proc,0,origHeight,300,View.VISIBLE);

            totalCost = 0.0;

            selectedItemsLayout.removeAllViews();
            for (Items i : selectedItemsList) {
                //Inflate here
                View view = UtilFunctions.ViewInflater(inflater, selectedItemsLayout, R.layout.template_selecteditems);

                //find elements and attach listners and updaters
                final TextView vName = view.findViewById(R.id.template_smenu_itemName);
                final TextView vNItems = view.findViewById(R.id.template_smenu_IncDecHolder_view);
                View vInc = view.findViewById(R.id.template_smenu_IncDecHolder_inc);
                View vDec = view.findViewById(R.id.template_smenu_IncDecHolder_dec);
                TextView vPrice = view.findViewById(R.id.template_smenu_price);
                TextView vCost = view.findViewById(R.id.template_smenu_cost);
                ImageView veg_nonveg = view.findViewById(R.id.template_smenu_veg_nonveg);

                vNItems.setText(i.getQty() + "");

                final String name = i.getItemName();
                vName.setText(name);
                vPrice.setText("\u20B9" + i.getPrice() + "");

                double cost =

                        Math.round
                                (
                                        ((i.getQty() * Double.parseDouble(i.getPrice())) * 100.0)
                                ) / 100.0;
                vCost.setText("\u20B9" + cost + "");

                //Update the total cost
                totalCost += cost;

                if (i.getIsVeg().contains("true")) {
                    veg_nonveg.setImageResource(R.drawable.icon_veg);
                } else {
                    veg_nonveg.setImageResource(R.drawable.icon_nonveg);
                }

                vInc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        List<Items> selItems = selectedItemsDao.queryBuilder()
                                .where(ItemsDao.Properties.ItemName.eq(
                                        name
                                ))
                                .where(ItemsDao.Properties.IsPaid.eq("false"))
                                .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                                .list();

                        for (Items s : selItems) {
                            int qty = s.getQty() + 1;
                            Long id = s.getId();
                            String price = s.getPrice();
                            String table = s.getTable();
                            String isVeg = s.getIsVeg();
                            String ItemUID = s.getItemUID();
                            selectedItemsDao.deleteByKey(s.getId());

                            if (qty > 0) {
                                selectedItemsDao.insert(new Items(id, name, qty, Res, "false", price, table, isVeg,ItemUID,selectedItemsDao,true));
                                vNItems.setText(qty + "");

                            }

                            PopulateSelectedItemsView();
                        }
                    }
                });

                vDec.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        List<Items> selItems = selectedItemsDao.queryBuilder()
                                .where(ItemsDao.Properties.ItemName.eq(
                                        name
                                ))
                                .where(ItemsDao.Properties.IsPaid.eq("false"))
                                .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                                .list();

                        for (Items s : selItems) {
                            int qty = s.getQty() - 1;
                            Long id = s.getId();
                            String price = s.getPrice();
                            String table = s.getTable();
                            String isVeg = s.getIsVeg();
                            String ItemUID = s.getItemUID();
                            selectedItemsDao.deleteByKey(s.getId());

                            if (qty > 0) {
                                selectedItemsDao.insert(new Items(id, name, qty, Res, "false", price, Table, isVeg,ItemUID,selectedItemsDao,true));
                                vNItems.setText(qty + "");

                            }

                            {
                                PopulateSelectedItemsView();
                            }
                        }

                    }
                });


            }

            if (totalCost > 0.0) {

                GrandTotal = (Math.round((totalCost + totalCost * GSTper + totalCost*__ServiceCharge) * 100.0) / 100.0);

                View view = UtilFunctions.ViewInflater(inflater, selectedItemsLayout, R.layout.template_totalamount);
                TextView vItemTotal = view.findViewById(R.id.selItm_ItemTotal);
                TextView vName = view.findViewById(R.id.selItm_GrandTotal);
                TextView vSGSTDetails=view.findViewById(R.id.selItm_SGSTDetails);
                TextView vSGST=view.findViewById(R.id.selItm_SGST);
                TextView vCGSTDetails=view.findViewById(R.id.selItm_CGSTDetails);
                TextView vCGST=view.findViewById(R.id.selItm_CGST);
                TextView vServiceCharge = view.findViewById(R.id.selItems_serviceCharge);


                vItemTotal.setText("\u20B9" + totalCost + "");

                if (GrandTotal <= totalCost) {
                    vName.setText("...");
                    vCGST.setText("...");
                    vSGST.setText("...");
                    vServiceCharge.setText("...");
                } else {
                    String CGST=GSTDetails.split(",")[1];
                    String SGST=GSTDetails.split(",")[0];

                    String _CGST= "" +(Double.parseDouble(CGST.split("%")[0])*totalCost/100.0);
                    String _SGST= "" +(Double.parseDouble(SGST.split("%")[0])*totalCost/100.0);
                    String _ServiceCharge = "" +Math.round((Double.parseDouble(ServiceCharge)*totalCost)*100.0)/100.0;

                    vName.setText("\u20B9" + GrandTotal);
                    vCGST.setText("\u20B9"+_CGST);
                    vSGST.setText("\u20B9"+_SGST);
                    vCGSTDetails.setText("CGST @"+CGST);
                    vSGSTDetails.setText("SGST @"+SGST);
                    vServiceCharge.setText("\u20B9"+_ServiceCharge);
                }

            }
        }

    }

    @Override
    public void onBackPressed() {

        trigger=false;
        FilesUtils.SendBasicInfo(SelectedItemsActivity.this,MenuActivity.class,Res+","+Table+",0",Constants.res);
    }

    @Override
    protected void onStart() {
        super.onStart();
        trigger = true;

        UtilFunctions.checkValidity(this,Res,Table,true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("hundred","trigger(Sel) : "+trigger);
      /*  if(trigger) {

            List<Items> selItems = selectedItemsDao.queryBuilder()
                    .where(ItemsDao.Properties.IsPaid.eq("false"))
                    .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                    .list();

            for (Items s : selItems)
            {

                selectedItemsDao.deleteByKey(s.getId());

                Log.d("hundred", "Sel onStop");
            }
        }*/

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
                    CallWaiter(Res,Table,R.id.root_sele);
                }
                if(Option.equals("Change Email"))
                {
                    ChangeEmail(SelectedItemsActivity.this);
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
        View kuchtoh = findViewById(R.id.appbar_kuchtoh);

        TextView Title = findViewById(R.id.appbar_title);
        Title.setText(title);

        backBtn.setVisibility(View.VISIBLE);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });

        kuchtoh.setVisibility(View.VISIBLE);

        kuchtoh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    trigger=false;
                    Intent intent =new Intent (activity,OrderedItemsActivity.class);

                    intent.putExtra("ResId",ResId);
                    intent.putExtra("Table",Table);
                    intent.putExtra("Chair",0);

                    startActivity(intent);
                    overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                }
            });


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
                        FilesUtils.SendBasicInfo(SelectedItemsActivity.this,MenuActivity.class,ResId+","+Table+","+0,Constants.res);
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

                            final Dialog dialog = new Dialog(SelectedItemsActivity.this);
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
                                        Toast.makeText(SelectedItemsActivity.this, "email can't be empty", Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        dao.deleteByKey(1L);
                                        dao.insert(new BasicUserData(1L, vemail.getText().toString()));
                                        Toast.makeText(SelectedItemsActivity.this, "E-Mail changed", Toast.LENGTH_SHORT).show();
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
                        trigger=false;
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

    View.OnClickListener confirmOrder_rest_listener = new View.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            //Send to firebase
            // Order/ResUID/Table/Chair
            if(t!=null)t.cleanUp();
            SendOrderToKitchen("__");

            //Save to history and mark unpaid
            SaveOrderToHistory();

            //Reload View
           // PopulateSelectedItemsView();

            //Send to next activity
            GoToOrderedItemsPage(Constants.InvoiceNo);

        }

    };

    View.OnClickListener getConfirmOrder_mall_listner = new View.OnClickListener() {
        @Override
        public void onClick(View view)
        {

            if(t!=null)t.cleanUp();

            if(GrandTotal>0.2) {
                //Open Dialog with 2 options Razor and Cash
                final Dialog dialog = new Dialog(SelectedItemsActivity.this);
                dialog.setContentView(R.layout.dialog_paymentmethod);
                dialog.setTitle("Payment Method");
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                // set the custom dialog components - text, image and button
                View vCash = dialog.findViewById(R.id.PayByCash);
                View vCard =  dialog.findViewById(R.id.PayByCard);
                View vCancel = dialog.findViewById(R.id.CancelPayment);

                vCash.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onClick(View view) {

                        final Dialog d = UtilFunctions.ShowLoadingBar(SelectedItemsActivity.this);

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

                                        Log.d("HTML",result.split(":")[1]);

                                        String invoice = result.split(":")[1];

                                        d.dismiss();

                                        Constants.methodOfPayment="Cash";

                                        UpdateItemsAsSuccess("Cash");

                                       // CallWaiterToCompleteThePayment("Cash",invoice);
                                    }
                                });


                    }
                });

                vCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Log.d("_Sel_","onClick, Constatns.ResDetails : "+Constants.RestaurantDetails);

                        //UpdateItemsAsSuccess();
                        startPayment();

                    }
                });

                vCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(dialog!=null)dialog.dismiss();
                    }
                });

                if(!isFinishing()&&dialog!=null)dialog.show();

            }
            else
            {
                Toast.makeText(SelectedItemsActivity.this, "Please wait while the total is being calculated", Toast.LENGTH_SHORT).show();
            }

        }
    };

    public void startPayment() {

        final String moneyInPaise = Integer.toString((int)(GrandTotal*100.0));
        Log.d("_Razorpay_total",moneyInPaise);

        /**
         * Instantiate Checkout
         */
        Checkout checkout = new Checkout();

        /**
         * Set your logo here
         */
        //checkout.setImage(R.drawable.logo);

        /**
         * Reference to current activity
         */
        final Activity activity = this;

        /**
         * Pass your payment options to the Razorpay Checkout as a JSONObject
         */
        try {
            JSONObject options = new JSONObject();

            /**
             * Merchant Name
             * eg: ACME Corp || HasGeek etc.
             */
            options.put("name", "Schwifty Technologies");

            /**
             * Description can be anything
             * eg: Order #123123
             *     Invoice Payment
             *     etc.
             */
            options.put("description", "Order ");

            options.put("currency", "INR");

            /**
             * Amount is always passed in PAISE
             * Eg: "500" = Rs 5.00
             */
            options.put("amount", moneyInPaise);

            checkout.open(activity, options);
        } catch(Exception e) {
            Log.e("_Ordered", "Error in starting Razorpay Checkout", e);
        }
    }

    @Override
    public void onPaymentSuccess(String s)
    {
        UpdateItemsAsSuccess("__");
    }

    private void UpdateItemsAsSuccess(String para)
    {
        Log.d("_Sel_","OnPaymentSuccess, Constatns.ResDetails : "+Constants.RestaurantDetails);
        selectedItemsDao = ((SelectedItemsApp)getApplication()).getSelectedItemsSession().getItemsDao();
        historyDao = ((SelectedItemsApp) getApplication()).getHistorySession().getItemsDao();
        basicDao = ((SelectedItemsApp) getApplication()).getBasicUserDataSession().getItemsDao();

        //Send to Kitchen
        SendOrderToKitchen(para);

        //Save to history and mark unpaid
        SaveOrderToHistory();

        final Dialog d = UtilFunctions.ShowLoadingBar(SelectedItemsActivity.this);

        String url=Constants.getInvoiceURL(
                Constants.getInvoice_para1,
                Constants.getInvoice_para2,
                Constants.getInvoice_para3);

        Ion.with(getApplicationContext()).load(url)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {

                        Log.d("HTML",result.split(":")[1]);

                        String invoice = result.split(":")[1];

                        d.dismiss();

                        //Send to next activity
                        Log.d("_Invoice_","SelectedItemsActivity, onPaymentSuccess, Invoice : "+invoice);
                        GoToOrderedItemsPage(invoice);

                    }
                });
    }

    @Override
    public void onPaymentError(int i, String s)
    {
        if(i== Checkout.NETWORK_ERROR)
        {
            Toast.makeText(this, "Payment failed due to no network", Toast.LENGTH_SHORT).show();
        }

        if(i==Checkout.PAYMENT_CANCELED)
        {
            Toast.makeText(this, "Payment was cancelled", Toast.LENGTH_SHORT).show();
        }
    }
}
