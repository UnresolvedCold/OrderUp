package com.schwifty.serviceplease;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.google.firebase.database.FirebaseDatabase;
import com.schwifty.serviceplease.Database_ORM.BasicUserData;
import com.schwifty.serviceplease.Database_ORM.BasicUserDataDao;
import com.schwifty.serviceplease.Database_ORM.Items;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    ItemsDao historyDao;
    List<Items> selItems;

    String moduleType="Rest";

    LayoutInflater inflater;
    private LinearLayout layoutHolder;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

       /* mToolbar = findViewById(R.id.history_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("History");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);*/

        historyDao = ((SelectedItemsApp) getApplication()).getHistorySession().getItemsDao();

        inflater = LayoutInflater.from(this);

        layoutHolder = findViewById(R.id.History_ItemsHolder);



        ListHistory();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isFinishing()) {
                    layoutHolder.removeAllViews();
                    ListHistory();
                }
            }
        }, 5000);

        ConfigToolbar("History","0","0",this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        layoutHolder.removeAllViews();
        ListHistory();


    }

    Boolean isMoreItemsHidden=true;

    private void ListHistory()
    {

        selItems = historyDao.queryBuilder()
                  .where(ItemsDao.Properties.IsPaid.eq("true"))
                  .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                  .orderDesc(ItemsDao.Properties.Id)
                .list();

        Log.d("history size ",""+selItems.size());

        if(selItems.size()>0)
        {
            findViewById(R.id.emptyHistoryAnimation).setVisibility(View.GONE);

           // UtilFunctions.ViewInflater(inflater, layoutHolder, R.layout.activity_bill);

            View mainView=null;
            LinearLayout ItemsContainer=null;
            int count = 0;


            for (Items item : selItems)
            {

                if(item.getItemName().equals("Schwifty"))
                {

                    mainView = UtilFunctions.ViewInflater(inflater, layoutHolder, R.layout.template_history_group);
                    ItemsContainer = mainView.findViewById(R.id.history_itemsContainer);
                    TextView ResName = mainView.findViewById(R.id.history_group_ResName);
                    TextView Date = mainView.findViewById(R.id.history_group_Date);
                    View Bill = mainView.findViewById(R.id.ViewBill);

                    String dt[] = UtilFunctions.GetDateAndTimeFromMillis(item.getId()).split(",");

                    ResName.setText(item.getResId().split(",")[0]);
                    final String ResId= item.getResId().split(",")[1];
                    final String type = item.getIsVeg();
                    final String MallId=item.getItemUID();
                    final long flag = item.getId();
                    Date.setText(dt[1]+"-"+dt[0]+"-"+dt[2]);
                    count=0;


                    Bill.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(type.contains("Mall"))
                            {
                                Constants.LoadMallModule(MallId,ResId);
                            }
                            else
                            {
                                Constants.LoadRestModule(ResId);
                            }

                            Intent intent = new Intent(HistoryActivity.this,RevisitBill.class);
                            intent.putExtra("ResId",ResId);
                            intent.putExtra("flag",flag);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                        }
                    });

                }
                else {

                   /* if(ItemsContainer!=null) {

                        View view = UtilFunctions.ViewInflater(inflater, ItemsContainer, R.layout.template_history_items);

                        TextView name = view.findViewById(R.id.history_items_name);
                        TextView qty = view.findViewById(R.id.history_items_qty);

                        name.setText(item.getItemName());
                        qty.setText("" + item.getQty());
                        count++;

                        if(count>2)
                        {
                            view.setVisibility(View.GONE);
                            final View v =mainView.findViewById(R.id.history_more);
                            final ImageView imgMore = mainView.findViewById(R.id.history_more_image);

                            if(count==3)
                            {
                                v.setVisibility(View.VISIBLE);

                                v.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view)
                                {

                                    if(isMoreItemsHidden)
                                    {
                                        imgMore.setBackgroundResource(R.drawable.ic_less);
                                        isMoreItemsHidden=false;
                                    }
                                    else
                                    {
                                        imgMore.setBackgroundResource(R.drawable.ic_more);
                                        isMoreItemsHidden=true;
                                    }
                                }
                            });
                            }
                        }

                    }*/
                }

            }



        }
        else
        {
            findViewById(R.id.emptyHistoryAnimation).setVisibility(View.VISIBLE);
        }

        NavigationalView((FloatingNavigationView) findViewById(R.id.history_floating_navigation_view));

    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(HistoryActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
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

                if(Option.equals("Change Email"))
                {
                    ChangeEmail(HistoryActivity.this);
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
        moreBtn.setVisibility(View.GONE);

        TextView Title = findViewById(R.id.appbar_title);
        Title.setText(title);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
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

                cart.setVisibility(View.GONE);
                alredyOrdered.setVisibility(View.GONE);

                changeMail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final BasicUserDataDao dao = ((SelectedItemsApp)getApplication()).getBasicUserDataSession().getBasicUserDataDao();

                        if(dao.loadAll().size()>0)
                        {
                            final BasicUserData d = dao.queryBuilder().where(BasicUserDataDao.Properties.Id.eq(1L)).list().iterator().next();

                            final Dialog dialog = new Dialog(HistoryActivity.this);
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
                                        Toast.makeText(HistoryActivity.this, "email can't be empty", Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        dao.deleteByKey(1L);
                                        dao.insert(new BasicUserData(1L, vemail.getText().toString()));
                                        Toast.makeText(HistoryActivity.this, "E-Mail changed", Toast.LENGTH_SHORT).show();
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


}
