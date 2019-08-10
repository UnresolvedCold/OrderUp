package com.schwifty.serviceplease;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aevi.print.PrinterApi;
import com.aevi.print.PrinterManager;
import com.aevi.print.model.PrintJob;
import com.aevi.print.model.PrintPayload;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.schwifty.serviceplease.Database_ORM.Items;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;
import com.schwifty.serviceplease.Mall.Mall_ShopListActivity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import io.reactivex.functions.Consumer;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

//import com.schwifty.qrcodescan.QrCodeActivity;

/**
 *
 * Queries
 *
 * 1. What if people start taking images of QR at table to book their seat in advance
 * 2. Would asking for location be good to check if they are in the restaurant?
 *
 *
 * Data Structure
 *      Menu - <Restaurant>/<MenuGroup>/<MenuItem>
 *      Restaurant Info - <Restaurant>/<info>
 *
 * What to do next
 *      MenuActivity --> ShowMenu (Wait for design)
 *
 */

public class MainActivity extends AppCompatActivity {

    //static variables
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 201;
    private static final int MY_PERMISSIONS_REQUEST_VIBRATE = 202;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 203;


    TourGuide mTourguide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.ScanQRCode)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        ScanQRCode();

                        if (mTourguide != null) {
                            mTourguide.cleanUp();
                        }

                        //SendDataToMenuActivity("resid1,5,0");
                        //SendDataToMallShops("Mall=MallUID");
                    }
                });

        findViewById(R.id.ViewHistory)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ShowHistory();
                    }
                });

        mTourguide = UtilFunctions.showGuide(101L,findViewById(R.id.ScanQRCode),
                this, "Click 'Scan' to get started", "Scan the QR code of your table",
                "#e54d26", Gravity.BOTTOM,  ((SelectedItemsApp) getApplication()).getBasicUserDataSession().getBasicUserDataDao(),
                TourGuide.Technique.Click);


    }

    private void ShowHistory()
    {
        SyncHistory();
    }

    private void SyncHistory()
    {

        final Dialog d = UtilFunctions.ShowLoadingBar(this);

        final DatabaseReference r = FirebaseDatabase.getInstance().getReference().child(Constants.PendingSync);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (task.isSuccessful())
                        {
                            final String deviceToken = task.getResult().getToken().toString();
                            Log.d( "hundred_99","Device Id = "+deviceToken);

                            r.child(deviceToken).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                {
                                    if(dataSnapshot.exists())
                                    {
                                        for(DataSnapshot snapshot:dataSnapshot.getChildren())
                                        {
                                            String invoice = snapshot.child("Invoice").getValue().toString();
                                            String resId = snapshot.child("ResId").getValue().toString();
                                            String table = snapshot.child("Table").getValue().toString();
                                            Log.d("hundred_99",invoice);
                                            //insert the values in history
                                            UpdateHistoryDao(resId,table,invoice,r.child(deviceToken).child(snapshot.getKey().toString()));

                                        }
                                    }

                                    Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                    if(d!=null) d.dismiss();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError)
                                {
                                    if(d!=null) d.dismiss();
                                }
                            });

                        }
                    }
                });


    }

    private void UpdateHistoryDao(final String resId, final String table, final String invoice, final DatabaseReference k)
    {

        Log.d("hundred_99","Update started");

        final Dialog d =UtilFunctions.ShowLoadingBar(this);
        final ItemsDao  historyDao = ((SelectedItemsApp) getApplication()).getHistorySession().getItemsDao();

        DatabaseReference r = FirebaseDatabase.getInstance().getReference().child(Constants.OrderHistoryRef).child(resId);

        r.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot snapshot:dataSnapshot.getChildren())
                {
                    if(snapshot.exists())
                        if(snapshot.hasChild("Invoice")&&snapshot.child("Invoice").getValue().toString().equals(invoice))
                        {
                           for(DataSnapshot itemSnapshot:snapshot.child("Items").getChildren())
                           {
                                String item,uid,isVeg,price,qty;
                                item = itemSnapshot.child("Item").getValue().toString();
                                uid = itemSnapshot.child("UID").getValue().toString();
                                isVeg = itemSnapshot.child("isVeg").getValue().toString();
                                price = itemSnapshot.child("price").getValue().toString();
                                qty = itemSnapshot.child("qty").getValue().toString();
                                Log.d("hundred_99","Update : Item"+item);

                               final Items i = new Items(System.currentTimeMillis(),item,"true",Integer.parseInt(qty),resId,"true",price,table,isVeg,uid,"","","","","");

                               historyDao.insert(i);

                           }

                            FirebaseDatabase.getInstance().getReference().child(Constants.RestaurantDetails).child(resId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String ResName = dataSnapshot.child("ResName").getValue().toString();
                                    historyDao.insert(new Items(System.currentTimeMillis(),"Schwifty","true",100,ResName+","+resId,"true",invoice,table,"false","UID","","","","",""));
                                    if(d!=null)d.dismiss();
                                    k.removeValue();
                                    Log.d("hundred_99","Update remove");

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    if(d!=null)d.dismiss();
                                    k.removeValue();
                                }
                            });
                                                                                                                                                 }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void ScanQRCode() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.VIBRATE},
                    MY_PERMISSIONS_REQUEST_VIBRATE);
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {

            Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
            startActivityForResult(i, REQUEST_CODE_QR_SCAN);
        }
    }

    private void GetQRInfo(String result) {
        //Comma separated Values
        //RUID : Restaurant UID(Generated at the time of registration)
        //Table : The table number (arb assigned as per need)
        //Chair : Chair Number (arb assigned as per need)
        //QR Result = RUID,Table,Chair
        //Load other values like website and contacts after these 3 values

        //Init the values


        if (result.contains(","))
        {
            if(result.split(",").length>=3) {


                SendDataToMenuActivity(result);

            }
        }
        else
        {
            if(result.contains("Mall")&&result.contains("="))
            {

                SendDataToMallShops(result);
            }
            else
            {
                Toast.makeText(this, "Scanned QR Code is invalid", Toast.LENGTH_SHORT).show();
            }

        }


    }

    private void SendDataToMenuActivity(final String result) {

        final Dialog loader = UtilFunctions.ShowLoadingBar(MainActivity.this);

        Constants.LoadRestModule(result.split(",")[0]);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task)
                    {
                        if (task.isSuccessful())
                        {
                            final String DeviceId = task.getResult().getToken();

                            FirebaseDatabase.getInstance().getReference().child(Constants.OccupiedMembers)
                                    .child(result.split(",")[0]).child(result.split(",")[1])
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                        {
                                            if(dataSnapshot.exists()) {
                                                if (dataSnapshot.getValue().toString().contains(DeviceId)) {
                                                    FilesUtils.SendBasicInfo(MainActivity.this, MenuActivity.class, result,Constants.res);
                                                }
                                                else
                                                {
                                                    FirebaseDatabase.getInstance().getReference().child("CallWaiter")
                                                            .child(result.split(",")[0])
                                                            .child(result.split(",")[1])
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                    if(dataSnapshot.exists()&&dataSnapshot.getValue().toString().contains("Pay"))
                                                                    {
                                                                        Snackbar.make(findViewById(R.id.root_main), "Table is not ready", Snackbar.LENGTH_LONG).show();

                                                                    }
                                                                    else
                                                                    {
                                                                        PassCodeVerification(result);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                }
                                                            });

                                                }
                                            }
                                            else
                                            {
                                                FirebaseDatabase.getInstance().getReference().child("CallWaiter")
                                                        .child(result.split(",")[0])
                                                        .child(result.split(",")[1])
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                if(dataSnapshot.exists()&&dataSnapshot.getValue().toString().contains("Pay"))
                                                                {
                                                                    Snackbar.make(findViewById(R.id.root_main), "Table is not ready", Snackbar.LENGTH_LONG).show();

                                                                }
                                                                else
                                                                {
                                                                    PassCodeVerification(result);
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });
                                            }

                                            if(loader!=null)loader.dismiss();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            if(loader!=null)loader.dismiss();
                                        }
                                    });


                        }
                    }
                });



    }

    private void SendDataToMallShops(final String result)
    {

        final Dialog loader = UtilFunctions.ShowLoadingBar(MainActivity.this);
        FirebaseDatabase.getInstance().getReference().child(result.split("=")[0])
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(result.split("=")[1]))
                        {
                            //send to view rest
                            Intent intent = new Intent(MainActivity.this, Mall_ShopListActivity.class);
                            intent.putExtra("MallId",result.split("=")[1]);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                        }
                        else
                        {
                            Toast.makeText(MainActivity.this, "Scanned QR Code is invalid", Toast.LENGTH_SHORT).show();
                        }
                        if(loader!=null)loader.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if(loader!=null)loader.dismiss();
                    }
                });
    }

    private void PassCodeVerification(final String result)
    {
        //Dialog to enter PassCode
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_passcode);
        dialog.setTitle("Enter the passcode");
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);

        // set the custom dialog components - text, image and button
        final TextView vPasscode = dialog.findViewById(R.id.passcode);
        View vPAsscodeOK = dialog.findViewById(R.id.passcode_ok);

        vPAsscodeOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                final String yourcode = vPasscode.getText().toString();

                if (TextUtils.isEmpty(yourcode)) {
                    Toast.makeText(MainActivity.this, "Passcode is empty", Toast.LENGTH_SHORT).show();
                } else if (yourcode.trim().length() != 3) {
                    Toast.makeText(MainActivity.this, "Wrong passcode", Toast.LENGTH_SHORT).show();
                } else {

                    final Dialog d = UtilFunctions.ShowLoadingBar(MainActivity.this);
                    FirebaseDatabase.getInstance().getReference().child(Constants.RestaurantDetails).child(result.split(",")[0].trim())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (dataSnapshot.child("PassCode").exists()) {

                                        if (yourcode.trim().equals(dataSnapshot.child("PassCode").getValue().toString())) {

                                            final DatabaseReference dr = FirebaseDatabase.getInstance().getReference().child(Constants.CallWaiter)
                                                    .child(result.split(",")[0].trim())
                                                    .child(result.split(",")[1].trim());

                                            dr.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                     // Notify Dining app that sear is occupied
                                                    if(dataSnapshot.exists())
                                                    {
                                                        if (!(dataSnapshot.getValue().toString().contains("SeatOccupied")))
                                                        {
                                                            String uid = dr.push().getKey().toString();
                                                            dr.child(uid).child("Purpose").setValue("SeatOccupied");
                                                        }
                                                    }
                                                    else
                                                    {
                                                        String uid = dr.push().getKey().toString();
                                                        dr.child(uid).child("Purpose").setValue("SeatOccupied");
                                                    }

                                                    // Update members
                                                    FirebaseInstanceId.getInstance().getInstanceId()
                                                            .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                                                    if (task.isSuccessful()) {
                                                                        String DeviceId = task.getResult().getToken();
                                                                        DatabaseReference occupiedSeat = FirebaseDatabase.getInstance().getReference()
                                                                                .child(Constants.OccupiedMembers)
                                                                                .child(result.split(",")[0])
                                                                                .child(result.split(",")[1]);

                                                                        String key = occupiedSeat.push().getKey().toString();

                                                                        occupiedSeat.child(key).child("DeviceId").setValue(DeviceId);

                                                                        FilesUtils.SendBasicInfo(MainActivity.this, MenuActivity.class, result,Constants.res);
                                                                    }
                                                                }
                                                            });



                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });


                                        } else {
                                            //Toast.makeText(MainActivity.this, "Wrong passcode", Toast.LENGTH_SHORT).show();
                                            Snackbar.make(findViewById(R.id.root_main), "Wrong Pass Code", Snackbar.LENGTH_LONG).show();
                                            dialog.dismiss();

                                        }
                                        d.dismiss();
                                    }
                                    else
                                    {
                                        //Toast.makeText(MainActivity.this, "No Resturant found", Toast.LENGTH_SHORT).show();
                                        Snackbar.make(findViewById(R.id.root_main), "Scanned QR COde is Wrong", Snackbar.LENGTH_LONG).show();
                                        dialog.dismiss();
                                        d.dismiss();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    d.dismiss();
                                }
                            });
                }


            }
        });

        dialog.show();
    }

    private void PermissionsWereDenied() {
        Toast.makeText(this, "Camera is required for scanning QR code", Toast.LENGTH_SHORT).show();
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if(resultCode != Activity.RESULT_OK)
        {
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if( result!=null)
            {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Scan Error");
                alertDialog.setMessage("QR Code could not be scanned");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            return;

        }


        if(requestCode == REQUEST_CODE_QR_SCAN)
        {
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");

            GetQRInfo(result);

        }
    }

    @Override
    public void onRequestPermissionsResult(
           int requestCode,
           String permissions[],
           int[] grantResults
    )
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                {
                        Log.i("Camera", "G : " + grantResults[0]);
                        // If request is cancelled, the result arrays are empty.
                        if
                        (
                            grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        )
                        {
                            // permission was granted,

                            ScanQRCode();
                        }

                         else
                         {
                            // permission denied, Disable the
                            // functionality that depends on this permission.

                            if (ActivityCompat.shouldShowRequestPermissionRationale
                                    (this, Manifest.permission.CAMERA)) {

                                PermissionsWereDenied();

                            }
                            else
                            {

                            }
                        }
                        break;
                 }
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
            {
                // If request is cancelled, the result arrays are empty.
                if
                (
                        grantResults.length > 0
                                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                )
                {
                    // permission was granted,

                    ScanQRCode();
                }

                else
                {
                    // permission denied, Disable the
                    // functionality that depends on this permission.

                    if (ActivityCompat.shouldShowRequestPermissionRationale
                            (this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                        PermissionsWereDenied();

                    }
                    else
                    {

                    }
                }
                break;
            }

            case MY_PERMISSIONS_REQUEST_VIBRATE:
            {
                // If request is cancelled, the result arrays are empty.
                if
                (
                        grantResults.length > 0
                                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                )
                {
                    // permission was granted,

                    ScanQRCode();
                }

                else
                {
                    // permission denied, Disable the
                    // functionality that depends on this permission.

                    if (ActivityCompat.shouldShowRequestPermissionRationale
                            (this, Manifest.permission.VIBRATE)) {

                        PermissionsWereDenied();

                    }
                    else
                    {

                    }
                }
                break;
            }

    }

}

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public class MyPrintDocumentAdapter extends PrintDocumentAdapter
    {
        Context context;

        private int pageHeight;
        private int pageWidth;
        public PdfDocument myPdfDocument;
        public int totalpages = 1;

        public MyPrintDocumentAdapter(Context context)
        {
            this.context = context;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes,
                             PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal,
                             LayoutResultCallback callback,
                             Bundle metadata) {

            myPdfDocument = new PrintedPdfDocument(context, newAttributes);

            pageHeight =
                    newAttributes.getMediaSize().getHeightMils()/1000 * 72;
            pageWidth =
                    newAttributes.getMediaSize().getWidthMils()/1000 * 72;

            if (cancellationSignal.isCanceled() ) {
                callback.onLayoutCancelled();
                return;
            }

            if (totalpages > 0) {
                PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                        .Builder("print_output.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(totalpages);

                PrintDocumentInfo info = builder.build();
                callback.onLayoutFinished(info, true);
            } else {
                callback.onLayoutFailed("Page count is zero.");
            }

        }


        @Override
        public void onWrite(final PageRange[] pageRanges,
                            final ParcelFileDescriptor destination,
                            final CancellationSignal cancellationSignal,
                            final WriteResultCallback callback) {


            for (int i = 0; i < totalpages; i++) {
                if (pageInRange(pageRanges, i))
                {
                    PdfDocument.PageInfo newPage = new PdfDocument.PageInfo.Builder(pageWidth,
                            pageHeight, i).create();

                    PdfDocument.Page page =
                            myPdfDocument.startPage(newPage);

                    if (cancellationSignal.isCanceled()) {
                        callback.onWriteCancelled();
                        myPdfDocument.close();
                        myPdfDocument = null;
                        return;
                    }
                    drawPage(page, i);
                    myPdfDocument.finishPage(page);
                }
            }

            try {
                myPdfDocument.writeTo(new FileOutputStream(
                        destination.getFileDescriptor()));
            } catch (IOException e) {
                callback.onWriteFailed(e.toString());
                return;
            } finally {
                myPdfDocument.close();
                myPdfDocument = null;
            }

            callback.onWriteFinished(pageRanges);



        }


        private void drawPage(PdfDocument.Page page,
                              int pagenumber) {
            Canvas canvas = page.getCanvas();

            pagenumber++; // Make sure page numbers start at 1

            int titleBaseLine = 72;
            int leftMargin = 54;

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(40);
            canvas.drawText(
                   // "Test Print Document Page " + pagenumber,
                    " ",
                    leftMargin,
                    titleBaseLine,
                    paint);

            paint.setTextSize(14);
           // canvas.drawText("This is some test content to verify that custom document printing works", leftMargin, titleBaseLine + 35, paint);

            if (pagenumber % 2 == 0)
                paint.setColor(Color.RED);
            else
                paint.setColor(Color.GREEN);

            PdfDocument.PageInfo pageInfo = page.getInfo();


            canvas.drawCircle(pageInfo.getPageWidth()/2,
                    pageInfo.getPageHeight()/2,
                    150,
                    paint);
        }


        private boolean pageInRange(PageRange[] pageRanges, int page)
        {
            for (int i = 0; i<pageRanges.length; i++)
            {
                if ((page >= pageRanges[i].getStart()) &&
                        (page <= pageRanges[i].getEnd()))
                    return true;
            }
            return false;
        }


    }


}
