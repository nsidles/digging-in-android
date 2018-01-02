package ubc.diggingin;

/*
* Android Smart WebView is an Open Source.
* Developed by Ghazi Khan (http://mgks.infeeds.com) under MIT Open Source License.
* This program is free to use for private and commercial purposes.
* As long as the source is mentioned under application's License Wiki.
* Please do not remove these comments, As the author has licensed it under such terms.
* For queries, email: getmgks@gmail.com
*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SiteActivity extends AppCompatActivity {

    //Permission variables
    static boolean ASWP_JSCRIPT     = true;     //enable JavaScript for webview
    static boolean ASWP_FUPLOAD     = true;     //upload file from webview
    static boolean ASWP_CAMUPLOAD   = true;     //enable upload from camera for photos
    static boolean ASWP_LOCATION    = true;     //track GPS locations
    static boolean ASWP_PBAR        = true;     //show progress bar in app
    static boolean ASWP_ZOOM        = false;    //zoom in webview
    static boolean ASWP_SFORM       = false;    //save form cache and auto-fill information
    static boolean ASWP_OFFLINE		= true;		//whether the loading webpages are offline or online
    static boolean ASWP_EXTURL		= true;		//open external url with default browser instead of app webview

    //Configuration variables
    private static String ASWV_URL      = "http://sidl.es/testing/testingdigging-in/?di_point_view="; //complete URL of your website or webpage
    private static String ASWV_F_TYPE   = "*/*";  //to upload any file type using "*/*"; check file type references for more
    public static String ASWV_HOST		= aswm_host(ASWV_URL);

    //Careful with these variable names if altering
    WebView asw_view;
    ProgressBar asw_progress;
    TextView asw_loading_text;

    private String asw_cam_message;
    private ValueCallback<Uri> asw_file_message;
    private ValueCallback<Uri[]> asw_file_path;
    private final static int asw_file_req = 1;

    private final static int loc_perm = 1;
    private final static int file_perm = 2;

    private SecureRandom random = new SecureRandom();

    private static final String TAG = "Digging In";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == asw_file_req) {
                    if (null == asw_file_path) {
                        return;
                    }
                    if (intent == null) {
                        if (asw_cam_message != null) {
                            results = new Uri[]{Uri.parse(asw_cam_message)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{ Uri.parse(dataString) };
                        }
                    }
                }
            }
            asw_file_path.onReceiveValue(results);
            asw_file_path = null;
        } else {
            if (requestCode == asw_file_req) {
                if (null == asw_file_message) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                asw_file_message.onReceiveValue(result);
                asw_file_message = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site);

        if (ASWP_PBAR) {
            asw_progress = (ProgressBar) findViewById(R.id.msw_progress);
        } else {
            findViewById(R.id.msw_progress).setVisibility(View.GONE);
        }
        asw_loading_text = (TextView) findViewById(R.id.msw_loading_text);

        asw_view = (WebView) findViewById(R.id.msw_view);

        //Webview settings; defaults are customized for best performance
        WebSettings webSettings = asw_view.getSettings();


        if(!ASWP_OFFLINE){
            webSettings.setJavaScriptEnabled(ASWP_JSCRIPT);
        }
        webSettings.setSaveFormData(ASWP_SFORM);
        webSettings.setSupportZoom(ASWP_ZOOM);
        webSettings.setGeolocationEnabled(ASWP_LOCATION);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } else if (Build.VERSION.SDK_INT >= 19) {
            asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 19) {
            asw_view.requestFocus();
            asw_view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        asw_view.setVerticalScrollBarEnabled(false);
        asw_view.setWebViewClient(new Callback());

        //Rendering the default URL
        Intent testIntent = getIntent();
        String message = testIntent.getStringExtra(MapsActivity.test);
        aswm_view(ASWV_URL + message, false);

        asw_view.setWebChromeClient(new WebChromeClient() {
            //Handling input[type="file"] requests for android API 16+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                if(ASWP_FUPLOAD) {
                    asw_file_message = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(ASWV_F_TYPE);
                    startActivityForResult(Intent.createChooser(i, "File Chooser"), asw_file_req);
                }
            }
            //Handling input[type="file"] requests for android API 21+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,WebChromeClient.FileChooserParams fileChooserParams){
                get_file();
                if(ASWP_FUPLOAD) {
                    if (asw_file_path != null) {
                        asw_file_path.onReceiveValue(null);
                    }
                    asw_file_path = filePathCallback;
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType(ASWV_F_TYPE);
                    Intent[] intentArray;
                    if (ASWP_CAMUPLOAD) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(SiteActivity.this.getPackageManager()) != null) {
                            File photoFile = null;
                            try {
                                photoFile = create_image();
                                takePictureIntent.putExtra("PhotoPath", asw_cam_message);
                            } catch (IOException ex) {
                                Log.e(TAG, "Image file creation failed", ex);
                            }
                            if (photoFile != null) {
                                asw_cam_message = "file:" + photoFile.getAbsolutePath();
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                            } else {
                                takePictureIntent = null;
                            }
                        }
                        if (takePictureIntent != null) {
                            intentArray = new Intent[]{takePictureIntent};
                        } else {
                            intentArray = new Intent[0];
                        }
                    } else {
                        intentArray = new Intent[0];
                    }
                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, asw_file_req);
                }
                return true;
            }

            //Getting webview rendering progress
            @Override
            public void onProgressChanged(WebView view, int p) {
                if (ASWP_PBAR) {
                    asw_progress.setProgress(p);
                    if (p == 100) {
                        asw_progress.setProgress(0);
                    }
                }
            }
        });
        if (getIntent().getData() != null) {
            String path     = getIntent().getDataString();
            /*
            If you want to check or use specific directories or schemes or hosts

            Uri data        = getIntent().getData();
            String scheme   = data.getScheme();
            String host     = data.getHost();
            List<String> pr = data.getPathSegments();
            String param1   = pr.get(0);
            */
            aswm_view(path, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Coloring the "recent apps" tab header; doing it onResume, as an insurance
        if (Build.VERSION.SDK_INT >= 23) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc = null;
            taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, getColor(R.color.colorPrimary));
            SiteActivity.this.setTaskDescription(taskDesc);
        }
    }

    //Setting activity layout visibility
    private class Callback extends WebViewClient {
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
        }

        public void onPageFinished(WebView view, String url) {
            findViewById(R.id.msw_welcome).setVisibility(View.GONE);
            findViewById(R.id.msw_view).setVisibility(View.VISIBLE);
        }
        //For android below API 23
        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getApplicationContext(), "Something Went Wrong!", Toast.LENGTH_SHORT).show();
            aswm_view("file:///android_res/raw/error.html", false);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            Toast.makeText(getApplicationContext(), "Something Went Wrong!", Toast.LENGTH_SHORT).show();
            aswm_view("file:///android_res/raw/error.html", false);
        }

        //Overriding webview URLs
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return url_actions(view, url);
        }

        //Overriding webview URLs for API 23+ [suggested by github.com/JakePou]
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return false;
        }
    }

    //Random ID creation function to help get fresh cache every-time webview reloaded
    public String random_id() {
        return new BigInteger(130, random).toString(32);
    }

    //Opening URLs inside webview with request
    void aswm_view(String url, Boolean tab) {
        if (tab) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } else {
            asw_view.loadUrl(url+"?rid="+random_id());
        }
    }

    //Actions based on shouldOverrideUrlLoading
    public boolean url_actions(WebView view, String url){
        boolean a = true;
        //Show toast error if not connected to the network
        if (!ASWP_OFFLINE ) {
            Toast.makeText(getApplicationContext(), "Please check your Network Connection!", Toast.LENGTH_SHORT).show();

            //Use this in a hyperlink to redirect back to default URL :: href="refresh:android"
        } else if (url.startsWith("refresh:")) {
            aswm_view(ASWV_URL, false);

            //Use this in a hyperlink to launch default phone dialer for specific number :: href="tel:+919876543210"
        } else if (url.startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            startActivity(intent);

            //Use this to open your apps page on google play store app :: href="rate:android"
        } else if (url.startsWith("rate:")) {
            final String app_package = getPackageName(); //requesting app package name from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app_package)));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app_package)));
            }

            //Sharing content from your webview to external apps :: href="share:URL" and remember to place the URL you want to share after share:___
        } else if (url.startsWith("share:")) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, view.getTitle());
            intent.putExtra(Intent.EXTRA_TEXT, view.getTitle()+"\nVisit: "+(Uri.parse(url).toString()).replace("share:",""));
            startActivity(Intent.createChooser(intent, "Share with your Friends"));

            //Use this in a hyperlink to exit your app :: href="exit:android"
        } else if (url.startsWith("exit:")) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            //Opening external URLs in android default web browser
        } else if (ASWP_EXTURL && aswm_host(ASWV_URL)!=ASWV_HOST) {
            aswm_view(url,true);

        } else {
            a = false;
        }
        return a;
    }

    //Getting host name
    public static String aswm_host(String url){
        if (url == null || url.length() == 0) {
            return "";
        }
        int dslash = url.indexOf("//");
        if (dslash == -1) {
            dslash = 0;
        } else {
            dslash += 2;
        }
        int end = url.indexOf('/', dslash);
        end = end >= 0 ? end : url.length();
        int port = url.indexOf(':', dslash);
        end = (port > 0 && port < end) ? port : end;
        Log.w("URL Host: ",url.substring(dslash, end));
        return url.substring(dslash, end);
    }

    //Getting device basic information
    public void get_info(){
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(ASWV_URL, "DEVICE=android");
        cookieManager.setCookie(ASWV_URL, "DEV_API=" + Build.VERSION.SDK_INT);
    }

    //Checking permission for storage and camera for writing and uploading images
    public void get_file(){
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

        //Checking for storage permission to write images for upload
        if (ASWP_FUPLOAD && ASWP_CAMUPLOAD && !check_permission(2) && !check_permission(3)) {
            ActivityCompat.requestPermissions(SiteActivity.this, perms, file_perm);

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (ASWP_FUPLOAD && !check_permission(2)) {
            ActivityCompat.requestPermissions(SiteActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, file_perm);

            //Checking for CAMERA permissions
        } else if (ASWP_CAMUPLOAD && !check_permission(3)) {
            ActivityCompat.requestPermissions(SiteActivity.this, new String[]{Manifest.permission.CAMERA}, file_perm);
        }
    }

    //Checking if particular permission is given or not
    public boolean check_permission(int permission){
        switch(permission){
            case 1:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            case 2:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            case 3:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        }
        return false;
    }

    //Creating image file for upload
    private File create_image() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".jpg", sd_directory);
    }

    //Action on back key tap/click
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (asw_view.canGoBack()) {
                        asw_view.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState ){
        super.onSaveInstanceState(outState);
        asw_view.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        asw_view.restoreState(savedInstanceState);
    }
}
