package com.example.dobrovreme;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.*;

public class MainActivity extends AppCompatActivity {

    // Tukaj se vse začne -> ob nastanku Activityja
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializacija ikonc in gumbov
        ImageView help_me = (ImageView) findViewById(R.id.help); // gumb za informacije
        EditText kraj_search = (EditText) findViewById(R.id.kraj_search); // prostor za vnost kraja
        ImageView search_img = (ImageView) findViewById(R.id.search_img); // gumb (slika) za začetek poizvedovanja vnosa
        ProgressBar nalaganje = (ProgressBar) findViewById(R.id.nalaganje); // vrtalka med poizvedovanjem
        nalaganje.setVisibility(View.INVISIBLE); // skrij vrtalko (ker se nič ne poizveduje ob zagonu)
        ImageView refresh = (ImageView) findViewById(R.id.refresh); // gumb (slika) za osvežitev podatkov
        TextView nazadnje = (TextView) findViewById(R.id.nazadnje); // podatki o zadnji osvežitvi
        
        // informativni podatki o vremenu
        TextView kraj_text = (TextView) findViewById(R.id.kraj_text); //ime in država kraja
        TextView kraj_stopinje = (TextView) findViewById(R.id.kraj_stopinje); // temperatura
        ImageView ikona_vremena = (ImageView) findViewById(R.id.ikona_vremena); // ikona vremena (sonček, oblaki ...)
        TextView opis = (TextView) findViewById(R.id.opis); // preprost opis (Jasno, Sneži, ...)
        TextView zdise = (TextView) findViewById(R.id.zdise); // RealFeel tehnologija
        TextView minmax = (TextView) findViewById(R.id.minmax); // Minimalne in maksimalne izmerjene temperature

        // Nalaganje podatkov iz shrambe
        loadFromStorage(kraj_text, kraj_stopinje, ikona_vremena);

        // DEBUG -> čas
        System.out.println(getTimestamp());

