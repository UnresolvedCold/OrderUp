package com.schwifty.serviceplease;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.schwifty.serviceplease.Database_ORM.Items;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tourguide.tourguide.TourGuide;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

//container for groups
public class MenuList
{
    List<MenuGroup> groups = new ArrayList<>();
    ItemsDao itemsDao;
    ItemsDao historyDao;

    View ProceedButonInMenuActivity;
    TourGuide t=null;


    public TourGuide getTour()
    {
        return t;
    }

    public void MenuInflater(LayoutInflater inflater, LinearLayout ViewParentElement, int R_Layout_Template, final Activity activity, final String res, final String Table)
    {

        MenuSearchInflater("*","*",inflater,ViewParentElement,R_Layout_Template,activity,res,Table);

    }



    public void MenuSearchInflater(String searchName,String searchVeg,LayoutInflater inflater, LinearLayout ViewParentElement, int R_Layout_Template, final Activity activity, final String res, final String Table)
    {
        final Dialog d =UtilFunctions.ShowLoadingBar(activity);

        Iterator<MenuGroup> grpItr = groups.iterator();
        itemsDao = ((SelectedItemsApp)activity.getApplication()).getSelectedItemsSession().getItemsDao();
        historyDao = ((SelectedItemsApp)activity.getApplication()).getHistorySession().getItemsDao();

        ViewParentElement.removeAllViews();

        while(grpItr.hasNext())
        {
            //Menu group inflator
            MenuGroup menuGroup = grpItr.next();


            Log.d("hundred_fow",""+menuGroup.getNAvailable()+" "+menuGroup.GroupName);
            if(menuGroup.getNAvailable()>0) {
                View view = UtilFunctions.ViewInflater(inflater, ViewParentElement, R_Layout_Template);
                menuGroup.setGroupView(view);


                String _grpName = menuGroup.GroupName;
                if (_grpName.contains("_")) {
                    _grpName = _grpName.split("_")[1];
                }

                ((TextView) (view.findViewById(R.id.template_menu_groupName))).setText(_grpName);

                int count = 0;
                //Inflate items
                Iterator<MenuItem> itmItr = menuGroup.Items.iterator();
                while (itmItr.hasNext()) {

                    //Menu UI

                    //Menu Response
                    final MenuItem item = itmItr.next();


                    //Search Conditions
                    boolean nameCondition;
                    boolean vegCondition;

                    if (searchName.equals("*")) {
                        nameCondition = true;
                    } else {
                        nameCondition = item.ItemName.toLowerCase().contains(searchName.toLowerCase());
                    }

                    if (searchVeg.equals("*")) {
                        vegCondition = true;
                    } else {
                        vegCondition = Boolean.parseBoolean(item.isVeg);
                    }


                    //Search Logic

                    if
                    (
                            nameCondition
                                    && vegCondition

                    ) {

                        View itemView = UtilFunctions.ViewInflater(inflater, (LinearLayout) view.findViewById(R.id.template_menu_itemsList), R.layout.template_item);

                        view.setVisibility(View.VISIBLE);

                        Log.d("hundred", item.ItemName + " " + "" + item.isAvailable);
                        if (!item.isAvailable) {
                            Log.d("hundred", "GONE");

                            itemView.setVisibility(GONE);
                        } else {
                            count++;
                        }

                        TextView vItemName = itemView.findViewById(R.id.template_menu_itemName);
                        final View vAddButton = itemView.findViewById(R.id.template_menu_Add);
                        final View vIncDecButtonHolder = itemView.findViewById(R.id.template_menu_IncDecHolder);
                        View vIncQty = itemView.findViewById(R.id.template_menu_IncDecHolder_inc);
                        View vDecQty = itemView.findViewById(R.id.template_menu_IncDecHolder_dec);
                        final TextView vNQty = itemView.findViewById(R.id.template_menu_IncDecHolder_view);
                        final TextView vDetails = itemView.findViewById(R.id.Details);

                        ImageView veg_nonveg = itemView.findViewById(R.id.template_menu_veg_nonveg);

                        final TextView vCost = itemView.findViewById(R.id.template_menu_cost);


                        vItemName.setText(item.ItemName);
                        vCost.setText("\u20B9" + item.price);

                        if (!item.Details.contains("$Empty$")) {
                            itemView.findViewById(R.id.DetailsContainer).setVisibility(VISIBLE);
                            vDetails.setText(item.Details);
                        } else {
                            itemView.findViewById(R.id.DetailsContainer).setVisibility(GONE);
                        }

                        final List<Items> selItems = itemsDao.queryBuilder()
                                .where(ItemsDao.Properties.ItemName.eq(
                                        item.ItemName
                                ))
                                .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                                .list();

                        if (selItems.size() > 0 && selItems.iterator().next().getIsPaid().equals("false")) {
                            Items itm = selItems.iterator().next();
                            vAddButton.setVisibility(GONE);
                            vNQty.setText(itm.getQty() + "");
                            vIncDecButtonHolder.setVisibility(View.VISIBLE);
                        }


                        if (item.isVeg.contains("true")) {
                            veg_nonveg.setImageResource(R.drawable.icon_veg);
                        } else {
                            veg_nonveg.setImageResource(R.drawable.icon_nonveg);
                        }

                        vAddButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                t = null;
                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!activity.isFinishing()) {
                                            t = UtilFunctions.showGuide(109L, activity.findViewById(R.id.viewcart), activity, "Click 'View Cart'",
                                                    "to have a view of your selected items", "#e54d26", Gravity.TOP,
                                                    ((SelectedItemsApp) activity.getApplication()).getBasicUserDataSession().getBasicUserDataDao(),
                                                    TourGuide.Technique.Click);
                                        } else {
                                            t = null;
                                        }
                                    }
                                }, 400);


                                vAddButton.setVisibility(GONE);
                                vIncDecButtonHolder.setVisibility(View.VISIBLE);

                                // itemsDao.insert(new Items(Long.parseLong(((itemsDao.loadAll().size())+1) + ""), item.ItemName, 1,res,"false",item.price));

                                itemsDao.insert(new Items(System.currentTimeMillis(), item.ItemName, 1, res, "false", item.price, Table, item.isVeg, item.UID,
                                        itemsDao, true));

                                vNQty.setText("1");

                                MenuAvtivity_ProceedBtn_Updater(activity);


                            }
                        });


                        vIncQty.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                List<Items> selItems = itemsDao.queryBuilder()
                                        .where(ItemsDao.Properties.ItemName.eq(
                                                item.ItemName
                                        ))
                                        .where(ItemsDao.Properties.IsPaid.eq("false"))
                                        .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                                        .list();

                                for (Items s : selItems) {
                                    int qty = s.getQty() + 1;
                                    Long id = s.getId();
                                    itemsDao.deleteByKey(s.getId());
                                    itemsDao.insert(new Items(id, item.ItemName, qty, res, "false", item.price, Table, item.isVeg, item.UID, itemsDao, false));
                                    vNQty.setText(qty + "");
                                }

                                MenuAvtivity_ProceedBtn_Updater(activity);


                            }
                        });


                        vDecQty.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                List<Items> selItems = itemsDao.queryBuilder()
                                        .where(ItemsDao.Properties.ItemName.eq(
                                                item.ItemName
                                        ))
                                        .where(ItemsDao.Properties.IsPaid.eq("false"))
                                        .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                                        .list();

                                for (Items s : selItems) {
                                    int qty = s.getQty() - 1;
                                    Long id = s.getId();
                                    itemsDao.deleteByKey(s.getId());

                                    if (qty > 0) {
                                        itemsDao.insert(new Items(id, item.ItemName, qty, res, "false", item.price, Table, item.isVeg, item.UID, itemsDao, true));
                                        vNQty.setText(qty + "");

                                    } else {
                                        vIncDecButtonHolder.setVisibility(GONE);
                                        vNQty.setText("0");
                                        vAddButton.setVisibility(View.VISIBLE);
                                    }
                                }

                                MenuAvtivity_ProceedBtn_Updater(activity);

                            }
                        });

                        MenuAvtivity_ProceedBtn_Updater(activity);
                    } else if (count <= 0) {
                        view.setVisibility(GONE);
                    }


                }
            }

        }

        UtilFunctions.ViewInflater(inflater,ViewParentElement, R.layout.template_menublank);

        d.dismiss();

    }




    public void AddGroup(MenuGroup group)
    {
        groups.add(group);
    }


    private double totalCost=0.0;
    private int nQty=0;

    public void MenuAvtivity_ProceedBtn_Updater(Activity activity)
    {

        totalCost=0.0;
        nQty = 0;
        TextView vCost = activity.findViewById(R.id.activity_menu_cost);
        TextView vNItems = activity.findViewById(R.id.activity_menu_nItems);

        List<Items> list = itemsDao.queryBuilder()
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .where((ItemsDao.Properties.HasBeenOrdered.eq("false")))
                .list();

        for(Items i : list)
        {
            totalCost+=Math.round(i.getQty() * Double.parseDouble(i.getPrice())*100.0)/100.0;
            nQty+=i.getQty();
        }

        vCost.setText("\u20B9"+totalCost);

        if(nQty == 1) {
            vNItems.setText(nQty+"");
        }
        else
        {
            vNItems.setText(nQty + "");
        }

        LinearLayout textView = activity.findViewById(R.id.menu_proceed);

        List selItms = itemsDao.queryBuilder()
                .where(ItemsDao.Properties.HasBeenOrdered.eq("false"))
                .where(ItemsDao.Properties.IsPaid.eq("false"))
                .list();

        if(selItms.size()>0) {

            MenuActivity.value_y =Constants.menu_group_aft_position_y_dp;
            MenuActivity.value_x = Constants.menu_group_aft_position_x_dp;

            ViewGroup.LayoutParams params = textView.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, activity.getResources().getDisplayMetrics());
            //textView.setLayoutParams(params);

            if((textView.getHeight()<=0)) {
                UtilFunctions.animateHeight(textView, 0, params.height, 300, VISIBLE);
            }
        }
        else
        {
            MenuActivity.value_y =Constants.menu_group_def_position_y_dp;
            MenuActivity.value_x = Constants.menu_group_def_position_x_dp;

            ViewGroup.LayoutParams params = textView.getLayoutParams();
           // params.height = 0;
           // textView.setLayoutParams(params);
            if((textView.getHeight()>0))
            UtilFunctions.animateHeight(textView,params.height,0,300,VISIBLE);

        }

    }

    public Double getTotalItemCost()
    {
        Double d=0.0;

        List<Items> list = itemsDao.queryBuilder().where(ItemsDao.Properties.IsPaid.eq("false")).list();

        for(Items i : list)
        {
            d+=Math.round(i.getQty() * Double.parseDouble(i.getPrice())*100.0)/100.0;
        }


        return d;
    }

}

