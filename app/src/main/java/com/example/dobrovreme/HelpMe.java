package com.example.dobrovreme;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HelpMe extends AppCompatActivity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_me);

        TextView odg1 = (TextView) findViewById(R.id.navodila2);
        odg1.setText("V polje za iskanje vnesete ime kraja za katerega vas zanima vreme. " +
                "Ime kraja je lahko v jeziku drzave v kateri s nahaja ali v angleškem ekvivalentu," +
                " če le ta obstaja.");

        TextView odg2 = (TextView) findViewById(R.id.navodila4);
        odg2.setText("Kraje je mogoče ločiti po državah. To lahko dosežemo tako, da vpišemo ime " +
                "kraja vstavimo vejico (,) in dopišemo mednarodno dogovorjeno okrajšavo ime " +
                "države. V primeru Slovenije bi to bilo \"SI\" (Kapitalizacija je ignorirana)." );

        TextView primer1 = (TextView) findViewById(R.id.navodila5);
        primer1.setText("Primer: Kremen,si");

        TextView odg3 = (TextView) findViewById(R.id.navodila7);
        odg3.setText("Osveževanje je preprosto. Enostavno kliknite krožno ikono v skrajnem " +
                "desnem spodnjem kotu ob datumu zadnje osvežitve.");

        ImageView nazaj = (ImageView) findViewById(R.id.nazaj);

        nazaj.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
