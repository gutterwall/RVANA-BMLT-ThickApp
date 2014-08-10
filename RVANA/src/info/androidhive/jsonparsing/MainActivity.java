package info.androidhive.jsonparsing;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;
import android.content.SharedPreferences;


import android.content.ActivityNotFoundException;

import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;



public class MainActivity extends ListActivity  {

    private ProgressDialog pDialog;

    // URL to get meetings JSON
    //private static String url = "http://api.androidhive.info/meetings/";
    private static String url = "http://metrorichna.org/BMLT/main_server/client_interface/json/?switcher=GetSearchResults";
    // JSON Node names
    private static final String TAG_ID = "id_bigint";
    private static final String TAG_NAME = "meeting_name";
    private static final String TAG_FORMATS = "formats";
    private static final String TAG_START = "start_time";
    private static final String TAG_DURATION = "duration_time";
    private static final String TAG_CITY = "location_municipality";
    private static final String TAG_STATE = "location_province";
    private static final String TAG_ADDRESS = "location_street";
    private static final String TAG_LOCATION = "location_text";
    private static final String TAG_ZIP = "location_postal_code_1";
    private static final String TAG_WEEKDAY = "weekday_tinyint";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_MAPADDRESS = "map_address";

    // meetings JSONArray
    //JSONArray meetings = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> meetingList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.list_days);

        setContentView(R.layout.activity_main);

        meetingList = new ArrayList<HashMap<String, String>>();

        //ListView lv = (ListView) findViewById(R.id.meeting_list);


        ListView lv = getListView();

        // Listview on item click listener

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // getting values from selected ListItem
                String name = ((TextView) view.findViewById(R.id.mapaddress))
                        .getText().toString();
                String longitude = ((TextView) view.findViewById(R.id.longitude))
                        .getText().toString();
                String latitude = ((TextView) view.findViewById(R.id.latitude))
                        .getText().toString();


                //Toast.makeText(getApplicationContext(), "Opening " + name + " in Google Maps", Toast.LENGTH_SHORT).show();
                /*
                String uri = String.format(Locale.ENGLISH, "geo:%s,%s", latitude, longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
                */
                String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?&daddr=%s,%s (%s)",  latitude, longitude, name);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                try
                {
                    startActivity(intent);
                }
                catch(ActivityNotFoundException ex)
                {
                    try
                    {
                        Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        startActivity(unrestrictedIntent);
                    }
                    catch(ActivityNotFoundException innerEx)
                    {
                        Toast.makeText(getApplicationContext(), "Please install a maps application", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

        Log.w("myApp","Before Contacts");
        new GetContacts().execute();
        Log.w("myApp","After Getting Contacts");


    }



    /**
     * Async task class to get json by making HTTP call
     */
    private class GetContacts extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }


        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);

