package cc.solla.creditcardpicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.MODE_PRIVATE;
import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;
import android.view.MenuItem;
import android.widget.Toast;

public class StaticFunction
{

    public static SharedPreferences userInfo, cardInfo;
    public static long userID;

    private static final Place.Type[] Convenience = { Place.Type.CONVENIENCE_STORE };
    private static final Place.Type[] PlaneTicket = { Place.Type.AIRPORT };
    private static final Place.Type[] TravelAgency = { Place.Type.TRAVEL_AGENCY };
    private static final Place.Type[] Hotel = { Place.Type.LODGING };
    private static final Place.Type[] Insurance = { Place.Type.INSURANCE_AGENCY };
    private static final Place.Type[] GOV = { Place.Type.CITY_HALL, Place.Type.LOCAL_GOVERNMENT_OFFICE };
    private static final Place.Type[] Movie = { Place.Type.MOVIE_RENTAL, Place.Type.MOVIE_THEATER };
    private static final Place.Type[] Car = { Place.Type.CAR_DEALER, Place.Type.CAR_RENTAL, Place.Type.CAR_REPAIR, Place.Type.CAR_WASH, Place.Type.PARKING };
    private static final Place.Type[] Maintain = { Place.Type.GYM, Place.Type.SPA, Place.Type.STADIUM, Place.Type.BEAUTY_SALON, Place.Type.HAIR_CARE };
    private static final Place.Type[] Asobi = { Place.Type.AMUSEMENT_PARK, Place.Type.AQUARIUM, Place.Type.ART_GALLERY, Place.Type.ZOO, Place.Type.MUSEUM };
    private static final Place.Type[] Traffic = { Place.Type.BUS_STATION, Place.Type.SUBWAY_STATION, Place.Type.TAXI_STAND, Place.Type.TRAIN_STATION, Place.Type.TRANSIT_STATION };
    private static final Place.Type[] Food = { Place.Type.NIGHT_CLUB, Place.Type.BAKERY, Place.Type.RESTAURANT, Place.Type.FOOD, Place.Type.CAFE, Place.Type.BAR };
    private static final Place.Type[] Hospital = { Place.Type.VETERINARY_CARE, Place.Type.PHARMACY, Place.Type.PHYSIOTHERAPIST, Place.Type.HOSPITAL, Place.Type.DENTIST, Place.Type.DOCTOR };
    private static final Place.Type[] Shop = { Place.Type.STORE, Place.Type.SUPERMARKET, Place.Type.HEALTH, Place.Type.GROCERY_OR_SUPERMARKET, Place.Type.HOME_GOODS_STORE, Place.Type.HARDWARE_STORE, Place.Type.LIQUOR_STORE, Place.Type.ELECTRONICS_STORE, Place.Type.DEPARTMENT_STORE, Place.Type.SHOPPING_MALL, Place.Type.BOOK_STORE, Place.Type.CLOTHING_STORE, Place.Type.SHOE_STORE };
    private static final Place.Type[][] TypeList = { Convenience, PlaneTicket, TravelAgency, Hotel, Insurance, GOV, Movie, Car, Maintain, Asobi, Traffic, Food, Hospital, Shop };
    public enum PlaceFeature { Convenience, PlaneTicket, TravelAgency, Hotel, Insurance, GOV, Movie, Car, Maintain, Asobi, Traffic, Food, Hospital, Shop }
    public static final String[] FeatureChineseName = { "便利超商", "機票", "旅行社", "旅館", "保險", "政府單位", "電影", "汽機車服務", "健美類", "遊樂類", "交通", "食物餐廳類" , "醫院" , "商店" };
    public static ArrayList<ArrayList<String>> shopNameList = null;
    public static SparseArray<String> onlineUrlList = null;
    public static ArrayList<String> AirlineHashSet = null;
    public static List<PlaceLikelihood> placeList;

    public static List<String> cardNumList;
    public static List<String> cardNameList;
    public static List<String> cardBankNameList;
    public static HashMap<String, CardInfo> cardInfoMap;

