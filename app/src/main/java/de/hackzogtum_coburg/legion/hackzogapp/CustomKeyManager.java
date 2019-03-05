package de.hackzogtum_coburg.legion.hackzogapp;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.X509KeyManager;

/**
 * Created by legion on 24.04.18.
 */

public class CustomKeyManager implements X509KeyManager, KeyChainAliasCallback {

    private Context ctx;
    private Activity act;
    private CountDownLatch latch = null;

    public CustomKeyManager(Activity act, Context ctx)
    {
        super();
        this.ctx = ctx;
        this.act = act;

    }

    public void alias(String s)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("certAlias", s);

        String expDate = this.getCertificateChain(s)[0].getNotAfter().toString();
        edit.putString("expDate", expDate);


        edit.commit();
        if(latch!=null)
            latch.countDown();
    }

    private String getAlias()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        String alias = pref.getString("certAlias", null);

        if(alias==null) {
            KeyChain.choosePrivateKeyAlias(this.act, this, null, null, null, -1, null);
            this.latch = new CountDownLatch(1);
            try {
                this.latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getAlias();
        }

        return alias;
    }

    public X509Certificate[] getCertificateChain(String alias)
    {
        X509Certificate[] chain = null;
        try{
            chain = KeyChain.getCertificateChain(this.ctx, alias);

        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (KeyChainException e) {
            e.printStackTrace();
        }
        finally {
            return chain;
        }
    }
    public PrivateKey getPrivateKey(String alias)
    {
        PrivateKey key = null;

        try {
            key = KeyChain.getPrivateKey(ctx, alias);
        }
        catch (KeyChainException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally
        {
            return key;
        }
    }
    public String[] getServerAliases(String keyType, Principal[] issuers)
    {
        return null;
    }
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
    {
        return this.getAlias();
    }
    public String[] getClientAliases(String keyType, Principal[] issuers)
    {
        return new String[] {this.getAlias()};
    }
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
    {
        return null;
    }

}