            Log.d("Response: ", "> " + jsonStr);
            String[] weekdays = new DateFormatSymbols().getWeekdays();
            String lastDay = "";
            if (jsonStr != null) {
                SharedPreferences settings = getSharedPreferences("RVANA",android.content.Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString( "jsondata", jsonStr);
                editor.commit();
            } else {

                SharedPreferences sharedPref = getSharedPreferences("RVANA", android.content.Context.MODE_PRIVATE);
                jsonStr = sharedPref.getString("jasondata","" );
                Log.w("Response: prefs ", "> " + jsonStr);
            }

            if (jsonStr != null) {
                try {
                    //JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray meetings = new JSONArray(jsonStr);

                    // looping through All Contacts
                    for (int i = 0; i < meetings.length(); i++) {
                        JSONObject c = meetings.getJSONObject(i);

                        //String id = c.getString(TAG_ID);
                        String name = c.getString(TAG_NAME);
                        //String formats = c.getString(TAG_FORMATS);
                        //Log.w("myApp", name);

                        String start_time;
                        if (c.isNull(TAG_START)) {
                            start_time = " ";
                        } else {
                            start_time = c.getString(TAG_START);
                        }

                        String formats;
                        if (c.isNull(TAG_FORMATS)) {
                            formats = " ";
                        } else {
                            formats = c.getString(TAG_FORMATS);
                        }
                        String address;
                        if (c.isNull(TAG_ADDRESS)) {
                            address = " ";
                        } else {
                            address = c.getString(TAG_ADDRESS);
                        }
                        String city;
                        if (c.isNull(TAG_CITY)) {
                            city = " ";
                        } else {
                            city = c.getString(TAG_CITY);
                        }
                        String state;
                        if (c.isNull(TAG_STATE)) {
                            state = "VA";
                        } else {
                            state = c.getString(TAG_STATE);
                        }

                        String zip;
                        if (c.isNull(TAG_ZIP)) {
                            zip = " ";
                        } else {
                            zip = c.getString(TAG_ZIP);
                        }

                        String location;
                        if (c.isNull(TAG_LOCATION)){
                            location = " ";
                        } else {
                            location = c.getString(TAG_LOCATION);
                        }

                        String weekday;
                        if (c.isNull(TAG_WEEKDAY)) {
                            weekday = " ";
                        } else {
                            weekday = c.getString(TAG_WEEKDAY);
                        }

                        String duration;
                        if (c.isNull(TAG_DURATION)) {
                            duration = " ";
                        } else {
                            duration = c.getString(TAG_DURATION);
                        }



                        // tmp hashmap for single contact
                        HashMap<String, String> contact = new HashMap<String, String>();

                        String dayOfWeek;
                        // adding each child node to HashMap key => value
                        //contact.put(TAG_ID, id);
                        if (weekday.equals("1")) {
                            dayOfWeek = "Sunday";
                        } else if (weekday.equals("2")) {
                            dayOfWeek = "Monday";
                        } else if (weekday.equals("3")) {
                            dayOfWeek = "Tuesday";
                        } else if (weekday.equals("4")) {
                            dayOfWeek = "Wednesday";
                        } else if (weekday.equals("5")) {
                            dayOfWeek = "Thursday";
                        } else if (weekday.equals("6")) {
                            dayOfWeek = "Friday";
                        } else if (weekday.equals("7")) {
                            dayOfWeek = "Saturday";
                        } else {
                            dayOfWeek = "Sunday";
                        }

                        if (lastDay.equals(weekday)) {
                            contact.put(TAG_WEEKDAY, " ");
                        } else {
                            contact.put(TAG_WEEKDAY, dayOfWeek);
                            lastDay = weekday;
                        }


                        String longitude;
                        if (c.isNull(TAG_LONGITUDE)){
                            longitude = "0";
                        } else {
                            longitude = c.getString(TAG_LONGITUDE);
                        }

                        String latitude;
                        if (c.isNull(TAG_LATITUDE)){
                            latitude = "0";
                        } else {
                            latitude = c.getString(TAG_LATITUDE);
                        }

                        contact.put(TAG_NAME, name);
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                            final java.util.Date start_time_date = sdf.parse(start_time);
                            LocalTime test = LocalTime.fromDateFields(start_time_date);
                            PeriodFormatter formatter = new org.joda.time.format.PeriodFormatterBuilder()
                                    .appendHours()
                                    .appendSeparator(":")
                                    .appendMinutes()
                                    .appendSeparator(":")
                                    .appendSeconds()
                                    .toFormatter();
                            Period myDuration =  formatter.parsePeriod(duration);
                            LocalTime end_time = test.plus(myDuration);
                            java.util.Date da = end_time.toDateTimeToday().toDate();
                            String conv_start_time = new SimpleDateFormat("h:mma").format(start_time_date);
                            String conv_end_time = new SimpleDateFormat("h:mma").format(da);

                            contact.put(TAG_START, conv_start_time + " to " + conv_end_time);
                        } catch (final java.text.ParseException e)
                            {
                                e.printStackTrace();
                                contact.put(TAG_START,start_time + " For " + duration + " Hours");
                        }


                        contact.put(TAG_FORMATS, formats);
                        contact.put(TAG_LOCATION,location);
                        contact.put(TAG_ADDRESS, address);
                        contact.put(TAG_CITY, city + ", " + state + " " + zip);
                        String mapaddress = location + ", " + address + ", " + city + ", " + state + " " + zip;
                        contact.put(TAG_MAPADDRESS, mapaddress);
                        contact.put(TAG_LONGITUDE,longitude);
                        contact.put(TAG_LATITUDE, latitude);
                        //Log.w("myApp", weekday);
                        // adding contact to contact list
                        meetingList.add(contact);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            Log.w("myApp","Almost Done");
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, meetingList,
                    R.layout.list_item, new String[]{TAG_WEEKDAY, TAG_NAME, TAG_START, TAG_FORMATS, TAG_LOCATION, TAG_ADDRESS, TAG_CITY,TAG_MAPADDRESS, TAG_LONGITUDE,TAG_LATITUDE},
                    new int[]{R.id.day, R.id.name, R.id.start_time, R.id.formats, R.id.location, R.id.address, R.id.citystatezip, R.id.mapaddress, R.id.longitude, R.id.latitude});
            Log.w("myApp", "Down Here!");
            setListAdapter(adapter);
            //ListView lv = getListView();


        }

    }
}