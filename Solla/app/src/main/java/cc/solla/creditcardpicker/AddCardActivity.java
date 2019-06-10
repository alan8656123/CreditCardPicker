package cc.solla.creditcardpicker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cc.solla.creditcardpicker.StaticFunction.*;

public class AddCardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        InitSharedPreferences(this);
        initCardList();
        RefreshCardList();
    }

    public void onClickAddCard(View v)
    {
        EditText cardNum = (EditText)findViewById(R.id.editTextCardNum);
        String cardNumText = cardNum.getText().toString();
        if (!isDatabaseReady())
        {
            Toast.makeText(this, getString(R.string.DatabaseNotReadyHint), Toast.LENGTH_SHORT)
                    .show();
            return;

        }
        if (cardNumText.length() != 8)
        {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (cardNumList.contains(cardNumText))
        {
            Toast.makeText(this, getString(R.string.AlreadyAddThisCard), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Cursor cursor = cardList.query("CardNum", new String[]{"CardNum", "BankName", "CardName"},
                "CardNum=?",new String[]{cardNumText},null,null,null);
        if (!cursor.moveToNext())
        {
            Toast.makeText(this, getString(R.string.NotSupportCreditCardHint), Toast.LENGTH_SHORT)
                    .show();
            cursor.close();
            return;
        }
        String _cardNum = cursor.getString(0);
        String _bankName = cursor.getString(1).replace(",", "");
        String _cardName = cursor.getString(2).replace(",", "");
        cursor.close();

        cardNumList.add(_cardNum);
        cardNameList.add(_cardName);
        cardBankNameList.add(_bankName);
        initCardInfoPromo_MT(_cardNum, _cardName, _bankName);
        sendAddCard(_cardNum);  //Record Add Card

        cardNum.getText().clear();
        RefreshCardList();
        SaveCardList();
    }


    private void RefreshCardList()
    {
        LinearLayout scrollLayout = (LinearLayout)findViewById(R.id.linearListCardList);
        scrollLayout.removeAllViews();
        int length = cardNumList.size();
        for (int i = 0; i < length; ++i)
            addNewPlaceToScrollView(cardNumList.get(i), cardNameList.get(i), cardBankNameList.get(i), i);
    }

    /**
     *
     * @param cardName: Set Text of TextView(Title)
     * @param IndexTag: Save in Button.Tag
     * @return A LinearLayout contains content_main_list
     */
    private void addNewPlaceToScrollView(String cardNum, String cardName, String BankName, int IndexTag)
    {
        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout scrollLayout = (LinearLayout)findViewById(R.id.linearListCardList);

        getLayoutInflater().inflate(R.layout.content_card_list, linearLayout);
        ((TextView)linearLayout.findViewById(R.id.textViewCardName)).setText(cardNum + " " + BankName + "\n" + cardName);

        Button button = (Button)linearLayout.findViewById(R.id.buttonRemoveCard);
        button.setTag(IndexTag);
        button.setOnClickListener((v ->
        {
            int index = (int)v.getTag();
            cardNumList.remove(index);
            cardBankNameList.remove(index);
            cardNameList.remove(index);
            RefreshCardList();
            SaveCardList();
        }));

        scrollLayout.addView(linearLayout);
    }

    @Override
    public void onBackPressed() {
        onSupportNavigateUp();
    }

    @Override
    public boolean onSupportNavigateUp(){
        boolean isForceBackMainActivity = getIntent().getBooleanExtra("isForceBackMainActivity", false);
        finish();
        if (isForceBackMainActivity)
            startActivity(new Intent(this, MainActivity.class));
        return true;
    }

}
