package cc.solla.creditcardpicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static cc.solla.creditcardpicker.StaticFunction.*;

public class OnlineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        InitSharedPreferences(this);
        if (!isAgreeUserPolicy())
            return;
        checkAndFetchDatabase(this);
        initCardList();
        if (!cardCountCheck(this))
            return;

        Intent intent = getIntent();
        String action = intent.getAction();

//        if (action == null)
//        {
//            exitWithErrorMessage("Cannot read the source!");
//            return;
//        }else if (!action.equalsIgnoreCase(Intent.ACTION_SEND) || !intent.hasExtra(Intent.EXTRA_TEXT))
//        {
//            exitWithErrorMessage("Cannot read the source!");
//            return;
//        }
        String strInfo = intent.getStringExtra(Intent.EXTRA_TEXT);
        int urlStart = strInfo.indexOf("http"),
            urlEnd = strInfo.indexOf("/", urlStart + "https://".length());
        String urlStr;
        if (urlEnd < urlStart)
            urlStr = strInfo.substring(urlStart).toLowerCase();
        else
            urlStr = strInfo.substring(urlStart, urlEnd).toLowerCase();
        String onlineTitle = null;
        ((TextView)findViewById(R.id.textView_OnlineShowUrl)).setText("Url：" + urlStr);
        Toast toast = Toast.makeText(this, "Waiting for loading", Toast.LENGTH_LONG);
        toast.show();
        while (!isCardFullInit())
            ;
        toast.cancel();
        int promoIndex, forShopTypeIndex = 0;
        if (urlStr.contains("railway")) {
            onlineTitle = "台鐵";
            promoIndex = -2;
            forShopTypeIndex = PlaceFeature.Traffic.ordinal();
        }
        else if (urlStr.contains("thsrc")) {
            onlineTitle = "高鐵";
            promoIndex = -1;
            forShopTypeIndex = PlaceFeature.Traffic.ordinal();
        }
        else
        {
            int length = onlineUrlList.size();
            for (promoIndex = 0; promoIndex < length; ++promoIndex)
            {
                int index = onlineUrlList.keyAt(promoIndex);
                if (!urlStr.contains(onlineUrlList.get(index)))
                    continue;
                onlineTitle = onlineUrlList.get(index);
                promoIndex = index;
                break;
            }
            if (promoIndex == length)
                if (!isAirline(urlStr))
                    promoIndex = Integer.MIN_VALUE;
                else
                {
                    promoIndex = -1;
                    forShopTypeIndex = PlaceFeature.PlaneTicket.ordinal();
                    onlineTitle = "機票";
                }
        }
        if (onlineTitle == null)
            onlineTitle = "Cannot found any match promo!";

        LinearLayout onlineShoppingPromo = findViewById(R.id.onlineShoppingPromoLayout);
        for (String cardNum : cardNumList) {
            CardInfo cardInfo = cardInfoMap.get(cardNum);
            if (cardInfo == null)
                continue;
            LinearLayout cardPromoLayout = null;
            LinearLayout listItem;
            switch (promoIndex)
            {
                case Integer.MIN_VALUE:
                    break;
                case -1:
                case -2:
                    int realPromoIndex = -promoIndex;
                    Pair<String, String> pair = cardInfo.Promo.get(forShopTypeIndex).get(realPromoIndex);
                    if (pair == null)
                        continue;
                    cardPromoLayout = createCardContainer(cardInfo);
                    listItem = cardPromoLayout.findViewById(R.id.promoItemLinearLayout);
                    String[] promoBrief = pair.first.split("\t");
                    String[] promoDetail = pair.second.split("\t");
                    for (int i = 0; i < promoBrief.length; i++)
                        listItem.addView(createCardPromoInfo(onlineTitle, promoBrief[i], promoDetail[i]));
                    break;
                default:
                    Pair<String, String> onlinePair = cardInfo.onlinePromo.get(promoIndex);
                    if (onlinePair == null)
                        continue;
                    cardPromoLayout = createCardContainer(cardInfo);
                    listItem = cardPromoLayout.findViewById(R.id.promoItemLinearLayout);
                    String[] promoOnlineBrief = onlinePair.first.split("\t");
                    String[] promoOnlineDetail = onlinePair.second.split("\t");
                    for (int i = 0; i < promoOnlineBrief.length; i++)
                        listItem.addView(createCardPromoInfo(onlineTitle, promoOnlineBrief[i], promoOnlineDetail[i]));
                    break;
            }
            if (cardPromoLayout != null)
                onlineShoppingPromo.addView(cardPromoLayout);
        }
        if (onlineShoppingPromo.getChildCount() == 0)
        {
            TextView textView = new TextView(this);
            textView.setText("No Promo Found!");
            textView.setTextSize(32);
            onlineShoppingPromo.addView(textView);
        }
        ((TextView)findViewById(R.id.textView_OnlineTitle)).setText(onlineTitle);
        sendOnlineShopping(urlStr, onlineTitle);
    }

    private void exitWithErrorMessage(String errorMsg)
    {
        if (errorMsg != null)
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        finish();
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

        button.setTag(linearLayout.findViewById(R.id.promoItemLinearLayout));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConstraintLayout parent = (ConstraintLayout)v.getParent();
                LinearLayout cardList = (LinearLayout)v.getTag();
                if (parent.indexOfChild(cardList) < 0)
                    parent.addView(cardList);
                else
                    parent.removeView(cardList);
            }
        });

        return linearLayout;
    }
    private boolean isAgreeUserPolicy() {
        if (userInfo.getBoolean("AgreePolicy", false))
            return true;
        if (!userInfo.getBoolean("IsFirstTime", true))
            android.os.Process.killProcess(android.os.Process.myPid());
        Toast.makeText(this, "Not agree policy yet. Open this app directly and agree it.", Toast.LENGTH_LONG).show();
        return false;
    }
}
