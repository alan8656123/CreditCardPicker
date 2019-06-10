package cc.solla.creditcardpicker;

import android.database.Cursor;
import android.support.v4.util.Pair;
import android.util.SparseArray;

import java.util.HashMap;

import static cc.solla.creditcardpicker.StaticFunction.*;

public class CardInfo {
    private String cardNum, cardName, cardBankName;
    public SparseArray<SparseArray<Pair<String, String>>> Promo = null;
    public HashMap<String, Pair<String, String>> mobilePayPromo;
    public SparseArray<Pair<String, String>> onlinePromo = null;

    public CardInfo(String cardNum, String cardName, String cardBankName)
    {
        this.cardNum = cardNum;
        this.cardName = cardName;
        this.cardBankName = cardBankName;
    }

    public String getCardNum()
    {
        return cardNum;
    }
    public String getCardName()
    {
        return cardName;
    }
    public String getCardBankName() { return cardBankName; }

    public void readFromDatabase()
    {
        //TODO Rewrite
        if (Promo != null)
            return;
        SparseArray<SparseArray<Pair<String, String>>> _Promo = new SparseArray<>();
        while (promoList == null)
            ;
        while (!promoList.isOpen())
            ;
        for (int i = 0; i < FeatureChineseName.length + 3; i++)
        {
            String tableName = "_" + Integer.toString(i);
            SparseArray<Pair<String, String>> promoInfo = new SparseArray<>();
            Cursor cursor = promoList.rawQuery("select Info from " + tableName + " where CardNum = " + cardNum, null);
            while (cursor.moveToNext())
            {
                String info = cursor.getString(0);
                String[] Arrays = info.split("\t");
                for (int j = 0; j < Arrays.length / 3; ++j)
                {
                    String shopIndex = Arrays[j * 3],
                            shopPromoBrief = Arrays[j * 3 + 1],
                            shopPromoDetail = Arrays[j * 3 + 2];
                    if (i != FeatureChineseName.length + 2) //MobilePay
                    {
                        int index = Integer.parseInt(shopIndex);
                        if (promoInfo.indexOfKey(index) < 0)
                            promoInfo.put(index, new Pair<>(shopPromoBrief, shopPromoDetail));
                        else
                        {
                            Pair<String, String> oldPromoPair = promoInfo.get(index);
                            Pair<String, String> promoPair = new Pair<>(oldPromoPair.first + "\t" + shopPromoBrief, oldPromoPair.second + "\t" + shopPromoDetail);
                            promoInfo.put(index, promoPair);
                        }
                    }else
                    {
                        if (mobilePayPromo == null)
                            mobilePayPromo = new HashMap<>();
                        if (!mobilePayPromo.containsKey(shopIndex))
                            mobilePayPromo.put(shopIndex, new Pair<>(shopPromoBrief, shopPromoDetail));
                        else
                        {
                            Pair<String, String> oldPromoPair = mobilePayPromo.get(shopIndex);
                            Pair<String, String> promoPair = new Pair<>(oldPromoPair.first + "\t" + shopPromoBrief, oldPromoPair.second + "\t" + shopPromoDetail);
                            mobilePayPromo.remove(shopIndex);
                            mobilePayPromo.put(shopIndex, promoPair);
                        }

                    }
                }
            }
            cursor.close();
            _Promo.put(i, promoInfo);
        }
        Promo = _Promo;
    }
    public void readOnlineFromDatabase()
    {
        if (onlinePromo != null)
            return;
        while (onlineList == null)
            ;
        while (!onlineList.isOpen())
            ;
        SparseArray<Pair<String, String>> list = new SparseArray<>();
        Cursor cursor = onlineList.rawQuery("select Info from OnlineShopping where CardNum = " + cardNum, null);
        while (cursor.moveToNext())
        {
            String info = cursor.getString(0);
            String[] Arrays = info.split("\t");
            for (int j = 0; j < Arrays.length / 3; ++j)
            {
                String shopIndex = Arrays[j * 3],
                        shopPromoBrief = Arrays[j * 3 + 1],
                        shopPromoDetail = Arrays[j * 3 + 2];

                int index = Integer.parseInt(shopIndex);
                if (list.indexOfKey(index) < 0)
                    list.put(index, new Pair<>(shopPromoBrief, shopPromoDetail));
                else
                {
                    Pair<String, String> oldPromoPair = list.get(index);
                    Pair<String, String> promoPair = new Pair<>(oldPromoPair.first + "\t" + shopPromoBrief, oldPromoPair.second + "\t" + shopPromoDetail);
                    list.put(index, promoPair);
                }
            }
        }
        cursor.close();
        onlinePromo = list;
    }
}
