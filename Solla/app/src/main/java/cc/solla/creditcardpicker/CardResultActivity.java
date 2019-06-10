package cc.solla.creditcardpicker;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static cc.solla.creditcardpicker.StaticFunction.*;
public class CardResultActivity extends AppCompatActivity {
    ArrayList<Pair<String, Pair<Integer, Integer>>> fitShopName = new ArrayList<>();
    LinearLayout checkBoxLinearLayout;
    LinearLayout cardResult;
    Place place;
    HashMap<CardInfo, CardLayout> promoRecordHashMap = new HashMap<>();

    private Pair<String, Pair<Integer, Integer>> packShopName(String str1, int int1, int int2) {
        return new Pair<>(str1, new Pair<>(int1, int2));
    }

    private boolean isTRA(String shopName) {
        if (!shopName.contains("車站") && !shopName.contains("鐵路") && !shopName.contains("台鐵"))
            return false;
        return true;
    }

    private String getCityName()
    {
        if (place.getAddress() == null)
            return "Error";
        String address = place.getAddress();
        address = address.replace("臺", "台");
        for (String cityName: CityNameList)
            if (address.contains(cityName))
                return cityName;
        return address;
    }
    private boolean isHSR(String shopName) {
        if (!shopName.contains("車站") && !shopName.contains("高鐵"))
            return false;
        if (shopName.contains("南港")
                || shopName.contains("台北") || shopName.contains("臺北")
                || shopName.contains("板橋")
                || shopName.contains("桃園")
                || shopName.contains("新竹") || shopName.contains("六家")
                || shopName.contains("苗栗") || shopName.contains("豐富")
                || shopName.contains("新烏日") || shopName.contains("台中") || shopName.contains("臺中")
                || shopName.contains("嘉義")
                || shopName.contains("台南") || shopName.contains("沙崙")
                || shopName.contains("高雄") || shopName.contains("新左營"))
            return true;
        return false;
    }

    private boolean isKRT(String shopName) {
        if (place.getAddress() == null)
            return false;
        if (!place.getAddress().contains("高雄"))
            return false;
        if (shopName.contains("MRT"))
            return true;
        if (shopName.contains("站") || shopName.contains("捷運"))
            return true;
        return false;

    }

    private void compareShopName(String shopName, PlaceFeature feature) {
        int placeTypeID = feature.ordinal();
        String placeTypeName = getFeatureName(placeTypeID);
        ArrayList<String> correspondShopNameList = shopNameList.get(placeTypeID);
        createCheckbox(placeTypeID);
        switch (feature) {
            case Traffic:
                fitShopName.add(packShopName(placeTypeName + "：全部", placeTypeID, 0));
                if (isHSR(shopName))
                    fitShopName.add(packShopName(placeTypeName + "：高鐵", placeTypeID, 1));
                if (isTRA(shopName))
                    fitShopName.add(packShopName(placeTypeName + "：台鐵", placeTypeID, 2));
                if (isKRT(shopName))
                    fitShopName.add(packShopName(placeTypeName + "：高雄捷運", placeTypeID, 3));
                break;

            case PlaneTicket:
                fitShopName.add(packShopName(placeTypeName + "：全部", placeTypeID, 0));
                if (shopName.contains("航空"))
                    fitShopName.add(packShopName(placeTypeName, placeTypeID, 1));
                if (shopName.contains("旅行社"))
                    fitShopName.add(packShopName(placeTypeName, placeTypeID, 1));
                break;

            default:
                int shopNameListLength = correspondShopNameList.size();
                for (int i = 0; i < shopNameListLength; ++i) {
                    String shopKeyName = correspondShopNameList.get(i);
                    if (i != 0 && !shopName.contains(shopKeyName))
                        continue;
                    fitShopName.add(packShopName(placeTypeName + "：" + shopKeyName, placeTypeID, i));
                }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_result);
        int index = getIntent().getIntExtra("Index", -1);
        if (index < 0) {
            Toast.makeText(this, getString(R.string.CardNumTooShort), Toast.LENGTH_LONG).show();
            return;
        }

        place = placeList.get(index).getPlace();
        String shopName = place.getName();

        if (shopName == null)
            return;
        if (place.getLatLng() != null)
            sendContactPay(shopName, place.getLatLng().latitude, place.getLatLng().longitude, getCityName());

        ((TextView) findViewById(R.id.textView_CardResultName)).setText(shopName);
        List<PlaceFeature> placeFeature = getAllFeature(place.getTypes());

        checkBoxLinearLayout = findViewById(R.id.typeLinearLayout);


        for (PlaceFeature feature : placeFeature)
            compareShopName(shopName, feature);

        fitShopName.add(packShopName("國內消費", FeatureChineseName.length, 0));
        createCheckbox(FeatureChineseName.length);
        createCheckbox(FeatureChineseName.length + 2);

        cardResult = findViewById(R.id.linearLayoutCardResult);
        for (String cardNum : cardNumList) {
            CardInfo cardInfo = cardInfoMap.get(cardNum);
            if (cardInfo == null)
                continue;
            CardLayout cardLayout = null;
            for (Pair<String, Pair<Integer, Integer>> mPair : fitShopName)  //<ShopName, <Index of Type, Index of Shop>>
            {
                if (mPair.first == null || mPair.second == null)
                    continue;
                String promoShopName = mPair.first;
                Pair<Integer, Integer> pair = mPair.second;
                if (pair.first == null || pair.second == null)
                    continue;
                SparseArray<Pair<String, String>> array = cardInfo.Promo.get(pair.first);
                if (array == null)
                    continue;
                if (array.indexOfKey(pair.second) < 0)
                    continue;
                Pair<String, String> promo = array.get(pair.second);
                String BriefPromo[] = promo.first.split("\t");
                String DetailPromo[] = promo.second.split("\t");
                for (int i = 0; i < BriefPromo.length; ++i) {
                    LinearLayout layoutPromoInfo = createCardPromoInfo(promoShopName, BriefPromo[i], DetailPromo[i]);
                    if (cardLayout == null) {
                        LinearLayout layoutCardInfo = createCardContainer(cardInfo);
                        cardLayout = new CardLayout(layoutCardInfo);
                        cardResult.addView(layoutCardInfo);
                        promoRecordHashMap.put(cardInfo, cardLayout);
                    }
                    cardLayout.insertNode(pair.first, layoutPromoInfo);
                }
            }
            if (cardInfo.mobilePayPromo != null) {
                for (String mobilePay : cardInfo.mobilePayPromo.keySet()) {
                    Pair<String, String> pair = cardInfo.mobilePayPromo.get(mobilePay);
                    String BriefPromo[] = pair.first.split("\t");
                    String DetailPromo[] = pair.second.split("\t");
                    for (int i = 0; i < BriefPromo.length; ++i) {

                        LinearLayout layoutPromoInfo = createCardPromoInfo(mobilePay, BriefPromo[i], DetailPromo[i]);
                        if (cardLayout == null) {
                            LinearLayout layoutCardInfo = createCardContainer(cardInfo);
                            cardLayout = new CardLayout(layoutCardInfo);
                            cardResult.addView(layoutCardInfo);
                            promoRecordHashMap.put(cardInfo, cardLayout);
                        }
                        cardLayout.insertNode(FeatureChineseName.length + 2, layoutPromoInfo);
                    }
                }
            }
        }
    }

