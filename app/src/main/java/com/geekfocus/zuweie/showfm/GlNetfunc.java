package com.geekfocus.zuweie.showfm;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zuweie on 3/13/14.
 */
public class GlNetfunc {

    private static GlNetfunc ourInstance = new GlNetfunc();

    public static GlNetfunc getInstance() {
        return ourInstance;
    }

    private GlNetfunc() {}

    public String get (String url) throws ClientProtocolException, IOException {

        HttpGet request = new HttpGet(url);
        HttpResponse response = null;
        DefaultHttpClient httpclient = getHttpClient();
        response = httpclient.execute(request);
        retrieveHttpClient(httpclient);

        if(response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity resEntity =  response.getEntity();
            return (resEntity == null) ? null : EntityUtils.toString(resEntity, HTTP.UTF_8);
        }
        return "";
    }

    public String post (String url) {
        return null;
    }

    public DefaultHttpClient getHttpClient () {

        DefaultHttpClient httpclient;
        synchronized(clients){
            if (!clients.isEmpty()){
                httpclient = clients.remove(0);
            }else{
                httpclient = createDefautHttpClient();
            }
            return httpclient;
        }

    }
    public void retrieveHttpClient( DefaultHttpClient client) {
        synchronized (clients){
            clients.add(client);
        }
    }
    public DefaultHttpClient createDefautHttpClient () {

        HttpParams params = new BasicHttpParams();

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue(params, true);

        ConnManagerParams.setTimeout(params, 8000);
        HttpConnectionParams.setConnectionTimeout(params, 8000);
        HttpConnectionParams.setSoTimeout(params, 8000);

        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
        return new DefaultHttpClient(conMgr, params);
    }

    List<DefaultHttpClient> clients = new ArrayList<DefaultHttpClient>();
}