package de.hackzogtum_coburg.legion.hackzogapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainView extends AppCompatActivity implements View.OnClickListener{

    private Handler spaceStatHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String sDate = pref.getString("expDate", "n.a.");

        TextView textView = (TextView) findViewById(R.id.expDateTxt);
        textView.setText(sDate);

        this.spaceStatHandler = new Handler();

    }

    @Override
    public void onResume()
    {

        super.onResume();
        spaceStatHandler.removeCallbacks(updateSpaceStat);
        spaceStatHandler.postDelayed(updateSpaceStat, 100);

    }

    @Override
    public void onPause() {
        super.onPause();
        spaceStatHandler.removeCallbacks(updateSpaceStat);
    }

    private final Runnable updateSpaceStat = new Runnable() {
        @Override
        public void run() {
            try {
                AsyncTask<Void, Void, String> t = new GetSpaceStatTask();
                t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null)  ;

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            spaceStatHandler.postDelayed(updateSpaceStat, 1 * 60 * 1000);
        }
    };

    class GetSpaceStatTask extends AsyncTask<Void, Void, String>
    {

        @Override
        protected void onPostExecute(String result) {
            TextView spaceStatTxt = (TextView) findViewById(R.id.spaceStatTxt);
            TextView present = (TextView) findViewById(R.id.present);
            if(!result.isEmpty()) {
                spaceStatTxt.setText("Space is: open");
                present.setText("present: " + result);
            }
            else {
                present.setText("present: " + "");
                spaceStatTxt.setText("Space is: closed");
            }

        }

        protected String doInBackground(Void... voids) {

            HttpURLConnection con = null;
            try {
                URL url = new URL("https://in.hackzogtum-coburg.de/list.php");
                //URL url = new URL("http://mmisc.de/~legion/in4app/list.php");

                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                InputStream in = new BufferedInputStream(con.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                String content = "";
                String line;
                while ((line = br.readLine()) != null) {
                    content += line;
                }

                content = content.replaceAll("<br>", " ");

                con.disconnect();
                con = null;

                return content;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    con.disconnect();
                    return "";
                }
            }

            return "";

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.menu_open_close, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        // if (id == R.id.action_settings) {
        //     return true;
        // }

        return super.onOptionsItemSelected(item);
    }

    public void btnOpenClick(View v)
    {

        final Context ctx = getApplicationContext();
        final MainView a = this;
        Executors.newSingleThreadExecutor().execute(new Runnable()
        {
            @Override
            public void run()
            {
                callDoor(true,a, ctx);
            }
        });

    }
    public void btnCloseClick(View v)
    {
        final Context ctx = getApplicationContext();
        final MainView a = this;
        Executors.newSingleThreadExecutor().execute(new Runnable()
        {
            @Override
            public void run()
            {
                callDoor(false, a, ctx);
            }
        });
    }

    public void showMsg(String msg) {

        final String fmsg = msg;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainView.this, fmsg, Toast.LENGTH_LONG).show();
            }
        });

    }

    public static void callDoor(Boolean open, MainView a, Context ctx)
    {
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {

                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        try {


            CustomKeyManager myKM = new CustomKeyManager(a, ctx);

            SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
            sslCtx.init(new KeyManager[] {myKM}, trustAllCerts, null);

            String urlString;
            if(open)
                //urlString = "https://gate.hackzogtum-coburg.de/frontend/api/door?action=open";
                urlString = "https://door/frontend/api/door?action=open";
            else
                //urlString = "https://gate.hackzogtum-coburg.de/frontend/api/door?action=close";
                urlString = "https://door/frontend/api/door?action=close";


            // HttpsURLConnection.setDefaultSSLSocketFactory(NoSSLv3Factory);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslCtx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            URL url = new URL(urlString);
            HttpsURLConnection urlConnection;
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                String content = "";
                String line;
                while((line = br.readLine()) != null)
                {
                    content += line;
                }

                if(!content.contains("performed"))
                {
                    a.showMsg("given cert not working - i will forget it - try again");
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
                    SharedPreferences.Editor edit = pref.edit();
                    edit.remove("certAlias");
                    edit.commit();
                }
                a.showMsg(content);


            } finally {
                urlConnection.disconnect();
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

    }


    @Override
    public void onClick(View view) {    }

}




