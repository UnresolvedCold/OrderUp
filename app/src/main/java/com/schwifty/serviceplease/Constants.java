package com.schwifty.serviceplease;

import android.os.Environment;
import android.util.Log;

import com.schwifty.serviceplease.Database_ORM.DaoSession;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;


import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Constants
{
    //QR Code Structure
    //to add a extra data just change the array here
    public static final String[] name = new String[]
            {"RUID","table","chair"};

    public static final int RestaurantUID = 0;

    public static int proceedBtnHeight = 45; //dp

    //Generating PDF
    public static String h_BMP_FileDirectory_sub = "Service Please/.bmp/";
    public static String path_Image_FileDirectory = Environment.getExternalStorageDirectory().toString()+"/Service Please/.bmp/";
    public static String path_PDF_FileDirectory = android.os.Environment.getExternalStorageDirectory().toString()+"/Service Please/";
    public static String h_PDF_FileDirectory_sub = "Service Please/.pdf/";
    public static String h_PDF_FileDirectory = android.os.Environment.getExternalStorageDirectory().toString()+"/Service Please/.pdf/";

    //Menu Group
    public static  int menu_group_def_position_x_dp = 25;
    public static  int menu_group_aft_position_x_dp = 25;
    public static  int menu_group_def_position_y_dp = 100;
    public static  int menu_group_aft_position_y_dp = 150;

    //Meta Data
    public static String Type="Rest";
    public static String menuRef="Menu";
    public static String OrderHistoryRef="OrderHistory";
    public static String Order="Order";
    public static String CallWaiter="CallWaiter";
    public static String OccupiedMembers="OccupiedMembers";
    public static  String RestaurantDetails="RestaurantDetails";
    public static String PendingSync="PendingSync";

    public static String getInvoice_para1="para1"; //RestId
    public static String getInvoice_para2="para2"; //Mall or Rest
    public static String getInvoice_para3="para3"; //MallId

    public static String InvoiceNo="0";

    public static String res = "__ValueForM@lls__";

    public static String methodOfPayment = "NA";
    public static String orderUID="NA";

    public static String paymentSystem="default";

    //Misc
    public static Boolean WaiterCalledForPayment = false;

    public static void setParas(String para1,String para2,String para3)
    {
        getInvoice_para1=para1;
        getInvoice_para2=para2;
        getInvoice_para3=para3;    }

    public static void LoadRestModule(String RestId)
    {
        Constants.Type="Rest";
        Constants.menuRef="Menu";
        Constants.RestaurantDetails="RestaurantDetails";
        Constants.Order="Order";
        Constants.OrderHistoryRef="OrderHistory";
        Constants.OccupiedMembers="OccupiedMembers";
        Constants.CallWaiter="CallWaiter";
        Constants.setParas(RestId,"Rest","para3");
    }

    public static void LoadMallModule(String MallId,String RestId)
    {
        Constants.Type="Mall";
        Constants.menuRef="Mall/"+MallId+"/Menu";
        Constants.RestaurantDetails="Mall/"+MallId+"/Shops";
        Constants.Order="Mall/"+MallId+"/Order";
        Constants.OrderHistoryRef="Mall/"+MallId+"/OrderHistory";
        Constants.OccupiedMembers="Mall/"+MallId+"/OccupiedMembers";
        Constants.CallWaiter="Mall/"+MallId+"/CallWaiter";
        Constants.setParas(RestId,"Mall",MallId);
    }

    public static String getInvoiceURL(String para1,String para2,String para3)
    {
        String url= "https://us-central1-serviceplease-25bf6.cloudfunctions.net/getInvoiceNumber/?" +
                "resId="+para1+
                "&type="+para2+
                "&mallId="+para3;
        Log.d("_URL_",url);
        return url;
    }

    public static String getOrderNo(String resId,String mallId)
    {
        String timeStamp = new SimpleDateFormat("ddMMyy").format(Calendar.getInstance().getTime());

        String url = "https://us-central1-serviceplease-25bf6.cloudfunctions.net/getOrderNumber?"+
                "resId="+resId+
                "&mallId="+mallId
                +"&date="+timeStamp;
        Log.d("_URL_",url);
        return url;
    }

    public static void SaveValues()
    {

    }

    public static void RetriveValues()
    {

    }

}
