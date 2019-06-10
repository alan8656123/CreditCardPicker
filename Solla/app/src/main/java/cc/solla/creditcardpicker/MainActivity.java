package cc.solla.creditcardpicker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static cc.solla.creditcardpicker.StaticFunction.*;
//https://pixabay.com/illustrations/credit-card-icon-financial-2389154/

//https://script.google.com/macros/s/AKfycbzDfBPsI7Y2NUn7TuZ1Z7p9aZ7rz8KD9vKv1t3UY4pdsRCy3D5k/exec?type=0&p3=42.1&p2=121.3&p1=ABC&p0=12323
public class MainActivity extends AppCompatActivity
{
    private final int timeIntervalMills = 5 * 60 * 1000;
    private PlacesClient placesClient;
    private AlertDialog.Builder placeDialog = null, phoneStateDialog = null;
    private boolean isInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String apiKey = getString(R.string.places_api_key);
        InitSharedPreferences(this);
        initCardList();

        if (!isAgreeUserPolicy())   //Check is agree user policy
            return; //Return when first time. (Disagree will lead to kill application)
        else if (!checkPermissionDialog())
            return; //Return when permission is not request
        try
        {
            if (checkInternet())    //Check Internet Connection
                checkAndFetchDatabase(this);
            else
                throw new Exception("No Internet Connection!");

            if (!(getResources().getConfiguration().locale.getCountry().equalsIgnoreCase("tw")))
                throw new Exception(getString(R.string.LocaleNotZHTW));
            if (!isLocationAvailable())
                throw new Exception(getString(R.string.Location_Not_Available));
            if (!isTaiwanLocation())
                throw new Exception(getString(R.string.Not_In_Taiwan));
            if (!timeIntervalCheck())
                throw new Exception(getString(R.string.FiveMinPerQueryError));
            if (apiKey.equals(""))
                throw new Exception(getString(R.string.Lost_API_Key));
            if (cardCountCheck() && !Places.isInitialized())    //If has more than one card && api not Initialized
                Places.initialize(getApplicationContext(), apiKey);
            else
                return;

            isInit = true;
        }catch (Exception e)
        {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    .show();
        }
        isRegisterUserID();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.StartScan)
        {
                if (placeList != null)   //If List is not empty (Scanned)
                    return true;

                if (!isInit)
                {
                    Toast.makeText(this, getString(R.string.ScanNearbyFailHint), Toast.LENGTH_LONG).show();
                    return true;
                }
                if (!isDatabaseReady())
                {
                    Toast.makeText(this, getString(R.string.DatabaseNotReadyHint), Toast.LENGTH_SHORT)
                            .show();
                    return true;

                }
                if (!cardCountCheck())
                    return true;
                if (!Places.isInitialized())
                    reloadActivity();
                ((TextView)findViewById(R.id.textView_ScanHint)).setText(R.string.WaitingForAPI);
                placesClient = Places.createClient(this);
                findCurrentPlace();
                return true;
        }

        boolean returnValue = MenuMainHandler(this, item);
        if (!returnValue)
            returnValue = super.onOptionsItemSelected(item);
        return returnValue;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (phoneStateDialog != null || placeDialog != null)
            return;
        reloadActivity();
    }

    public void onClickNextStep(View view)
    {
        Button button = (Button)view;
        int index = (int)button.getTag();
        Intent intent = new Intent(this, CardResultActivity.class);
        intent.putExtra("Index", index);
        startActivity(intent);
    }

    ////////////////////////////////////////////////////////////////

    private boolean isRegisterUserID()
    {
        if (!checkInternet())
            return false;
        sendOpenAppRecord();    //Record App Open
        if (userInfo.getBoolean("isRegisterUserID", false))
            return true;
        new Thread(()->{
            try
            {
                URL urlPath = new URL("https://script.google.com/macros/s/AKfycbzDfBPsI7Y2NUn7TuZ1Z7p9aZ7rz8KD9vKv1t3UY4pdsRCy3D5k/exec?type=0&p0=" + String.valueOf(userID));
                HttpsURLConnection https = (HttpsURLConnection)urlPath.openConnection();//設定超時間為3秒
                https.setConnectTimeout(3*1000);
                //防止遮蔽程式抓取而返回403錯誤
                https.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
                https.getResponseCode();
            }catch (Exception e)
            {
                return;
            }
            userInfo
                    .edit()
                    .putBoolean("isRegisterUserID", true)
                    .commit();
        }).start();
        return true;
    }
    private boolean isAgreeUserPolicy()
    {
        if (userInfo.getBoolean("AgreePolicy", false))
            return true;
        if (userID > 0)
            return true;
        if (!userInfo.getBoolean("IsFirstTime", true))
            android.os.Process.killProcess(android.os.Process.myPid());
        new AlertDialog.Builder(this)
                .setTitle("使用須知")
                .setMessage(R.string.UserPolicy)
                .setPositiveButton("同意", (dialog,  which) ->
                        {
                            Random random = new Random();
                            while (userID == 0)
                                userID = random.nextLong();
                            userInfo.edit()
                                    .putBoolean("IsFirstTime", false)
                                    .putBoolean("AgreePolicy", true)
                                    .putLong("userID", userID)
                                    .apply();
                            reloadActivity();
                        }
                )
                .setNegativeButton("不同意", (dialog,  which) ->
                        {
                            userInfo.edit()
                                    .putBoolean("IsFirstTime", false)
                                    .putBoolean("AgreePolicy", false)
                                    .commit();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        })
                .show();

        return false;
    }

    private boolean cardCountCheck()
    {
        return StaticFunction.cardCountCheck(this);
    }

    /**
     * isGPSEnabled: Get Location by GPS
     * isNetworkEnabled: Get Location by 3G/4G/WiFi Station
     * @return Is Location Available
     */
    private boolean isLocationAvailable()
    {
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return isGPSEnabled | isNetworkEnabled;
    }

    /**
     * Try to get Location with Celling Network
     * @return true if in Taiwan
     */
    private boolean isTaiwanLocation()
    {
        if (!checkPermission(READ_PHONE_STATE))
        {
            Toast.makeText(this, getString(R.string.OnPhoneStatePermissionRefused), Toast.LENGTH_LONG).show();
            return false;
        }

        TelephonyManager telManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager == null)
            return true;
        String countryIso = telManager.getNetworkCountryIso();
        if (countryIso == null)
            return true;
        return countryIso.equalsIgnoreCase("tw");
    }

    /**
     * Check time interval between two success access API
     * @return is time interval large enough
     */
    private boolean timeIntervalCheck()
    {
        if (BuildConfig.DEBUG)
            return true;
        long LastTime = userInfo.getLong("LastTime", 0);
        long NowTime = System.currentTimeMillis();
        return (NowTime - LastTime > timeIntervalMills); //timeIntervalMills per query
    }

    /**
     * Fetches a list of {@link PlaceLikelihood} instances that represent the Places the user is
     * most
     * likely to be at currently.
     */
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private void findCurrentPlaceWithPermissions()
    {
        List<Place.Field> placeFields = new ArrayList<>();
        placeFields.add(Place.Field.NAME);
        placeFields.add(Place.Field.TYPES);
        placeFields.add(Place.Field.LAT_LNG);
        placeFields.add(Place.Field.ADDRESS);

        FindCurrentPlaceRequest currentPlaceRequest =
                FindCurrentPlaceRequest.newInstance(placeFields);
        Task<FindCurrentPlaceResponse> currentPlaceTask =
                placesClient.findCurrentPlace(currentPlaceRequest);

        currentPlaceTask.addOnSuccessListener(
                (response) ->
                {
                    placeList = response.getPlaceLikelihoods();
                    int length = placeList.size();
                    for (int i = 0; i < length; ++i)
                        addNewPlaceToScrollView(placeList.get(i).getPlace().getName(), i);

                    findViewById(R.id.textView_ScanHint).setVisibility(View.GONE);
                    if (!BuildConfig.DEBUG)
                        userInfo.edit().putLong("LastTime", System.currentTimeMillis()).apply();

                });


        currentPlaceTask.addOnFailureListener(
                (exception) ->
                {
                    if (exception instanceof ApiException)
                    {
                        ApiException apiException = (ApiException) exception;
                        int statusCode = apiException.getStatusCode();
                        if (statusCode == 9010)
                            Toast.makeText(this, getString(R.string.APINoQuota), Toast.LENGTH_LONG).show();

                        ((TextView)findViewById(R.id.textView_ScanHint)).setText(R.string.Scan_Fail_Hint);
                    }
                    exception.printStackTrace();
                });
    }

    /**
     * Fetches a list of {@link PlaceLikelihood} instances that represent the Places the user is
     * most
     * likely to be at currently.
     */
    private void findCurrentPlace()
    {
        if (!checkPermission(ACCESS_WIFI_STATE) || !checkPermission(ACCESS_FINE_LOCATION))
        {
            Toast.makeText(
                    this,
                    getString(R.string.OnPermissionRefused),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Note that it is not possible to request a normal (non-dangerous) permission from
        // ActivityCompat.requestPermissions(), which is why the checkPermission() only checks if
        // ACCESS_FINE_LOCATION is granted. It is still possible to check whether a normal permission
        // is granted or not using ContextCompat.checkSelfPermission().
        if (checkPermission(ACCESS_FINE_LOCATION))
            findCurrentPlaceWithPermissions();
    }

    /**
     *
     * @param Title: Set Text of TextView(Title)
     * @param IndexTag: Save in Button.Tag
     * @return A LinearLayout contains content_main_list
     */
    private void addNewPlaceToScrollView(String Title, int IndexTag)
    {
        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout scrollLayout = (LinearLayout)findViewById(R.id.linearLayoutScroll);

        getLayoutInflater().inflate(R.layout.content_main_list, linearLayout);
        ((TextView)linearLayout.findViewById(R.id.textView_Title)).setText(Title);

        Button button = (Button)linearLayout.findViewById(R.id.buttonNextStep);
        button.setTag(IndexTag);
        button.setOnClickListener(this::onClickNextStep);

        scrollLayout.addView(linearLayout);
    }

    private boolean checkPermissionDialog()
    {
        if (!checkPermission(ACCESS_FINE_LOCATION))
            placeDialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.Permission))
                .setMessage(getResources().getString(R.string.PermissionLocationHint))
                .setPositiveButton(getResources().getString(R.string.Understand), (dialog,  which) ->
                        {
                            askPermission(ACCESS_FINE_LOCATION);
                            if (phoneStateDialog != null)
                                phoneStateDialog.show();
                            placeDialog = null;
                        }
                );
        if (!checkPermission(READ_PHONE_STATE))
            phoneStateDialog = new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.Permission))
                    .setMessage(getResources().getString(R.string.PermissionPhoneStateHint))
                    .setPositiveButton(getResources().getString(R.string.Understand), (dialog,  which) ->
                            {
                                askPermission(READ_PHONE_STATE);
                                phoneStateDialog = null;
                            }
                    );
        if (placeDialog != null)
            placeDialog.show();
        else if (phoneStateDialog != null)
            phoneStateDialog.show();
        else
            return true;
        return false;
    }

    private boolean checkPermission(String permission)
    {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermission(String permission)
    {
        ActivityCompat.requestPermissions(this, new String[]{ permission }, 0);
    }

    private void reloadActivity()
    {
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);
    }

    private boolean checkInternet()
    {
        ConnectivityManager mConnectivityManager =  (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        try
        {
            if (networkInfo == null || !networkInfo.isConnectedOrConnecting())
                return false;
        }catch (Exception e)
        {
            return false;
        }
        return true;
    }
}
