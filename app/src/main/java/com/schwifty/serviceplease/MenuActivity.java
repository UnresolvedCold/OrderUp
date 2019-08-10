package com.schwifty.serviceplease;

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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.schwifty.serviceplease.Database_ORM.BasicUserData;
import com.schwifty.serviceplease.Database_ORM.BasicUserDataDao;
import com.schwifty.serviceplease.Database_ORM.CurrentScannedEntity;
import com.schwifty.serviceplease.Database_ORM.CurrentScannedEntityDao;
import com.schwifty.serviceplease.Database_ORM.DaoSession;
import com.schwifty.serviceplease.Database_ORM.Items;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;
import com.schwifty.serviceplease.Effects.OnSwipeTouchListener;
import com.schwifty.serviceplease.Mall.Mall_ShopListActivity;

import org.greenrobot.greendao.annotation.Id;

import java.util.List;

import tourguide.tourguide.TourGuide;

import static android.view.View.GONE;

public class MenuActivity extends AppCompatActivity {

    private String [] IntentData;


    //menuNode is the reference to a node where data is grouped as MenuGroup/MenuItem
    private DatabaseReference menuNode;
    private MenuList menuList;

    LayoutInflater inflater;

    ScrollView menuListScroller;

    LinearLayout menuLayout;

    ItemsDao selectedItemsDao;

    Boolean trigger = true;

    Double totalItemCost=0.0;

    TourGuide mTourguide=null;

    private View vProceed;
    private View vMenuScroll;

    private Toolbar mToolbar;

    static int value_x = Constants.menu_group_def_position_x_dp;
    static int value_y = Constants.menu_group_def_position_y_dp; //for showing menu groups dialog

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

       /* mToolbar = findViewById(R.id.menu_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Menu");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);*/

        selectedItemsDao = ((SelectedItemsApp)getApplication()).getSelectedItemsSession().getItemsDao();

       // selectedItemsDao.deleteAll();
        //Get Data from intent
        Intent intent = getIntent();
        IntentData = new String[Constants.name.length];



        for(int i = 0;i<Constants.name.length;i++)
        {
            IntentData[i] = intent.getStringExtra(Constants.name[i]);
        }

        UtilFunctions.checkValidity(this,IntentData[0],IntentData[1],true);

        SaveCurrents();

        //Init Global Variables
        inflater = LayoutInflater.from(this);
        menuLayout=(LinearLayout) findViewById(R.id.menu_List);
        menuListScroller = findViewById(R.id.menulist_scrollview);
        menuList=new MenuList();
        menuNode = FirebaseDatabase.getInstance().getReference().child(Constants.menuRef);

        ShowMenu(IntentData[Constants.RestaurantUID]);

        SearchMenu();
        ConfigToolbar("Menu",IntentData[0],IntentData[1]);

        vProceed=findViewById(R.id.menu_proceed);

        vProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Send to next activity

                trigger = false;

                if(menuList.getTour()!=null)menuList.getTour().cleanUp();

                Intent intent = new Intent(MenuActivity.this, SelectedItemsActivity.class);
                intent.putExtra("Res",IntentData[0]);
                intent.putExtra("Table",IntentData[1]);
                intent.putExtra("Chair",IntentData[2]);