        // Handler za pomoč
        help_me.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Funkcija odpiranja pomoči
                openHelpMe();
            }
        });

        // Handler za klik gumba za poizvedovanje
        search_img.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                
                // Shrani podatke o trenutnem času za podatek o zadnjem osveževanju
                try {
                    saveTimestamp(getTimestamp());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Gumb za iskanje se skrije in gumb nalaganja se prikaže
                search_img.setVisibility(View.INVISIBLE);
                nalaganje.setVisibility(View.VISIBLE);

                // Pridobitev vnosa in pošiljanje handlerju za pridobitev podatkov o tem kraju
                String query = kraj_search.getText().toString();
                getWeatherData(query, kraj_text, kraj_stopinje, ikona_vremena, false);
            }
        });

        // handler ki simulira klik gumba za poizvedovanje ko kliknemo enter na tipkovnici
        kraj_search.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    search_img.performClick();
                    return true;
                }
                return false;
            }
        });

        // Handler za osveževanje podatkov 
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                // Shrani čas osveževanja
                try {
                    saveTimestamp(getTimestamp());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                // handler za vrtalko osveževanja (true -> začni vrteti; false -> ustavi vrtenje)
                refreshImg(true);
                
                // pridobi podatke o vremenu
                try {
                    // išče poizvedbo že shranjenega imena kraja kar v spremenljivki imena in držve kraja
                    getWeatherData(kraj_text.getText().toString(), kraj_text, kraj_stopinje, ikona_vremena, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // odpri nov activity s pomočjo
    public void openHelpMe(){
        Intent intent = new Intent(this, HelpMe.class);
        startActivity(intent);
    }

    // vrtalka osveževanja 
    public void refreshImg(boolean b){
        ImageView refresh = (ImageView) findViewById(R.id.refresh); // pridobi objekt slikice vrtalke
        
        // rotacijske funkcije (priprava vrtacije; sama vrtacija se še ne začne)
        RotateAnimation rotate = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f); 
        rotate.setDuration(400);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        
        // če je true bo začel vrteti
        if (b){
            refresh.startAnimation(rotate);
        }
        
        // če je false se vrtacija konča
        else{
            refresh.clearAnimation();
            Toast.makeText(getApplicationContext(), "Uspešno osveženo!", Toast.LENGTH_SHORT).show();
        }
    }

    // Funkcija za dejansko spreminjanje podatko prikazanih na UI
    public void changeWeather(JsonObject finalResponse, String responseString, TextView kraj_text, TextView kraj_stopinje, ImageView ikona_vremena, boolean is_refreshing){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Nastavitev kraja in države
                String kraj = finalResponse.get("name").getAsString();
                JsonObject drzava1 = (JsonObject) finalResponse.get("sys");
                String drzava = drzava1.get("country").getAsString();

                // Izjeme imen, ker obstajajo napake v bazi podatkov ponudnika in LAŽEJO
                switch(kraj){
                    case "Trieste":
                        kraj = "Trst";
                        drzava = "SI";
                        break;
                    case "Province of Gorizia":
                        kraj = "Gorica";
                        drzava = "SI";
                        break;
                    case "Klagenfurt":
                        kraj = "Celovec";
                        drzava = "SI";
                        break;
                    default :
                        break;
                }

                kraj_text.setText(kraj + ", " + drzava);

                // Nastavitev temperature
                JsonObject obj = (JsonObject) finalResponse.get("main");
                double temperature = obj.get("temp").getAsDouble();

                // Legacy koda. Včasih je bla temperatura v decimalkah. Nič več
                //DecimalFormat formatter = new DecimalFormat("#0.0");
                //String s = (formatter.format(temperature));

                String s = "" + (int)temperature;
                kraj_stopinje.setText(s + Html.fromHtml("°"));

                // Nastavitev vremenske slikice/ikone
                JsonArray iconArr = finalResponse.getAsJsonArray("weather");
                JsonObject iconObj = (JsonObject) iconArr.get(0);
                String ikona = iconObj.get("icon").getAsString();
                ikona = "_" + ikona;

                Context context = ikona_vremena.getContext();
                int id = context.getResources().getIdentifier(ikona, "drawable", context.getPackageName());
                ikona_vremena.setImageResource(id);

                // Nastavitev opisa
                TextView opis = (TextView) findViewById(R.id.opis);
                String stanje = iconObj.get("main").getAsString();

                //prevajalna tabela
                switch(stanje){
                    case "Fog":
                    case "Mist":
                        stanje = "Megla";
                        break;
                    case "Clear":
                        stanje = "Jasno";
                        break;
                    case "Rain":
                        stanje = "Dežuje";
                        break;
                    case "Clouds":
                        stanje = "Oblačno";
                        break;
                    case "Thunderstorm":
                        stanje = "Nevihta";
                        break;
                    case "Drizzle":
                        stanje = "Kaplja";
                        break;
                    case "Snow":
                        stanje = "Sneži";
                        break;
                    case "Smoke":
                        stanje = "Dim";
                        break;
                    case "Haze":
                        stanje = "Smog";
                        break;
                    case "Dust":
                    case "Sand":
                        stanje = "Pesek";
                        break;
                    default:
                        stanje = stanje + " (Ni prevoda)";
                }

                opis.setText(stanje);

                // Nastavitev Zdi Se kot IN Min/Max temp.
                TextView zdise = (TextView) findViewById(R.id.zdise);
                TextView minmax = (TextView) findViewById(R.id.minmax);

                int feels_like = obj.get("feels_like").getAsInt();
                int min = obj.get("temp_min").getAsInt();
                int max = obj.get("temp_max").getAsInt();

                zdise.setText("RealFeel: " + feels_like + Html.fromHtml("°"));
                minmax.setText(min + "" + Html.fromHtml("°") + "  /  " + max + "" + Html.fromHtml("°"));

                // KONEC SIMBOLA ZA NALAGANJE
                ImageView search_img = (ImageView) findViewById(R.id.search_img);
                ProgressBar nalaganje = (ProgressBar) findViewById(R.id.nalaganje);
                search_img.setVisibility(View.VISIBLE);
                nalaganje.setVisibility(View.INVISIBLE);

                // Če se osvežuje bo to ustavilo vrtenje simbola osveževanja in poslalo Toast
                if (is_refreshing) {
                    refreshImg(false);
                }

                // Shranjevanje datuma osveževanja/nalaganja
                TextView nazadnje = (TextView) findViewById(R.id.nazadnje);
                try {
                    nazadnje.setText("Nazadnje osveženo: " + loadTimestamp());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // SHRANJEVANJE V BAZO
                String json = responseString;

                try {
                    saveData(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // DEBUG
                try {
                    System.out.println("LOADED --> " + loadData());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Funkcija ki poiveduje o podatkih vremena
    public void getWeatherData(String query, TextView kraj_text, TextView kraj_stopinje, ImageView ikona_vremena, boolean is_refreshing){

        // sprazni iskalno polje
        EditText kraj_search = (EditText) findViewById(R.id.kraj_search);
        kraj_search.setText("");

        // nova nit, da aplikacije ne zmrne med iskanjem po internetu
        Thread newThread = new Thread(() -> {

            // API kliuč
            String api_key = "a00b1048e2b6dcedf11f5cd552411546";
            String api_url = "https://api.openweathermap.org/data/2.5/weather?q=" + query + "&appid=" + api_key + "&units=metric";

            // Poizveduj
            try {
                URL url = new URL(api_url);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                //Getting the response code
                int responsecode = conn.getResponseCode();

                // 404 Not Found (kraja ni bili mogoče najti)
                if (responsecode == 404){
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Tega kraja ni bilo mogoče najti", Toast.LENGTH_SHORT).show();

                            // KONEC SIMBOLA ZA NALAGANJE
                            ImageView search_img = (ImageView) findViewById(R.id.search_img);
                            ProgressBar nalaganje = (ProgressBar) findViewById(R.id.nalaganje);
                            search_img.setVisibility(View.VISIBLE);
                            nalaganje.setVisibility(View.INVISIBLE);

                            if (is_refreshing) {
                                refreshImg(false);
                            }
                        }
                    });

                }
                // Napaka pri poivedovanju (s strani strežnika)
                else if (responsecode != 200){
                    System.out.println("Napaka pri API povezavi 2 -> HTTP CODE: " + responsecode);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Neuspešno. Ali ste vnesli pravilne podatke? Koda: " + responsecode, Toast.LENGTH_LONG).show();
                            
                            // KONEC SIMBOLA ZA NALAGANJE
                            ImageView search_img = (ImageView) findViewById(R.id.search_img);
                            ProgressBar nalaganje = (ProgressBar) findViewById(R.id.nalaganje);
                            search_img.setVisibility(View.VISIBLE);
                            nalaganje.setVisibility(View.INVISIBLE);

                            // Če se je osveževalo naj se vrtalka osveževanja neha vrteti
                            if (is_refreshing) {
                                refreshImg(false);
                            }
                        }
                    });
                }
                
                // Koda 200 OK
                else {

                    String inline = "";
                    Scanner scanner = new Scanner(url.openStream());

                    // JSON -> String (Scanner)
                    while (scanner.hasNext()) {
                        inline += scanner.nextLine();
                    }
                    scanner.close();

                    //String -> JseonObject

                    String json = inline;
                    JsonObject response = new JsonParser().parse(json).getAsJsonObject();
                    System.out.println(response);

                    // Funkcija za sprembmo podatkov na UI
                    changeWeather(response, json, kraj_text, kraj_stopinje, ikona_vremena, is_refreshing);

                }
                // zaključi povezavo
                conn.disconnect();
            }
            catch(IOException e){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Napaka. Ali internet deluje?", Toast.LENGTH_SHORT).show();

                        // KONEC SIMBOLA ZA NALAGANJE
                        ImageView search_img = (ImageView) findViewById(R.id.search_img);
                        ProgressBar nalaganje = (ProgressBar) findViewById(R.id.nalaganje);
                        search_img.setVisibility(View.VISIBLE);
                        nalaganje.setVisibility(View.INVISIBLE);
                        if (is_refreshing) {
                            refreshImg(false);
                        }
                    }
                });
                System.out.println(e.getMessage());
            }
        });
        newThread.start();

    }

    // Funkcija za shranjevanje podatkov na telefon
    public void saveData(String data) throws Exception {

        FileOutputStream fos = openFileOutput("savedWeatherData", Context.MODE_PRIVATE);
        fos.write(data.getBytes());
        fos.close();

    }

    // Funkcija za pridobitev shranjenih podatkov
    public String loadData() throws Exception{

        try{
            FileInputStream fis = openFileInput("savedWeatherData");
            Scanner scanner = new Scanner(fis);
            scanner.useDelimiter("\\Z");
            String content = scanner.next();
            scanner.close();
            return content;
        }
        // če shranjenih podatkov ni pripravimo "Dummy" Podatke
        catch(FileNotFoundException e){
            String s = "{\n" +
                    "  \"coord\": {\n" +
                    "    \"lon\": 15.49,\n" +
                    "    \"lat\": 45.96\n" +
                    "  },\n" +
                    "  \"weather\": [\n" +
                    "    {\n" +
                    "      \"id\": 801,\n" +
                    "      \"main\": \"Clouds\",\n" +
                    "      \"description\": \"few clouds\",\n" +
                    "      \"icon\": \"02d\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"base\": \"stations\",\n" +
                    "  \"main\": {\n" +
                    "    \"temp\": 0,\n" +
                    "    \"feels_like\": 0,\n" +
                    "    \"temp_min\": 0,\n" +
                    "    \"temp_max\": 0,\n" +
                    "    \"pressure\": 1029,\n" +
                    "    \"humidity\": 63\n" +
                    "  },\n" +
                    "  \"visibility\": 10000,\n" +
                    "  \"wind\": {\n" +
                    "    \"speed\": 2.6,\n" +
                    "    \"deg\": 50\n" +
                    "  },\n" +
                    "  \"clouds\": {\n" +
                    "    \"all\": 20\n" +
                    "  },\n" +
                    "  \"dt\": 1606223646,\n" +
                    "  \"sys\": {\n" +
                    "    \"type\": 1,\n" +
                    "    \"id\": 6816,\n" +
                    "    \"country\": \"Prosim, poiščite kakšen kraj, da pričnete.\",\n" +
                    "    \"sunrise\": 1606198268,\n" +
                    "    \"sunset\": 1606231133\n" +
                    "  },\n" +
                    "  \"timezone\": 3600,\n" +
                    "  \"id\": 3197147,\n" +
                    "  \"name\": \"Prazno\",\n" +
                    "  \"cod\": 200\n" +
                    "}";
            return s;
        }

    }

    // nalaganje podatkov iz shrambe in pošiljanje po posodobitvi podatkov
    public void loadFromStorage(TextView kraj_text, TextView kraj_stopinje, ImageView ikona_vremena){
        try {
            String content = loadData();
            JsonObject jsonContent = new JsonParser().parse(content).getAsJsonObject();
            changeWeather(jsonContent, content, kraj_text, kraj_stopinje, ikona_vremena, false);
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    // shrani čas osvežitve
    public void saveTimestamp(String stamp) throws Exception {
        FileOutputStream fos = openFileOutput("savedTimestamp", Context.MODE_PRIVATE);
        fos.write(stamp.getBytes());
        fos.close();
    }

    // pridobitev časa osvežitve iz shranjenih podatkov
    public String loadTimestamp() throws Exception{
        try{
            FileInputStream fis = openFileInput("savedTimestamp");
            Scanner scanner = new Scanner(fis);
            scanner.useDelimiter("\\Z");
            String content = scanner.next();
            scanner.close();
            return content;
        }
        catch(FileNotFoundException e){
            return "Nikoli";
        }
    }

    // pridobitev časa
    @SuppressLint("SimpleDateFormat")
    public String getTimestamp(){
        return new SimpleDateFormat("dd.MM.yyyy, HH:mm").format(Calendar.getInstance().getTime());
    }
}
