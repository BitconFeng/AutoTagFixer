package mx.dev.franco.automusictagfixer.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Settings;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;

/**
 * Created by franco on 12/01/18.
 */

public final class DetectorRemovableMediaStorages extends BroadcastReceiver{
    private LocalBroadcastManager mLocalBroadcastManager;
    private Context mContext;

    public DetectorRemovableMediaStorages(Context context){
        mContext = context;
        mLocalBroadcastManager =  LocalBroadcastManager.getInstance(mContext);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("media","works");
        String action = intent.getAction();
        Toast toast = Toast.makeText(context, "", Toast.LENGTH_LONG);
        View view = toast.getView();
        TextView text = view.findViewById(android.R.id.message);
        text.setTextColor(ContextCompat.getColor(context, R.color.grey_900));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            text.setTextAppearance(R.style.CustomToast);
        }
        else {
            text.setTextAppearance(context,R.style.CustomToast);
        }
        view.setBackground(ContextCompat.getDrawable(context, R.drawable.background_custom_toast) );
        toast.setGravity(Gravity.CENTER, 0, 0);

        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            text.setText(context.getString(R.string.media_mounted));
        }
        else if(action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            text.setText(context.getString(R.string.media_unmounted));
            //Revoke permission to write to SD card and remove URI from shared preferences.
            if(Constants.URI_SD_CARD != null) {
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                context.getContentResolver().releasePersistableUriPermission(Constants.URI_SD_CARD, takeFlags);
                Constants.URI_SD_CARD = null;
                Settings.ENABLE_SD_CARD_ACCESS = false;
            }

            SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(Constants.URI_TREE);
            editor.apply();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor e  = preferences.edit();
            e.putBoolean("key_enable_sd_card_access",false);
            e.commit();
        }

        toast.show();
        //Reload number of storages availables
        StorageHelper.getInstance(context).getBasePaths().clear();
        StorageHelper.getInstance(context).detectStorages();

        boolean isServiceRunning = ServiceHelper.withContext(context).withService(FixerTrackService.CLASS_NAME).isServiceRunning();
        //if media has mounted or unmounted, stop service to avoid
        //inconsistency in data
        if(isServiceRunning){
            Intent intentStopService = new Intent();
            intentStopService.setAction(Constants.Actions.ACTION_STOP_SERVICE);
            intentStopService.putExtra(Constants.Actions.ACTION_STOP_SERVICE, Constants.StopsReasons.CANCEL_TASK);
            mLocalBroadcastManager.sendBroadcast(intent);
        }

    }
}