    private static final String ShopListDataBaseUrl = "https://sites.google.com/site/cardpickerdb/ShopList.db";
    private static final String PromoListDataBaseUrl = "https://sites.google.com/site/cardpickerdb/PromoList.db";
    private static final String OnlineDataBaseUrl = "https://sites.google.com/site/cardpickerdb/Online.db";
    private static final String CardNumDataBaseUrl = "https://sites.google.com/site/cardpickerdb/CardNum.db";
    private static final String AirlineUrl = "https://sites.google.com/site/cardpickerdb/airline.txt";
    private static final String[] databaseUrlSet = { ShopListDataBaseUrl, PromoListDataBaseUrl, OnlineDataBaseUrl, CardNumDataBaseUrl };
    public static SQLiteDatabase shopList = null, promoList = null, onlineList = null, cardList = null;
    private static final String[] databaseFileName = { "ShopList.db", "PromoList.db", "Online.db", "CardNum.db" };
    private static final boolean[] databaseConnection = { false, false, false, false };
    public static final String[] CityNameList =
            {
                    "台北",
                    "新北",
                    "桃園",
                    "台中",
                    "台南",
                    "高雄",
                    "基隆",
                    "新竹",
                    "嘉義",
                    "苗栗",
                    "彰化",
                    "南投",
                    "雲林",
                    "嘉義",
                    "屏東",
                    "花蓮",
                    "宜蘭",
                    "臺東",
                    "澎湖",
                    "連江",
                    "金門"
            };
    public static void InitSharedPreferences(@NonNull Context context)
    {
        if (StaticFunction.userInfo == null)
            StaticFunction.userInfo = context.getSharedPreferences("UserInfo" , MODE_PRIVATE);
        if (StaticFunction.cardInfo == null)
            StaticFunction.cardInfo = context.getSharedPreferences("CardInfo", MODE_PRIVATE);
        userID = userInfo.getLong("userID", 0);
    }

    public static boolean isDatabaseReady()
    {
        return databaseConnection[0] && databaseConnection[1] && databaseConnection[2] && databaseConnection[3];
    }

    public static boolean cardCountCheck(Activity activity)
    {
        int CardCount = userInfo.getInt("CardCount", 0);
        if (CardCount > 0)
            return true;

        Toast.makeText(activity, activity.getString(R.string.NoCardHint), Toast.LENGTH_LONG)
                .show();
        activity.finish();
        Intent intent = new Intent(activity, AddCardActivity.class);
        if (activity.getClass() == MainActivity.class)
            intent.putExtra("isForceBackMainActivity", true);
        activity.startActivity(intent);
        return false;
    }

    public static void initCardList()
    {
        if (cardNumList != null)    //Is Loaded?
            return;

        String rawCardNum = cardInfo.getString("CardNum", null);
        String rawCardName = cardInfo.getString("CardName", null);
        String rawCardBankName = cardInfo.getString("CardBankName", null);

        if (rawCardNum != null && rawCardNum.length() != 0) {
            cardNumList = new ArrayList<>(Arrays.asList(rawCardNum.split(",")));
            cardNameList = new ArrayList<>(Arrays.asList(rawCardName.split(",")));
            cardBankNameList = new ArrayList<>(Arrays.asList(rawCardBankName.split(",")));
        }
        else{
            cardNumList = new ArrayList<>();
            cardNameList = new ArrayList<>();
            cardBankNameList = new ArrayList<>();
        }

        HashMap<String, CardInfo> hashMap = new HashMap<>();
        int length = cardNumList.size();
        for (int i = 0; i < length; ++i)
            hashMap.put(cardNumList.get(i), new CardInfo(cardNumList.get(i), cardNameList.get(i), cardBankNameList.get(i)));
        cardInfoMap = hashMap;
    }

    public static boolean isCardFullInit()
    {
        if (cardInfoMap == null || onlineUrlList == null)
            return false;
        if (cardInfoMap.size() < cardNumList.size())
            return false;
        for (CardInfo info : cardInfoMap.values())
            if (info.Promo == null || info.onlinePromo == null)
                return false;
        return true;
    }