                startActivity(intent);

            }
        });

        vMenuScroll = findViewById(R.id.menu_choose);

        mTourguide = UtilFunctions.showGuide(104L,vMenuScroll,this,"Click 'Menu' to view item groups","",
                "#e54d26",Gravity.TOP,((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(),TourGuide.Technique.Click);

        findViewById(R.id.root_menu).setOnTouchListener(new OnSwipeTouchListener(this)
        {
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
            }

            @Override
            public void onSwipeLeft() {
                //super.onSwipeLeft();
                Intent intent1 = new Intent(MenuActivity.this,SelectedItemsActivity.class);
                startActivity(intent1);
            }
        });

        View callWaiter=findViewById(R.id.menu_callwaiter);

        if(Constants.Type.equals("Mall"))
        {
            callWaiter.setVisibility(GONE);
        }

        callWaiter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CallWaiter(IntentData[0],IntentData[1]);
            }
        });

    }

    private void SaveCurrents()
    {

         CurrentScannedEntityDao dao = ((SelectedItemsApp)getApplication()).getCurrentsSession().getCurrentScannedEntityDao();
         dao.deleteAll();
         CurrentScannedEntity e = new CurrentScannedEntity(100L,Constants.Type,Constants.getInvoice_para1,Constants.getInvoice_para3,IntentData[1]);
         dao.insert(e);

    }

    @Override
    protected void onStart() {
        super.onStart();

        vMenuScroll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(mTourguide!=null)mTourguide.cleanUp();
                mTourguide = UtilFunctions.showGuide(103L,findViewById(R.id.appbar_menu_searchbtn),MenuActivity.this,"Click here ","to search an item",
                        "#e54d26",Gravity.BOTTOM,((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(),TourGuide.Technique.Click);
                //Dialog to enter PassCode
                final Dialog dialog = new Dialog(MenuActivity.this);
                dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
                dialog.setContentView(R.layout.dialog_menugroups);
                dialog.setTitle("Menu");
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
                wmlp.gravity = Gravity.BOTTOM|Gravity.RIGHT ;
               // wmlp.x = 30;   //x position
               // wmlp.y = location[1]-vMenuScroll.getHeight();   //y position
                wmlp.x = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value_x, getResources().getDisplayMetrics());
                wmlp.y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value_y, getResources().getDisplayMetrics());

                // set the custom dialog components - text, image and button
                LinearLayout vContainer = dialog.findViewById(R.id.MenuGroupContainer);
                vContainer.removeAllViews();

                for(MenuGroup g : menuList.groups) {

                    final View s = g.GroupView;

                    View v = UtilFunctions.ViewInflater(inflater, vContainer, R.layout.template_group_items);
                    TextView tvName = v.findViewById(R.id.templateGroupItems);
                    TextView tvQty = v.findViewById(R.id.templateGroupNItems);
                    tvName.setText(g.GroupName);
                    tvQty.setText(g.getNItems()+"");

                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ScrollTo(s);
                            dialog.dismiss();
                        }
                    });
                }

                dialog.show();


            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        trigger=true;

        ShowMenu(IntentData[Constants.RestaurantUID]);
    }

    @Override
    public void onBackPressed() {

        View showMenuItems = findViewById(R.id.showMenuItems);
        if(showMenuItems.getVisibility()==View.VISIBLE)
        {
            showMenuItems.setVisibility(GONE);
        }
        else
        {
            List<Items> selItems = selectedItemsDao.queryBuilder()
                    .where(ItemsDao.Properties.IsPaid.eq("false"))
                    .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                    .list();

            for (Items s : selItems)
            {

                selectedItemsDao.deleteByKey(s.getId());

                Log.d("hundred", "Sel onStop");
            }
            Log.d("hundred","deleted by back pressed");

            //super.onBackPressed();
            if(Constants.Type.equals("Rest")) {

                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }

            if(Constants.Type.equals("Mall"))
            {
                Intent intent = new Intent(MenuActivity.this, Mall_ShopListActivity.class);
                intent.putExtra("MallId",Constants.getInvoice_para3);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        Log.d("hundred","trigger(Menu) : "+trigger);

        if(trigger)
        {
            List<Items> selItems = selectedItemsDao.queryBuilder()
                    .where(ItemsDao.Properties.IsPaid.eq("false"))
                    .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                    .list();

            for (Items s : selItems)
            {

                selectedItemsDao.deleteByKey(s.getId());

                Log.d("hundred", "Sel onStop");
            }
            Log.d("hundred","dao deleted");


        }

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
                    CallWaiter(IntentData[0],IntentData[1]);
                }
                if(Option.equals("Change Email"))
                {
                    ChangeEmail();
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
        final Dialog dialogb = new Dialog(MenuActivity.this);
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

    private void CallWaiter(final String ResId,final String Table)
    {
        String uid = FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table).push().getKey().toString();
        FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table).child(uid).child("Purpose").setValue("Help");

        Log.d("hundred_oiu",Constants.CallWaiter+"\n"+
                FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter).child(ResId).child(Table).toString()+"\n"+
                "Table="+Table+" Intent 1="+IntentData[1]);
        //Toast.makeText(MenuActivity.this, "Waiter has been called", Toast.LENGTH_SHORT).show();
        Snackbar.make(findViewById(R.id.root_menu), "Waiter has been called", Snackbar.LENGTH_LONG).show();
    }

    private void ChangeEmail()
    {
        final BasicUserDataDao dao = ((SelectedItemsApp)getApplication()).getBasicUserDataSession().getBasicUserDataDao();

        if(dao.loadAll().size()>0)
        {
            final BasicUserData d = dao.queryBuilder().where(BasicUserDataDao.Properties.Id.eq(1L)).list().iterator().next();

            final Dialog dialog = new Dialog(MenuActivity.this);
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
                        Toast.makeText(MenuActivity.this, "email can't be empty", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        dao.deleteByKey(1L);
                        dao.insert(new BasicUserData(1L, vemail.getText().toString()));
                        Toast.makeText(MenuActivity.this, "E-Mail changed", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
            dialog.show();

        }
    }

    private void ConfigToolbar(String title, final String ResId, final String Table)
    {

        View backBtn = findViewById(R.id.appbar_menu_backhbtn);
        View moreBtn = findViewById(R.id.appbar_menu_more);
        View cartBtn = findViewById(R.id.appbar_menu_cartbtn);

        FloatingNavigationView menuBtn = findViewById(R.id.appbar_menu_floating_navigation_view);

        TextView Title = findViewById(R.id.appbar_menu_title);
        Title.setText(title);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                onBackPressed();
            }
        });

        NavigationalView(menuBtn);

        cartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trigger=false;

                ItemsDao historyDao = ((SelectedItemsApp) getApplication()).getHistorySession().getItemsDao();

                if(selectedItemsDao.queryBuilder().where(ItemsDao.Properties.IsPaid.eq(false))
                        .where(ItemsDao.Properties.HasBeenOrdered.eq(false)).list().size()<=0)
                {
                    Intent intent =new Intent (MenuActivity.this,OrderedItemsActivity.class);

                    intent.putExtra("ResId",IntentData[0]);
                    intent.putExtra("Table",IntentData[1]);
                    intent.putExtra("Chair",IntentData[2]);

                    startActivity(intent);
                    overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                }
                else
                {
                    Intent intent =new Intent (MenuActivity.this,SelectedItemsActivity.class);

                    intent.putExtra("Res",IntentData[0]);
                    intent.putExtra("Table",IntentData[1]);
                    intent.putExtra("Chair",IntentData[2]);

                    startActivity(intent);
                    overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                }

            }
        });

        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                final Dialog dialog = new Dialog(MenuActivity.this);
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
                        FilesUtils.SendBasicInfo(MenuActivity.this,MenuActivity.class,ResId+","+Table+","+0,Constants.res);
                    }
                });


                cart.setVisibility(View.VISIBLE);
                alredyOrdered.setVisibility(View.VISIBLE);

                changeMail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        ChangeEmail();
                    }
                });

                alredyOrdered.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        trigger=false;
                        Intent intent =new Intent (MenuActivity.this,OrderedItemsActivity.class);

                        intent.putExtra("ResId",IntentData[0]);
                        intent.putExtra("Table",IntentData[1]);
                        intent.putExtra("Chair",IntentData[2]);

                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    }
                });

                cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        trigger=false;
                        Intent intent =new Intent (MenuActivity.this,SelectedItemsActivity.class);

                        intent.putExtra("Res",IntentData[0]);
                        intent.putExtra("Table",IntentData[1]);
                        intent.putExtra("Chair",IntentData[2]);

                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    }
                });

                exit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MenuActivity.this,MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    }
                });


                callWaiter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                       CallWaiter(ResId,Table);
                       dialog.dismiss();
                    }
                });

                about.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Show dialog

                        About();
                        dialog.dismiss();
                    }
                });


                dialog.show();

            }
        });

    }


    private String switchState="*";
    int searchContainerHeight=0; //searchContainer
    private void SearchMenu()
    {

        View SearchBtn = findViewById(R.id.appbar_menu_searchbtn);
        final View SearchContainer = findViewById(R.id.appbar_menu_searchcontainer);
        final EditText SearchText = findViewById(R.id.appbar_menu_searchText);
        final Switch isVegSwitch = (Switch)findViewById(R.id.switch_isVegSwitcher);


        isVegSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if(isChecked)
                {
                    switchState="true";

                }
                else
                {
                    switchState="*";
                }
                String s = SearchText.getText().toString();
                if(TextUtils.isEmpty(s)) {
                    menuList.MenuSearchInflater("*", switchState, inflater, menuLayout, R.layout.template_menu, MenuActivity.this, IntentData[0], IntentData[1]);
                }
                else
                {
                    menuList.MenuSearchInflater(s, switchState, inflater, menuLayout, R.layout.template_menu, MenuActivity.this, IntentData[0], IntentData[1]);

                }
            }
        });



         SearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                String searchString = SearchText.getText().toString();

                if(TextUtils.isEmpty(searchString))
                {
                    searchString="*";
                }

                menuList.MenuSearchInflater(searchString,switchState,inflater,menuLayout,R.layout.template_menu,MenuActivity.this,IntentData[0],IntentData[1]);

                if(mTourguide!=null)mTourguide.cleanUp();

            }
        });


        SearchContainer.post(new Runnable() {
            @Override
            public void run() {
                SearchContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                searchContainerHeight = SearchContainer.getMeasuredHeight();
            }
        });

        SearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText e = findViewById(R.id.appbar_menu_searchText);

                if(SearchContainer.getVisibility()==GONE) {

                    UtilFunctions.animateHeight(SearchContainer,0,searchContainerHeight,400,View.VISIBLE);

                }
                else
                {
                    UtilFunctions.animateHeight(SearchContainer,searchContainerHeight,0,400,View.GONE);

                }

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!isFinishing())
                        {
                            if(mTourguide!=null)mTourguide.cleanUp();
                            mTourguide = UtilFunctions.showGuide(102L,SearchContainer,MenuActivity.this,"Type what you want to search",
                                    "customize your search here",
                                    "#e54d26",Gravity.BOTTOM,((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(),
                                    TourGuide.Technique.Click);
                        }
                    }
                }, 400);

            }
        });
    }


    private void ScrollTo(View view)
    {
        menuListScroller.scrollTo(0,view.getTop());

        final TextView vGroupName = view.findViewById(R.id.template_menu_groupName);


        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(50); //You can manage the blinking time with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(5);
        vGroupName.startAnimation(anim);

        View showMenuItems = findViewById(R.id.showMenuItems);

        showMenuItems.setVisibility(View.GONE);

    }

    private void ShowMenu(String RUID) {
        //Connect to database and get the menu string
        //Don't parse JSON automatically
        //Then Format the menu to display it in the way you want
        //Don't user recycler view
        //Instead use dynamic addition and Scroll view

        final Dialog d =UtilFunctions.ShowLoadingBar(MenuActivity.this);

        menuNode.child(RUID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                menuList.groups.clear();

               // Log.d("hundred",dataSnapshot.getValue().toString());

                menuLayout.removeAllViews();
                for (DataSnapshot menuGrp : dataSnapshot.getChildren()) {
                    //get the name of group and display it
                    //store it all in a list
                    //List of List of GroupNames of List of Items
                    MenuGroup grp = new MenuGroup(menuGrp.getKey().toString());

                    for (DataSnapshot item : menuGrp.getChildren()) {
                        //get item name and diplay it
                        if(item.hasChild("hasSubItems") && item.hasChild("isAvailable")) {
                            MenuItem itm = new MenuItem(
                                    item.child("Name").getValue().toString(),
                                    item.child("isVeg").getValue().toString(),
                                    item.child("hasSubItems").getValue().toString(),
                                    item.child("Price").getValue().toString(),
                                    item.getKey().toString(),
                                    item.child("Details").getValue().toString(),
                                    item.child("isAvailable").getValue().toString()
                            );
                            grp.AddItem(itm);
                        }
                    }

                    menuList.AddGroup(grp);
                }
                menuList.MenuInflater(inflater,menuLayout,R.layout.template_menu,MenuActivity.this,IntentData[0],IntentData[1]);
               // menuList.MenuSearchInflater("*","t",inflater,menuLayout,R.layout.template_menu,MenuActivity.this,IntentData[0],IntentData[1]);
                d.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                d.dismiss();
            }
        });


    }

}
