package com.schwifty.serviceplease;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.schwifty.serviceplease.Database_ORM.BasicUserData;
import com.schwifty.serviceplease.Database_ORM.BasicUserDataDao;
import com.schwifty.serviceplease.Database_ORM.BasicUserData_DBHelper;
import com.schwifty.serviceplease.Database_ORM.ItemsDao;
import com.schwifty.serviceplease.Database_ORM.SelectedItemsApp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

public class UtilFunctions
{

    public static View ViewInflater(LayoutInflater inflater, LinearLayout ViewParentElement, int R_Layout_Template)
    {
        View mView = inflater.inflate(R_Layout_Template, null, false);
        ViewParentElement.addView(mView);
        return mView;
    }

    public static String GetDateAndTimeFromMillis(long timeInMillis)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM,dd,yyyy,HH,mm");
        Date resultdate = new Date(timeInMillis);
        return  sdf.format(resultdate);
    }

    public static Dialog ShowLoadingBar(Activity thisActivity)
    {
        if(!thisActivity.isFinishing()) {
            final Dialog dialog = new Dialog(thisActivity);
            dialog.setContentView(R.layout.dialog_loadingbar);
            dialog.setTitle("Enter the passcode");
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(false);

            dialog.show();
            return dialog;
        }

        return null;
    }

    //use id as 101,102...
    public static TourGuide showGuide(long ID,
                                      View view, Activity activity,
                                      String title, String description, String color, int gravity,
                                      BasicUserDataDao dao,TourGuide.Technique TourGuide_Technique)
    {
        //sql
        if(dao.loadAll().size()<=0)
        {
            BasicUserData b = new BasicUserData(1L,"null");
            dao.insert(b);
        }
/*
       if(dao.queryBuilder().where(BasicUserDataDao.Properties.Id.eq(ID)).list().size()<=0)
        {
            //Do it on first run
            TourGuide mTourGuideHandler = TourGuide.init(activity).with(TourGuide_Technique)
                    .setPointer(new Pointer())
                    .setToolTip(
                            new ToolTip()
                                    .setTitle(title)
                                    .setDescription(description)
                                    .setBackgroundColor(Color.parseColor(color))
                                    .setShadow(true)
                                    .setGravity(gravity)
                    )
                    .setOverlay(new Overlay())
                    .playOn(view);

            BasicUserData b = new BasicUserData(ID,"null");
            dao.insert(b);

            return mTourGuideHandler;
        }
        else
        {*/
            return null;
        //}

    }

    public static void checkValidity(final Activity activity , final String resId, final String Table, final boolean trigger)
    {

        if(Constants.Type.equals("Rest"))
        {
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (task.isSuccessful()) {
                                final String DeviceId = task.getResult().getToken();

                                DatabaseReference r = FirebaseDatabase.getInstance().getReference().child(Constants.OccupiedMembers).child(resId).child(Table);


                                r.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (!(dataSnapshot.exists() && dataSnapshot.getValue().toString().contains(DeviceId))) {

                                            if (trigger) {
                                                Intent i = new Intent(activity, Launcher.class);
                                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                                                ItemsDao dao = ((SelectedItemsApp) activity.getApplication()).getSelectedItemsSession().getItemsDao();
                                                dao.deleteAll();

                                                activity.startActivity(i);
                                            }
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

    }



    // slide the view from below itself to the current position
    public static void slideUp(View view){

    }

    // slide the view from its current position to below itself
    public static void animateHeight(final View view, int initialHeight, int finalHeight, int Duration, final int finalVisibility){
        //make it visible
        view.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams l = view.getLayoutParams();
        l.height = initialHeight;
        view.setLayoutParams(l);

        Log.d("hundred_yo","initial: "+initialHeight+" final: "+finalHeight);
        ValueAnimator anim = ValueAnimator.ofInt(initialHeight,finalHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.height = val;
                view.setLayoutParams(layoutParams);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setVisibility(finalVisibility);
            }
        });
        anim.setDuration(Duration);
        anim.start();


    }



}