    public static void initCardInfoPromo_MT(String cardNum, String cardName, String cardBankName)
    {
        while (cardInfoMap == null)
            ;

        try
        {
            if (!cardInfoMap.containsKey(cardNum))
                cardInfoMap.put(cardNum, new CardInfo(cardNum, cardName, cardBankName));
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        CardInfo info = cardInfoMap.get(cardNum);

        new Thread(new Thread() {
            private CardInfo card;
            public Thread set(CardInfo cardInfo) {
                card = cardInfo;
                return this;
            }

            @Override
            public void run() {
                card.readFromDatabase();
            }
        }.set(info)).start();
    }

    public static void initCardInfoOnline_MT(String cardNum, String cardName, String cardBankName)
    {
        while (cardInfoMap == null)
            ;

        try
        {
            if (!cardInfoMap.containsKey(cardNum))
                cardInfoMap.put(cardNum, new CardInfo(cardNum, cardName, cardBankName));
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        CardInfo info = cardInfoMap.get(cardNum);

        new Thread(new Thread() {
            private CardInfo card;
            public Thread set(CardInfo cardInfo) {
                card = cardInfo;
                return this;
            }

            @Override
            public void run() {
                card.readOnlineFromDatabase();
            }
        }.set(info)).start();
    }

    public static void SaveCardList()
    {
        if (cardNumList == null)
            return;
        StringBuilder sbCardNum = new StringBuilder();
        StringBuilder sbcardName = new StringBuilder();
        StringBuilder sbcardBankName = new StringBuilder();
        int length = cardNumList.size();
        for (int i = 0; i < length; i++) {
            sbCardNum.append(cardNumList.get(i)).append(",");
            sbcardName.append(cardNameList.get(i)).append(",");
            sbcardBankName.append(cardBankNameList.get(i)).append(",");
        }

        cardInfo.edit()
                .putString("CardNum", sbCardNum.toString())
                .putString("CardName", sbcardName.toString())
                .putString("CardBankName", sbcardBankName.toString())
                .apply();

        userInfo
                .edit()
                .putInt("CardCount", length)
                .apply();
    }

    public static List<PlaceFeature> getAllFeature(List<Place.Type> inputListOfTypes)
    {
        List<PlaceFeature> result = new ArrayList<>();
        if (inputListOfTypes == null || inputListOfTypes.size() == 0)
            return result;
        for (int i = 0; i < TypeList.length; ++i)
        {
            for (Place.Type type:
                 TypeList[i])
            {
                if (!inputListOfTypes.contains(type))   //Not contains
                    continue;
                result.add(PlaceFeature.values()[i]);   //Contains. Add to List
                break;  //Scan next
            }
        }
        return result;
    }

    private static void setDatabase(int index, SQLiteDatabase sqLiteDatabase)
    {
        int length;
        if (sqLiteDatabase != null)
            databaseConnection[index] = true;
        switch (index)
        {
            case 0:
                shopList = sqLiteDatabase;
                initShopList();
                break;
            case 1:
                promoList = sqLiteDatabase;
                while (cardInfoMap == null || cardNumList == null)
                    ;
                length = cardNumList.size();
                for (int i = 0; i < length; ++i)
                    initCardInfoPromo_MT(cardNumList.get(i), cardNameList.get(i), cardBankNameList.get(i));
                break;
            case 2:
                onlineList = sqLiteDatabase;
                while (cardInfoMap == null || cardNumList == null)
                    ;
                length = cardNumList.size();
                for (int i = 0; i < length; ++i)
                    initCardInfoOnline_MT(cardNumList.get(i), cardNameList.get(i), cardBankNameList.get(i));
                initOnlineList();
                break;
            case 3:
                cardList = sqLiteDatabase;
                break;
        }
    }

    public static void initShopList()
    {
        if (shopNameList != null)
            return;
        ArrayList<ArrayList<String>> largeList = new ArrayList<>();
        while (!shopList.isOpen())
            ;
        for (int i = 0; i < FeatureChineseName.length; ++i)
        {
            String tableName = "_" + Integer.toString(i);
            ArrayList<String> smallList = new ArrayList<>();
            largeList.add(smallList);
            Cursor cursor = shopList.rawQuery("select * from " + tableName,null);
            while (cursor.moveToNext())
            {
                int index = cursor.getInt(0);
                String shopName = cursor.getString(1);
                smallList.add(index, shopName);
            }
            cursor.close();
        }
        shopNameList = largeList;
    }

    public static void initOnlineList()
    {
        if (onlineUrlList != null)
            return;
        while (!onlineList.isOpen())
            ;
        SparseArray<String> list = new SparseArray<>();
        Cursor cursor = onlineList.rawQuery("select * from OnlineShoppingList",null);
        while (cursor.moveToNext())
        {
            int index = cursor.getInt(0);
            String shopUrl = cursor.getString(1).toLowerCase();
            list.put(index, shopUrl);
        }
        cursor.close();
        onlineUrlList = list;
    }

    public static void checkAndFetchDatabase(Context context)
    {
        downloadAirlineKeyword(context);
        for (int i = 0; i < databaseFileName.length; ++i)
        {
            new Thread(new Thread() {
                private Context context;
                private int i;

                public Thread set(Context c, int i) {
                    this.context = c;
                    this.i = i;
                    return this;
                }

                @Override
                public void run() {
                    File file = new File(context.getFilesDir() + "/" + databaseFileName[i]);
                    long nowSystemTime = System.currentTimeMillis() / (60*60*24*1000);

                    long fileModifiedTime = file.lastModified()/(60*60*24*1000);
                    if (file.exists() && fileModifiedTime != nowSystemTime)
                        if (!file.delete())
                            file.deleteOnExit();
                    while (!file.exists())
                        StaticFunction.downloadHttpsSaveFiles(context, databaseUrlSet[i], databaseFileName[i]);
                    setDatabase(i, SQLiteDatabase.openDatabase(file.getPath(), null, MODE_PRIVATE));
                }
            }.set(context, i)).start();
        }
    }
    public static void sendOpenAppRecord()
    {
        asyncSendParameter(1, new String[] { String.valueOf(userID) });
    }

    public static void sendAddCard(String cardNum)
    {
        asyncSendParameter(2, new String[] { String.valueOf(userID), cardNum });
    }

    public static void openMobilePay(String PayName)
    {
        asyncSendParameter(3, new String[] { String.valueOf(userID), PayName });
    }

    public static void sendOnlineShopping(String Url, String AppResult)
    {
        asyncSendParameter(4, new String[] { String.valueOf(userID), Url, AppResult });
    }

    public static void sendContactPay(String ShopName, double Lat, double Lng, String cityName)
    {
        asyncSendParameter(5, new String[] { String.valueOf(userID), ShopName,String.valueOf(Lat), String.valueOf(Lng), cityName });
    }


    private static void asyncSendParameter(int type, @NonNull String[] parameter)
    {
        StringBuilder stringBuilder = new StringBuilder("https://script.google.com/macros/s/AKfycbzDfBPsI7Y2NUn7TuZ1Z7p9aZ7rz8KD9vKv1t3UY4pdsRCy3D5k/exec?");
        try {
            stringBuilder
                    .append("type=")
                    .append(String.valueOf(type));
            for (int i = 0; i < parameter.length; ++i) {
                stringBuilder
                        .append("&p")
                        .append(String.valueOf(i))
                        .append("=")
                        .append(URLEncoder.encode(parameter[i], "UTF-8"));
            }
        }catch (Exception e)
        {

        }
        String url = stringBuilder.toString();

        new Thread(()->{
            try {
                URL urlPath = new URL(url);
                HttpsURLConnection https = (HttpsURLConnection) urlPath.openConnection();//設定超時間為3秒
                https.setConnectTimeout(3 * 1000);
                //防止遮蔽程式抓取而返回403錯誤
                https.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
                https.getResponseCode();
            }catch (Exception e)
            {

            }
        }).start();
    }
    private static void downloadAirlineKeyword(Context context)
    {
        new Thread(new Thread() {
            private Context context;

            public Thread set(Context c) {
                this.context = c;
                return this;
            }

            @Override
            public void run() {
                File file = new File(context.getFilesDir() + "/airline.txt");
                long nowSystemTime = System.currentTimeMillis() / (60*60*24*1000);

                long fileModifiedTime = file.lastModified()/(60*60*24*1000);
                if (file.exists() && fileModifiedTime != nowSystemTime)
                    if (!file.delete())
                        file.deleteOnExit();
                while (!file.exists())
                    StaticFunction.downloadHttpsSaveFiles(context, AirlineUrl, "airline.txt");
                ArrayList<String> temp = new ArrayList<>();
                try
                {
                    Scanner scanner = new Scanner(file);
                    while (scanner.hasNextLine())
                    {
                        String keyword = scanner.nextLine().toLowerCase().trim().replace(" ", "").replace("\n", "").replace("\t", "");
                        temp.add(keyword);
                    }
                }catch (Exception e)
                {
                    Log.e("StaticFunction", e.toString());
                }
                AirlineHashSet = temp;
            }
        }.set(context)).start();
    }
    private static InputStream getHttpsFile(String url)
    {
        try
        {
            URL urlPath = new URL(url);
            HttpsURLConnection https = (HttpsURLConnection)urlPath.openConnection();//設定超時間為3秒
            https.setConnectTimeout(3*1000);
            //防止遮蔽程式抓取而返回403錯誤
            https.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
            //得到輸入流
            return https.getInputStream();
        }catch (Exception e)
        {
            Log.e("StaticFunction", e.toString());
        }
        return null;
    }

    private static void downloadHttpsSaveFiles(Context context, String url, String FileName)
    {
        try
        {
            InputStream inputStream = getHttpsFile(url);
            if (inputStream == null)
                return;
            int Size = 1024 * 1024;
            byte dbBuffer[] = new byte[Size];

            FileOutputStream fos = context.openFileOutput(FileName, Context.MODE_PRIVATE);
            int fileSize;

            while ((fileSize = inputStream.read(dbBuffer)) > 0)
                fos.write(dbBuffer, 0, fileSize);

            fos.flush();
            fos.close();
        }catch (Exception e)
        {
            Log.e("StaticFunction", e.toString());
        }
    }

    public static void OpenApplication(Context context, String PackageName) throws RuntimeException
    {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(PackageName);
        if (intent == null)
            throw new RuntimeException();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean MenuMainHandler(Context context, MenuItem item)
    {
        int id = item.getItemId();
        try {
            switch (id) {
                case R.id.ManageCard:
                    context.startActivity(new Intent(context, AddCardActivity.class));
                    return true;
                case R.id.OpenLinePay:
                    openMobilePay("LinePay");
                    Intent intent;
                    Uri linePayUri = Uri.parse("line://pay/generateQR");
                    intent = new Intent(Intent.ACTION_VIEW, linePayUri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return true;
                case R.id.OpenPIWallet:
                    openMobilePay("Pi");
                    OpenApplication(context, context.getResources().getString(R.string.PackageNamePI));
                    return true;
                case R.id.OpenGooglePay:
                    openMobilePay("GooglePay");
                    OpenApplication(context, context.getResources().getString(R.string.PackageNameGooglePay));
                    return true;
                case R.id.OpenLine:
                    openMobilePay("LinePay");
                    OpenApplication(context, context.getResources().getString(R.string.PackageNameLine));
                    return true;
                case R.id.OpenJkos:
                    openMobilePay("Jkos");
                    OpenApplication(context, context.getResources().getString(R.string.PackageNameJkos));
                    return true;
                case R.id.OpenSamsungPay:
                    openMobilePay("SamsungPay");
                    OpenApplication(context, context.getResources().getString(R.string.PackageNameSamsungPay));
                    return true;
                default:
                    return false;
            }
        }catch (Exception e)
        {
            Toast.makeText(context, "無法" + item.getTitle(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public static String getFeatureName(int index)
    {
        if (index < FeatureChineseName.length)
            return FeatureChineseName[index];
        int Val = index - FeatureChineseName.length;
        switch (Val)
        {
            case 0:
                return "國內消費";
            case 1:
                return "國外消費";
            case 2:
                return "行動支付";
        }
        return "???????";
    }

    public static boolean isAirline(String url)
    {
        while (AirlineHashSet == null)
            ;
        for (String keyword:
                AirlineHashSet) {
            if (url.contains(keyword))
                return true;
        }
        return false;
    }
}
