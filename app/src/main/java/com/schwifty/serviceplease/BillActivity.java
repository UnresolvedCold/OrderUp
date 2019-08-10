package com.schwifty.serviceplease;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.schwifty.serviceplease.Database_ORM.BasicUserData;
import com.schwifty.serviceplease.Database_ORM.BasicUserDataDao;
import com.schwifty.serviceplease.Database_ORM.Items;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.acl.Group;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import tourguide.tourguide.TourGuide;

public class BillActivity extends AppCompatActivity {

    ItemsDao historyDao;
    LayoutInflater inflater;
    LinearLayout itemsList;

    String ResId;

    private static Bitmap bitScroll;

    DatabaseReference ResDetails;

    String ResName="Error";
    String ResAddress="Error";
    String GSTIN="Error";
    String ResTel="Error";
    String GSTValue="Error";
    String ServiceCharge="Error";
    String Invoice;
    String Table;

    Double ItemTotal=0.0;
    Double GrandTotal=0.0;
    Double GSTCost=0.0;
    Double ServiceCost=0.0;

    private Toolbar mToolbar;

   ;

    String email="null";

    TourGuide t=null;

    Dialog loading_email=null;
    Dialog loading_download = null;

    View vEmail;
    View vDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);

        inflater = LayoutInflater.from(this);
        itemsList = findViewById(R.id.billItemList);

        Intent intent = getIntent();
        ResId = intent.getStringExtra("ResId");
        Invoice = intent.getStringExtra("Invoice");
        Table = intent.getStringExtra("Table");

        Log.d("_Bill_","onCreate, RestDetails = "+Constants.RestaurantDetails);

        ResDetails = FirebaseDatabase.getInstance().getReference().child(Constants.RestaurantDetails).child(ResId);

        historyDao = ((SelectedItemsApp) getApplication()).getHistorySession().getItemsDao();

        PopulateBill();

        vEmail = findViewById(R.id.bill_email);
        vDownload = findViewById(R.id.bill_download);

        vEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(t!=null)t.cleanUp();

                loading_email = UtilFunctions.ShowLoadingBar(BillActivity.this);
                ValidateEmail();

            }
        });

        vDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(t!=null)t.cleanUp();
                t=UtilFunctions.showGuide(107L,vEmail,BillActivity.this,"Click email",
                        "to get your receipt emailed\nyou will receive your bill as an email attachment","#e54d26", Gravity.TOP,
                        ((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(),TourGuide.Technique.Click);

                loading_download= UtilFunctions.ShowLoadingBar(BillActivity.this);

                if(isReadStoragePermissionGranted())
                    if(isWriteStoragePermissionGranted())
                    {
                        ScrollView scrollView = findViewById(R.id.scroll_revBIll);
                        bitScroll = getBitmapFromView(scrollView, scrollView.getChildAt(0).getHeight(), scrollView.getChildAt(0).getWidth());
                        final Dialog d =UtilFunctions.ShowLoadingBar(BillActivity.this);
                        saveBitmap(bitScroll);
                        savePdf(true,Constants.path_PDF_FileDirectory);
                        d.dismiss();
                        /*Intent intent = new Intent(RevisitBill.this,PreviewActivity.class);
                        startActivity(intent);*/

                    }

            }
        });

        ConfigToolbar("Bill",ResId,Table,this);

        NavigationalView((FloatingNavigationView)findViewById(R.id.bill_floating_navigation_view));

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(t!=null)t.cleanUp();
        t =UtilFunctions.showGuide(108L,vDownload,BillActivity.this,"Click download",
                "to save your receipt\nyour receipt will be saved at sdcard/Service Please","#e54d26", Gravity.TOP,
                ((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(),TourGuide.Technique.Click);
    }

    private void PopulateBill()
    {

        final Dialog d =UtilFunctions.ShowLoadingBar(BillActivity.this);
        itemsList.removeAllViews();
        ItemTotal=0.0;
        GrandTotal=0.0;
        GSTCost=0.0;



        ResDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                Log.d("_Bill_","_PopulateBill_, ResDetails = "+dataSnapshot.getValue().toString());

                if(dataSnapshot.hasChild("ResName"))
                {
                    ResName = dataSnapshot.child("ResName").getValue().toString();
                    ResAddress = dataSnapshot.child("Address").getValue().toString();
                    ResTel = dataSnapshot.child("Tel").getValue().toString();
                    GSTIN = dataSnapshot.child("GSTIN").getValue().toString();

                    GSTValue = dataSnapshot.child("GSTValue").getValue().toString();
                    ServiceCharge=dataSnapshot.child("ServiceCharge").getValue().toString();


                }

                View lView = UtilFunctions.ViewInflater(inflater, itemsList, R.layout.template_restaurantdetails);

                TextView vResName = lView.findViewById(R.id.res_Name);
                TextView vResAddress = lView.findViewById(R.id.res_Address);
                TextView vResGSTIN = lView.findViewById(R.id.res_GSTIN);
                TextView vResTel = lView.findViewById(R.id.res_Tel);
                TextView vInvoice = lView.findViewById(R.id.res_Invoice);

                View vOrderContainer = lView.findViewById(R.id.res_OrderNo_container);
                final TextView vOrder = lView.findViewById(R.id.res_OrderNo);

                vResName.setText(ResName);
                vResAddress.setText(""+ResAddress);
                vResTel.setText("Tel : "+ResTel);
                vResGSTIN.setText("GSTIN No. : "+GSTIN);
                vInvoice.setText("Invoice No. : "+Invoice);
                if(Constants.Type.equals("Mall"))
                {
                    vOrderContainer.setVisibility(View.VISIBLE);


                    Log.d("_hundred_0","hg"+Constants.getInvoice_para3+" "+Constants.getInvoice_para1);
                    if(Constants.methodOfPayment.contains("Cash"))
                    {

                        FirebaseDatabase.getInstance().getReference()
                                .child("Mall")
                                .child(Constants.getInvoice_para3)
                                .child("Order")
                                .child(Constants.getInvoice_para1)
                                .child(Constants.orderUID)
                                .child("__Table__")
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists())
                                        {
                                            Log.d("_hundred_0","jh");
                                            String token = dataSnapshot.getValue().toString();

                                            vOrder.setText("Token No. : " + token);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                    }
                    else
                    {
                        vOrder.setText("Token No. : " + Table);
                    }
                }

                List<Items> selItems = historyDao.queryBuilder()
                        .where(ItemsDao.Properties.IsPaid.eq("true"))
                        .where(ItemsDao.Properties.HasBeenOrdered.eq("true"))
                        .orderDesc(ItemsDao.Properties.Id)
                        .list();

                UtilFunctions.ViewInflater(inflater, itemsList, R.layout.template_ordereditems);
                UtilFunctions.ViewInflater(inflater, itemsList, R.layout.blankspace_horizontalline);

                int count=0;
                for(Items s:selItems)
                {
                    if(s.getItemName().equals("Schwifty"))
                    {
                        if(count>0)
                            break;

                        count++;
                    }
                    else
                    {
                        View view = UtilFunctions.ViewInflater(inflater, itemsList, R.layout.template_ordereditems);

                        TextView vItmName = view.findViewById(R.id.template_orderedItems_ItemName);
                        TextView vQty = view.findViewById(R.id.template_orderedItems_Quantity);
                        TextView vPrice = view.findViewById(R.id.template_orderedItems_Total);

                        vItmName.setText(s.getItemName());
                        vQty.setText(s.getQty()+"");
                        vPrice.setText("\u20B9"+Math.round(Double.parseDouble(s.getPrice()) * s.getQty() * 100.0)/100.0+"");



                        double cost =

                                Math.round
                                        (
                                                ((s.getQty()*Double.parseDouble(s.getPrice()))*100.0)
                                        )/100.0;

                        ItemTotal+=cost;

                    }

                }

                UtilFunctions.ViewInflater(inflater, itemsList, R.layout.blankspace_horizontalline);

                //Display grand total here after displaying GSTIN

                GSTCost = Math.round(ItemTotal*Double.parseDouble(GSTValue)*100.0)/100.0;
                ServiceCost=Math.round(ItemTotal*Double.parseDouble(ServiceCharge)*100.0)/100.0;

                GrandTotal = Math.round((ItemTotal + GSTCost +ServiceCost)*100.0)/100.0;

                View view = UtilFunctions.ViewInflater(inflater, itemsList, R.layout.template_gst_total);
                TextView vGstCost = view.findViewById(R.id.GSTCost);
                TextView vGrandTotal=view.findViewById(R.id.GrandTotal);
                TextView vItemTotal = view.findViewById(R.id.ItemTotal);
                TextView vGSTINFO = view.findViewById(R.id.template_orderedItems_GSTInfo);
                TextView vServiceCharge = view.findViewById(R.id.ServiceCharge);

                vItemTotal.setText("\u20B9"+ItemTotal+"");
                vGrandTotal.setText("\u20B9"+GrandTotal+"");
                vServiceCharge.setText("\u20B9"+ServiceCost);
                vGstCost.setText("\u20B9"+GSTCost+"");

                String GSTDetails = dataSnapshot.child("GSTDetails").getValue().toString();

                vGSTINFO.setText("("+GSTDetails.split(",")[0]+" + "+GSTDetails.split(",")[1]+") ");

                d.dismiss();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                d.dismiss();
            }
        });

    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(BillActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2:


            case 3:
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                    ScrollView scrollView = findViewById(R.id.scroll_revBIll);
                    bitScroll = getBitmapFromView(scrollView, scrollView.getChildAt(0).getHeight(), scrollView.getChildAt(0).getWidth());

                    if(isWriteStoragePermissionGranted()) {
                        saveBitmap(bitScroll);
                        savePdf(true,Constants.path_PDF_FileDirectory);

                    }

                }else{
                    //  progress.dismiss();
                }
                break;
            case 4:
            case 5:

                if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    if(isAccessNetworkStatePermissionGranted())
                    {
                        ScrollView scrollView = findViewById(R.id.scroll_revBIll);
                        bitScroll = getBitmapFromView(scrollView, scrollView.getChildAt(0).getHeight(), scrollView.getChildAt(0).getWidth());

                        saveBitmap(bitScroll);
                        savePdf(true,Constants.h_PDF_FileDirectory);
                        SendEmail(email);
                    }
                }

                break;
        }
    }


    //permissions
    String TAG = "hundred_3";
    public  boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted1");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted1");
            return true;
        }
    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted2");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted2");
            return true;
        }
    }

    public  boolean isInternetPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.INTERNET)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted3");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked3");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 4);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted3");
            return true;
        }
    }

    public  boolean isAccessNetworkStatePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted4");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked4");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 5);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted4");
            return true;
        }
    }


    //create bitmap from the ScrollView
    private Bitmap getBitmapFromView(View view, int height, int width) {

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);

        return bitmap;
    }

    String now;

    public void saveBitmap(Bitmap bitmap) {



        now = Calendar.getInstance().getTimeInMillis()+"";


        File myDirectory = new File(Environment.getExternalStorageDirectory(), Constants.h_BMP_FileDirectory_sub);

        //File myDirectory = new File(getApplicationInfo().dataDir);

        if(!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

        String mPath = Constants.path_Image_FileDirectory+ "/"+ResName+"_"+Invoice +"_"+ now + ".jpeg";
        //String mPath = myDirectory + "/"+ResName+"_"+Invoice+"_"+ResId+ ".jpeg";
        File imagePath = new File(mPath);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // Toast.makeText(getApplicationContext(),imagePath.getAbsolutePath()+"", Toast.LENGTH_LONG).show();
            // Log.e("ImageSave", "Saveimage");
        } catch (FileNotFoundException e) {
            // Log.e("GREC", e.getMessage(), e);
        } catch (IOException e) {
            // Log.e("GREC", e.getMessage(), e);
        }
        finally {

        }
    }

    public void savePdf(boolean showToast,String dir)
    {
        File myDirectory = new File(Environment.getExternalStorageDirectory(), Constants.h_PDF_FileDirectory_sub);

        if(!myDirectory.exists())
        {
            myDirectory.mkdirs();
        }

        try {

            Document document = new Document();

            PdfWriter.getInstance(document, new FileOutputStream(dir +
                    ResName+"_"+Invoice+"_"+now+".pdf")); //  Change pdf's name.

            document.open();

            Image image = Image.getInstance(Constants.path_Image_FileDirectory +ResName+"_"+
                    Invoice+"_"+now+".jpeg");  // Change image's name and extension.

            float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                    - document.rightMargin() - 0) / image.getWidth()) * 100; // 0 means you have no indentation. If you have any, change it.
            image.scalePercent(scaler);
            image.setAlignment(Image.ALIGN_CENTER | Image.ALIGN_TOP);

            document.add(image);
            document.close();



     }
        catch(IOException e)
        {

        }
        catch(DocumentException e)
        {

        }
        finally {

            if(showToast) {

                final String msg = "Your bill is stored at /sdcard/Service Please/" + ResName + "_" + Invoice + "_" + now + ".pdf";
                final String fileName = ResName + "_" + Invoice + "_" + now + ".pdf";
               // Toast.makeText(this,  msg, Toast.LENGTH_SHORT).show();
                Snackbar s = Snackbar.make(findViewById(R.id.root_bill),  msg, Snackbar.LENGTH_LONG);
                s.setAction("View", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        OpenBill(fileName);
                    }
                })
                .setActionTextColor(getResources().getColor(R.color.holo_blue_dark));
                s.show();

            }
            if(loading_download!=null)loading_download.dismiss();
        }

    }

    private void OpenBill(String filename)
    {
        Uri selectedUri = Uri.parse(Constants.path_PDF_FileDirectory);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "resource/folder");

        if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
        {
            startActivity(intent);
        }
        else
        {
            Snackbar.make(findViewById(R.id.root_bill),"The required app is not found",Snackbar.LENGTH_SHORT).show();

        }
    }

    private void SendEmail(final String email)
    {


        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        Uri file = Uri.fromFile(new File(Constants.h_PDF_FileDirectory + ResName+"_"+Invoice+"_"+now+".pdf"));

        final StorageReference riversRef = storageRef.child("Bill/"+ResName+"_"+Invoice+"_"+now+".pdf");

        UploadTask task =riversRef.putFile(file);
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(loading_email!=null)loading_email.dismiss();
            }
        })
        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                WebView webView = new WebView(BillActivity.this);
                Log.d("hundred_yo","Email: "+email);
                webView.loadUrl(
                        "https://us-central1-serviceplease-25bf6.cloudfunctions.net/sendMail?"
                                +"dest="+email+"&"
                                +"Bill="+ResName+"_"+Invoice+"_"+now
                );


                if(loading_email!=null)loading_email.dismiss();
                Snackbar.make(findViewById(R.id.root_bill),  "Mail Sent", Snackbar.LENGTH_LONG).show();

            }
        });

    }


    private String ValidateEmail()
    {
        final BasicUserDataDao dao = ((SelectedItemsApp)getApplication()).getBasicUserDataSession().getBasicUserDataDao();
        List<BasicUserData> dataList = dao.queryBuilder().where(BasicUserDataDao.Properties.Id.eq(1L)).list();
        if(dataList.size()>0)
        {

            BasicUserData data = dataList.iterator().next();

            if (data.getEmail().equals("null")||TextUtils.isEmpty(data.getEmail()))
            {
                //ask user to set an email
                final Dialog dialog = new Dialog(BillActivity.this);
                dialog.setContentView(R.layout.dialog_get_email);
                dialog.setTitle("Email");
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                View vokBtn = dialog.findViewById(R.id.email_okbtn);
                final EditText vemail = dialog.findViewById(R.id.email_address);

                vokBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        email = vemail.getText().toString();
                        if (TextUtils.isEmpty(email))
                        {
                            Toast.makeText(BillActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            BasicUserData newdata = new BasicUserData(1L, email);
                            dao.deleteByKey(1L);
                            dao.insert(newdata);

                            if (isAccessNetworkStatePermissionGranted())
                                if (isInternetPermissionGranted()) {
                                    ScrollView scrollView = findViewById(R.id.scroll_revBIll);
                                    bitScroll = getBitmapFromView(scrollView, scrollView.getChildAt(0).getHeight(), scrollView.getChildAt(0).getWidth());

                                    saveBitmap(bitScroll);
                                    savePdf(false,Constants.h_PDF_FileDirectory);
                                    SendEmail(email);
                                }

                            dialog.dismiss();


                        }

                    }
                });

                dialog.show();
            }
            else
            {
                email = data.getEmail().trim();

                if (isAccessNetworkStatePermissionGranted())
                    if (isInternetPermissionGranted()) {
                        ScrollView scrollView = findViewById(R.id.scroll_revBIll);
                        bitScroll = getBitmapFromView(scrollView, scrollView.getChildAt(0).getHeight(), scrollView.getChildAt(0).getWidth());

                        saveBitmap(bitScroll);
                        savePdf(false,Constants.h_PDF_FileDirectory);
                        SendEmail(email);
                    }
            }



        }
        return email;
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
                    CallWaiter(ResId,Table,R.id.root_bill);
                }
                if(Option.equals("Change Email"))
                {
                    ChangeEmail(BillActivity.this);
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
        moreBtn.setVisibility(View.GONE);

        TextView Title = findViewById(R.id.appbar_title);
        Title.setText(title);

        ((ImageView)backBtn).setImageResource(R.drawable.ic_home_white);

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
                View visitMenu = v.findViewById(R.id.viewmenu);

                cart.setVisibility(View.GONE);
                alredyOrdered.setVisibility(View.GONE);

                changeMail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final BasicUserDataDao dao = ((SelectedItemsApp)getApplication()).getBasicUserDataSession().getBasicUserDataDao();

                        if(dao.loadAll().size()>0)
                        {
                            final BasicUserData d = dao.queryBuilder().where(BasicUserDataDao.Properties.Id.eq(1L)).list().iterator().next();

                            final Dialog dialog = new Dialog(BillActivity.this);
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
                                        Toast.makeText(BillActivity.this, "email can't be empty", Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        dao.deleteByKey(1L);
                                        dao.insert(new BasicUserData(1L, vemail.getText().toString()));
                                        Toast.makeText(BillActivity.this, "E-Mail changed", Toast.LENGTH_SHORT).show();
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

                visitMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FilesUtils.SendBasicInfo(BillActivity.this,MenuActivity.class,ResId+","+Table+","+0,Constants.res);
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