//Container for a group
class MenuGroup
{
    public String GroupName;
    public View GroupView;
    public List<MenuItem> Items;

    public int getNAvailable()
    {
        Iterator<MenuItem> i = this.Items.iterator();

        while(i.hasNext())
        {
            MenuItem _i = i.next();
            if(_i.isAvailable)
            {
                return 1;
            }
        }

        return -1;
    }

    MenuGroup(String groupName)
    {
        if(groupName.contains("_"))GroupName=groupName.split("_")[1];
        else GroupName=groupName;

        Items = new ArrayList<>();
        GroupView = null;
    }

    public void setGroupView(View view)
    {
        GroupView = view;
    }

    public void AddItem(MenuItem item)
    {

        Items.add(item);
    }

    public int getNItems()
    {
        Iterator<MenuItem> i = this.Items.iterator();

        int count =0;
        while(i.hasNext())
        {
            MenuItem _i = i.next();
            if(_i.isAvailable)
            {
                count++;
            }
        }

        return count;
    }

}

//Container for items
class MenuItem
{
    public String ItemName;
    public String isVeg;
    public String price;
    public String hasSubItems; // half plate,full plate
    public String UID;
    public String Details;
    public Boolean isAvailable;


    MenuItem(String itemName,String isVeg,String hasSubItems,String price,String UID,String Details,String isAvailable)
    {
        ItemName = itemName;
        this.isVeg= isVeg;
        this.hasSubItems = hasSubItems;
        this.price=price;
        this.UID = UID;
        this.Details= Details;
        this.isAvailable = Boolean.parseBoolean(isAvailable);
    }

}

