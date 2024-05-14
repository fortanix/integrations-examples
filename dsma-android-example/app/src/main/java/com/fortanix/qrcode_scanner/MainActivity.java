package com.fortanix.qrcode_scanner;

import android.Manifest;
import android.os.SystemClock;

import android.util.Log;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.content.Intent;
import android.app.AlertDialog;
import android.view.MenuInflater;
import androidx.annotation.NonNull;
import android.content.DialogInterface;
import androidx.core.app.ActivityCompat;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import com.fortanix.dsm.accelerator.*;


import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;
    AlertDialog.Builder builder;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static int isAppLaunch = 1;

    private static String FORTANIX_API_ENDPOINT = "<Endpoint URL>";
    private static String FORTANIX_API_KEY = "<API Key>";
    private static String FORTANIX_KEY_UUID = "<KEY UUID>";
    private static String FORTANIX_DSMA_CACHE_DURATION = "<Duration in Seconds>";


    DSMAccelerator client = new DSMAccelerator(FORTANIX_API_ENDPOINT, null);
    public MainActivity() throws DSMAcceleratorException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

        if (isAppLaunch == 1) {
            Toast.makeText(MainActivity.this,
                            "Please setup DSM resources, from Settings, at right top",
                            Toast.LENGTH_LONG)
                    .show();
        }

        mScannerView = new ZXingScannerView(this);
        setContentView(R.layout.activity_main);
        builder = new AlertDialog.Builder(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getString("fortanix_endpoint", null) == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("fortanix_endpoint", FORTANIX_API_ENDPOINT);
            editor.apply();
        }

        if(sharedPreferences.getString("apikey", null) == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("apikey", FORTANIX_API_KEY);
            editor.apply();
        }

        if(sharedPreferences.getString("key_name", null) == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("key_uuid", FORTANIX_KEY_UUID);
            editor.apply();
        }

        if(sharedPreferences.getString("cache_duration", null) == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("cache_duration", FORTANIX_DSMA_CACHE_DURATION);
            editor.apply();
        }

        try {
            InitializeDSMAccelerator(this.client);
        } catch (DSMAcceleratorException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Successfully Initialized DSMAccelerator");

        try {
            AuthenticateToDSM(this.client);
        } catch (DSMAcceleratorException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Successfully Authenticated to DSM");

        if (isAppLaunch == 1 ) {
            // Reset as this would be back event from settings
            isAppLaunch = 0;
        } else {
            String message = "Fortanix API ENDPOINT : " + sharedPreferences.getString("fortanix_endpoint", null);
            Toast.makeText(MainActivity.this,
                            message,
                            Toast.LENGTH_SHORT)
                    .show();
        }


        ViewGroup contentFrame = (ViewGroup) findViewById(R.id.content_frame);
        mScannerView = new ZXingScannerView(this);
        contentFrame.addView(mScannerView);
    }

    public void AuthenticateToDSM(DSMAccelerator client) throws DSMAcceleratorException {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        client.auth(sharedPreferences.getString("apikey", null));
    }

    public String Detokenize(String cipher) throws DSMAcceleratorException {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        try {
            Kid kid = new Kid(FORTANIX_KEY_UUID);

            DecryptResponse decryptResp = client.decrypt(DecryptRequest.builder()
                    .setKey(SobjectDescriptor.builder().setKid(kid.toString()).build())
                    .setCipher(cipher.getBytes())
                    .setAlg(Algorithm.AES)
                    .setMode(CipherMode.FPE)
                    .build());
            System.out.println("Plain Text : "+ new String(decryptResp.getPlain(), StandardCharsets.UTF_8));
            return new String(decryptResp.getPlain(), StandardCharsets.UTF_8);
        } catch (DSMAcceleratorException e) {
            throw new RuntimeException(e);
        }
    }

    public void InitializeDSMAccelerator(DSMAccelerator client) throws DSMAcceleratorException {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        if(sharedPreferences.getString("fortanix_endpoint", null) != null) {
            client = new DSMAccelerator(sharedPreferences
                    .getString("fortanix_endpoint", null), null, sharedPreferences.getString("cache_duration", null));
            client.clearCache();
        }

    }

    private void checkPermission(String permission, int cameraPermissionCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(
                MainActivity.this,
                permission)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            MainActivity.this,
                            new String[] { permission },
                            cameraPermissionCode);
        }
    }

    @Override
    public void handleResult(Result result) {
        String data = result.getText();
        Log.i("MAIN", "data: " + data);

        try {
            showMessageDialog(Detokenize(result.getText()));
        } catch (DSMAcceleratorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera(-1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    public void showMessageDialog(String message) {
        builder.setMessage(message)
                .setTitle(R.string.dialog_title)
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mScannerView.resumeCameraPreview(MainActivity.this);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mScannerView.resumeCameraPreview(MainActivity.this);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                                "Camera Permission Granted",
                                Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(MainActivity.this,
                                "Camera Permission Denied",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
}
