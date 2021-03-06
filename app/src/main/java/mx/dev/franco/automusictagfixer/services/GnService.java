package mx.dev.franco.automusictagfixer.services;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnStorageSqlite;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.Constants;

/**
 * Created by franco on 5/07/17.
 */

public class GnService{
    private static int sCountIntents = 0;
    private static final java.lang.String TAG = GnService.class.getName();
    //We can set a Context in static field while we call getApplicationContext() to avoid memory leaks, because
    //if we use the activity Context, this activity can remain in memory due is still in use its Context
    private static Context sContext;
    private static volatile GnService sGnService;
    private static volatile GnManager sGnManager;
    public static volatile GnUser sGnUser;
    private static volatile GnLocale sGnLocale;

    /*Production account*/
    public static final String sGnsdkLicenseString =
    "-- BEGIN LICENSE v1.0 0B64261A --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 297011532\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE 0B64261A --\\r\\nlAADAgAeLXKgVttVCmTzGU8Lixv2VY0nKZECLARnmGWPPmpdAB8Bgp+dp5HRX8tQLJh1OvmV1ipXLqr6oy6Ds3ClSOE8\\r\\n-- END LICENSE 0B64261A --\\r\\n";   //"-- BEGIN LICENSE v1.0 A75228BC --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 843162123\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE A75228BC --\\r\\nlAADAgAe/WEZPZ5IaetmxgKEpZm7EjG1SLm/yLvyhTwzlr8cAB4R2GcEuN/6PovFycqgCmnnmr3ioB/KXt3EDTz8yYk=\\r\\n-- END LICENSE A75228BC --\\r\\n";
    public static final String sGnsdkClientId = "297011532";//"843162123";
    public static final String sGnsdkClientTag = "6CB01DB21FA7F47FDBF1FD6DCDFA8E88";
    public static final String sAppString = "AutomaticMusicTagFixer";
    /***************************/

    /*public static final String sGnsdkLicenseString =
            "-- BEGIN LICENSE v1.0 CC3F43CA --\r\nname: \r\nnotes: Gracenote Open Developer Program\r\nstart_date: 0000-00-00\r\nclient_id: 1061926482\r\nmusicid_file: enabled\r\nmusicid_text: enabled\r\nmusicid_stream: enabled\r\nmusicid_cd: enabled\r\nplaylist: enabled\r\nvideoid: enabled\r\nvideo_explore: enabled\r\nlocal_images: enabled\r\nlocal_mood: enabled\r\nvideoid_explore: enabled\r\nacr: enabled\r\nepg: enabled\r\n-- SIGNATURE CC3F43CA --\r\nlAADAgAerPlBmNtnUv1xMQHyMSC8ENU3ZAAnQk3fTGGFe+ckAB8BTp1hquoRKBDJwnIBnJA49foo6jf6RjZIe1/aiatn\r\n-- END LICENSE CC3F43CA --\r\n";
    public static final String sGnsdkClientId = "1061926482";
    public static final String sGnsdkClientTag = "763A3CB0E4D95E9EA3C2EAB9B10ADE11";
    public static final String sAppString = "TestingApp";*/


    public static volatile boolean sApiInitialized = false;
    public static volatile boolean sIsInitializing = false;
    public static final int API_INITIALIZED_FROM_SPLASH = 100;
    public static final int API_INITIALIZED_AFTER_CONNECTED = 101;
    private static AsyncApiInitialization sAsyncApiInitialization;

    /**
     * We don't need instances of this class
     */
    private GnService(Context context){
        if(sContext == null) {
            sContext = context.getApplicationContext();
        }
    }


    /**
     * Set sContext for use in GNSDK methods
     * @param appContext
     */
    public static GnService withContext(Context appContext){
        if(sGnService == null){
            sGnService = new GnService(appContext);
        }
        return sGnService;
    }

    /**
     * This method initializes the API
     */
    public void initializeAPI(final int connectedFrom){
        if(sAsyncApiInitialization == null && !sApiInitialized){
            Log.d("sApiInitialized","initializing api");
            sAsyncApiInitialization = new AsyncApiInitialization(connectedFrom);
            sAsyncApiInitialization.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, connectedFrom);
        }
    }


    public static class AsyncApiInitialization extends AsyncTask<Integer,Void,Boolean> {
        private int mConnectedFrom = API_INITIALIZED_FROM_SPLASH;
        private String mMsg = "";

        public AsyncApiInitialization(int connectedFrom){
            mConnectedFrom = connectedFrom;
        }
        @Override
        protected Boolean doInBackground(Integer... code) {
            //We initialize the necessary objects for using the GNSDK API in a different thread for not blocking the UI
            try {
                sGnManager = new GnManager(sContext, sGnsdkLicenseString, GnLicenseInputMode.kLicenseInputModeString);
                sGnUser = new GnUser(new GnUserStore(sContext), sGnsdkClientId, sGnsdkClientTag, sAppString);
                sGnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic, GnLanguage.kLanguageSpanish, GnRegion.kRegionGlobal, GnDescriptor.kDescriptorDetailed, sGnUser);
                sGnLocale.setGroupDefault();
                GnStorageSqlite.enable();
                mConnectedFrom = code[0];
                //When api could not be initialized since SplashActivity,
                //inform to user after MainActivity starts

                return sApiInitialized = true;
            } catch (GnException e) {
                e.printStackTrace();
                mMsg = e.getMessage();
                //If could not be established the initialization of Gracenote API, try again
                return sApiInitialized = sIsInitializing = false;
            }

        }

        @Override
        protected void onPostExecute(Boolean res){
            Log.d("res",res+" " + (mConnectedFrom == API_INITIALIZED_AFTER_CONNECTED));
            //Schedule initialization if was not possible this time
            if(!res){
                //when number of intents is 5
                //inform the user that API could not be initialized and the cause
                if(sCountIntents == 5){
                    Toast toast = Toast.makeText(sContext, sContext.getString(R.string.could_not_init_api) + mMsg, Toast.LENGTH_LONG);
                    View view = toast.getView();
                    TextView text = (TextView) view.findViewById(android.R.id.message);
                    text.setTextColor(ContextCompat.getColor(sContext, R.color.grey_900));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        text.setTextAppearance(R.style.CustomToast);
                    }
                    else {
                        text.setTextAppearance(sContext,R.style.CustomToast);
                    }
                    view.setBackground(ContextCompat.getDrawable(sContext, R.drawable.background_custom_toast) );
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    sCountIntents = 0;
                    //Stop trying to initialize API
                    JobScheduler jobScheduler = (JobScheduler) sContext.getApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    jobScheduler.cancelAll();
                    JobManager.withContext(sContext).cancelAllJobs();
                }
                else {
                    sCountIntents++;
                }
            }
            //Notify to user
            else {
                JobManager.withContext(sContext).cancelAllJobs();
                Intent intent = new Intent();
                intent.setAction(Constants.GnServiceActions.ACTION_API_INITIALIZED);
                LocalBroadcastManager.getInstance(sContext).sendBroadcastSync(intent);
            }

            sAsyncApiInitialization = null;
        }

    }
}
