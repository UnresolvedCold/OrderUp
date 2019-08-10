package com.schwifty.serviceplease;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FilesUtils {

    static final String FILE_PNG = "test_page.png";
    //static final String FILE_DOC = "What is PrintHand.doc";
    static final String FILE_DOC = "D_ND end_17-Apr-2019_02:42:17.pdf";
    static final String FILE_PDF = "What is PrintHand.pdf";

    static void extractFilesFromAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        File dir = getFilesDir(context);
        for (String filename : new String[]{FILE_PNG, FILE_DOC, FILE_PDF})
            extractFileFromAssets(assetManager, dir, filename);
    }

    private static void extractFileFromAssets(AssetManager assetManager, File dir, String filename) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                inputStream = assetManager.open(filename);
                File outFile = new File(dir, filename);
                if (outFile.exists())
                    outFile.delete();
                outFile.createNewFile();
                outputStream = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1)
                    outputStream.write(buffer, 0, read);
                outputStream.flush();
            } finally {
                if (inputStream != null)
                    inputStream.close();
                if (outputStream != null)
                    outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String getFilePath(Context context, String filename) {
        File file = new File(getFilesDir(context), filename);
        return file.exists() ? file.getAbsolutePath() : null;
    }

    private static File getFilesDir(Context context) {
        return context.getExternalCacheDir();
    }

    //result = comma separated values
    //ref for mall = "Mall=MallId"
    public static void SendBasicInfo(Activity activity,Class c,String result,String ref)
    {
        Dialog t =UtilFunctions.ShowLoadingBar(activity);

        Constants.res = ref;

        Intent intent = new Intent (activity,c);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

        for(int i = 0;i< Constants.name.length;i++)
        {
            intent.putExtra(Constants.name[i],result.split(",")[i]);
        }
        //intent.putExtra("menuRef",menuRef_from_root);

        if(t!=null)t.dismiss();

        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }


}