    private LinearLayout createCardPromoInfo(String ShopTitle, String BriefPromo, String DetailPromo) {
        LinearLayout linearLayout = new LinearLayout(this);
        getLayoutInflater().inflate(R.layout.content_card_promo_item, linearLayout);
        ((TextView) linearLayout.findViewById(R.id.textView_PromoBrief)).setText(BriefPromo);
        ((TextView) linearLayout.findViewById(R.id.textView_TypeTitle)).setText(ShopTitle);
        ((TextView) linearLayout.findViewById(R.id.textView_DetailPromo)).setText(DetailPromo);
        return linearLayout;
    }

    private LinearLayout createCardContainer(CardInfo cardInfo) {
        LinearLayout linearLayout = new LinearLayout(this);
        getLayoutInflater().inflate(R.layout.content_card_promo_list, linearLayout);
        String strCardInfo = cardInfo.getCardName() + '\n' +
                cardInfo.getCardNum() + ' ' + cardInfo.getCardBankName();

        ((TextView) linearLayout.findViewById(R.id.PromoCardInfo)).setText(strCardInfo);
        Button button = linearLayout.findViewById(R.id.buttonShowMore);

        button.setTag(cardInfo);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CardInfo cardInfo = (CardInfo)v.getTag();
                if (promoRecordHashMap.get(cardInfo) != null)
                    promoRecordHashMap.get(cardInfo).changeVisible();
            }
        });

        return linearLayout;
    }

    private void createCheckbox(int index) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(getFeatureName(index));
        checkBox.setTag(index);
        checkBox.setChecked(true);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Update ViewData
                int type = (int) buttonView.getTag();
                for (CardLayout cardLayout : promoRecordHashMap.values())
                    cardLayout.changeVisible(type, isChecked);
            }
        });
        checkBoxLinearLayout.addView(checkBox);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.StartScan).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        boolean returnValue = MenuMainHandler(this, item);
        if (!returnValue)
            returnValue = super.onOptionsItemSelected(item);
        return returnValue;
    }


}
class CardLayout {
    private LinearLayout outterLayout;
    private LinearLayout promoContainerLayout;
    private ArrayList<LinearLayout> listLayout = new ArrayList<>();
    private ArrayList<Integer> listIndex = new ArrayList<>();
    private ArrayList<Boolean> listVisibleBoolean = new ArrayList<>();
    private boolean isOutterLayoutVisible = true;

    public CardLayout(LinearLayout layout)
    {
        outterLayout = layout;
        promoContainerLayout = outterLayout.findViewById(R.id.promoItemLinearLayout);
    }
    public void insertNode(int typeID, LinearLayout linearLayout)
    {
        listLayout.add(linearLayout);
        listIndex.add(typeID);
        listVisibleBoolean.add(true);
        promoContainerLayout.addView(linearLayout);
    }

    public void changeVisible(int typeID, boolean visible)
    {
        if (!visible)
        {
            int length = listLayout.size();
            for (int i = 0; i < length; i++)
                if (listIndex.get(i) == typeID && listVisibleBoolean.get(i))
                {
                    promoContainerLayout.removeView(listLayout.get(i));
                    listVisibleBoolean.add(i, false);
                }
        }else
        {
            int length = listLayout.size();
            for (int i = 0; i < length; i++)
                if (listIndex.get(i) == typeID)
                    listVisibleBoolean.add(i, true);
            removeAllNode();
            addAllNode();
        }

    }

    public void removeAllNode()
    {
        promoContainerLayout.removeAllViews();
    }

    public void addAllNode()
    {
        int length = listLayout.size();
        for (int i = 0; i < length; i++)
            if (listVisibleBoolean.get(i))
                promoContainerLayout.addView(listLayout.get(i));
    }

    public void changeVisible()
    {
        if (isOutterLayoutVisible)
            removeAllNode();
        else
            addAllNode();
        isOutterLayoutVisible = !isOutterLayoutVisible;
    }
}
