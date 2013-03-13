/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.ds.avare.place.Destination;
import com.ds.avare.utils.Helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class AirportActivity extends Activity {
    
    private StorageService mService;
    private Destination mDestination;
    private ListView mAirport;
    private Toast mToast;
    private AfdView mAfdView;
    private Spinner mSpinner;
    private List<String> mList;
    private boolean mIgnoreFocus;

    /*
     * For being on tab this activity discards back to main activity
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity)this.getParent()).switchTab(0);
    }
    
    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.airport, null);
        setContentView(view);
        mAirport = (ListView)view.findViewById(R.id.airport_list);
        mAfdView = (AfdView)view.findViewById(R.id.airport_afd);
        mSpinner = (Spinner)view.findViewById(R.id.airport_spinner);
        mIgnoreFocus = true;

        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                if(mDestination != null && mService != null && mList != null) {
                    /*
                     * mIgnoreFocus will get rid of spinner position 0 call on onResume()
                     */
                    if(mIgnoreFocus) {
                        pos = mService.getAfdIndex();
                        mIgnoreFocus = false;
                    }
                    String[] afd = mDestination.getAfd();
                    if(afd != null) {
                        if(pos > afd.length) {
                            pos = 0;
                        }
                        mSpinner.setSelection(pos);
                        if(pos > 0) {
                            mService.loadDiagram(afd[pos - 1] + ".jpg");
                            mAfdView.setBitmap(mService.getDiagram());
                            /*
                             * Show graphics
                             */
                            mAirport.setVisibility(View.INVISIBLE);
                        }
                        else {
                            mAirport.setVisibility(View.VISIBLE);                            
                            mService.loadDiagram(null);
                            mAfdView.setBitmap(null);
                        }
                        mService.setAfdIndex(pos);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mService = null;
    }
    
    
    /** Defines callbacks for service binding, passed to bindService() */
    /**
     * 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            /* 
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();

            mList = new ArrayList<String>();
            mList.add(getApplicationContext().getString(R.string.AFD));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(AirportActivity.this,
                    android.R.layout.simple_spinner_item, mList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);

            /*
             * Now get all stored data
             */
            mDestination = mService.getDestination();
            if(null == mDestination) {
                mAfdView.setBitmap(null);
                mToast.setText(getString(R.string.ValidDest));
                mAirport.setVisibility(View.VISIBLE);
                mToast.show();
                return;
            }

            /*
             * Get Text A/FD
             */
            LinkedHashMap <String, String>map = mDestination.getParams();
            String[] views = new String[map.size()];
            String[] values = new String[map.size()];
            int iterator = 0;
            for(String key : map.keySet()){
                views[iterator] = key;
                values[iterator] = map.get(key);
                iterator++;
            }
            mAirport.setClickable(false);
            mAirport.setCacheColorHint(Color.WHITE);
            mAirport.setDividerHeight(10);
            mAirport.setBackgroundColor(Color.WHITE);
            mAirport.setAdapter(new TypeValueAdapter(AirportActivity.this, views, values));

            /*
             * Start adding graphical A/FD
             */
            
            /*
             * Not found
             */
            if((!mDestination.isFound()) || (mDestination.getAfd() == null)) {
                mAfdView.setBitmap(null);
                mAirport.setVisibility(View.VISIBLE);
                return;                
            }
            
            /*
             * Now add all A/FD pages to the list
             */
            String[] afd = mDestination.getAfd();            
            for(int plate = 0; plate < afd.length; plate++) {
                String tokens[] = afd[plate].split("/");
                mList.add(tokens[tokens.length - 1]);
            }
            
            /*
             * A list of A/FD available for this airport
             */
            adapter = new ArrayAdapter<String>(AirportActivity.this,
                    android.R.layout.simple_spinner_item, mList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);            
        }    

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);
    }

    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();
        
        /*
         * Registering our receiver
         * Bind now.
         */
        mIgnoreFocus = true;
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Helper.setOrientationAndOn(this);
    }

    /**
     * 
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
