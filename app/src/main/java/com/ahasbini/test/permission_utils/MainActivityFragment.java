package com.ahasbini.test.permission_utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = MainActivityFragment.class.getSimpleName();

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Here we're just logging the permission check for reference, should be denied when running
        // the tests.
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "onCreate: permission granted");
        } else {
            Log.i(TAG, "onCreate: permission denied");
        }

        view.findViewById(R.id.fShowSdCardB).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: called");
                if (ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    showSdCardLisDialog();
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            1);
                }
            }
        });

        return view;
    }

    private void showSdCardLisDialog() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // Getting list of files
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                final StringBuilder sdCardDir = new StringBuilder();
                for (File file : externalStorageDirectory.listFiles()) {
                    sdCardDir.append(file.getName()).append(file.isDirectory() ? "/\n" : "\n");
                }

                new Handler(getActivity().getMainLooper())
                        .post(new Runnable() {

                            @Override
                            public void run() {
                                // Displaying them in a dialog
                                new AlertDialog.Builder(getActivity())
                                        .setTitle("List")
                                        .setMessage(sdCardDir.toString())
                                        .setPositiveButton(android.R.string.ok, null)
                                        .setCancelable(false)
                                        .show();
                            }
                        });
            }
        }).start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: called");
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: permission granted");
                showSdCardLisDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
