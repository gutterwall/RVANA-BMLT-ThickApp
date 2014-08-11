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


    // Hashmap for ListView  (The SAUSAGE in the Making)
    ArrayList<HashMap<String, String>> meetingList;


    //HERE's the MAIN system call.  We start up here...

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //We starts our layout...
        setContentView(R.layout.activity_main);

        //We create our sausage
        meetingList = new ArrayList<HashMap<String, String>>();


        //We grab our bun
        ListView lv = getListView();

        // Listview on item click listener
        //We make the whole shebang clickable here.  So we clicks a meeting and it opens into google maps or Browser to maps.goog.ecom
        //With Location,Address,City,State,Zip description and longitude/latitude pointer.
        //Maybe make a LONG CLICK on v2
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // getting values from selected ListItem Object (Get the place location hidden fields)
                String name = ((TextView) view.findViewById(R.id.mapaddress))
                        .getText().toString();
                String longitude = ((TextView) view.findViewById(R.id.longitude))
                        .getText().toString();
                String latitude = ((TextView) view.findViewById(R.id.latitude))
                        .getText().toString();

                //Should I put the toast back?  It opens really quickly... On my Note 3...
                //Toast.makeText(getApplicationContext(), "Opening " + name + " in Google Maps", Toast.LENGTH_SHORT).show();

                //Here's the make a URL
                String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?&daddr=%s,%s (%s)",  latitude, longitude, name);
                //Here we warn Android
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                //Here we make the action specifier
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                try
                {
                    //GO FOR IT!
                    startActivity(intent);
                }
                catch(ActivityNotFoundException ex)
                {
                    try
                    {
                        //No google Maps installed, open a browser
                        Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        startActivity(unrestrictedIntent);
                    }
                    catch(ActivityNotFoundException innerEx)
                    {
                        //Failed a browser?  You Suck!
                        Toast.makeText(getApplicationContext(), "Please install a maps application", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

        Log.w("myApp","Before Contacts");
        //Grab all our meat and make a sausage!
        //Get all the meetings and put it into the List View...
        new GetContacts().execute();
        Log.w("myApp","After Getting Contacts");
        //End Main Loop

    }



    /**
     * Async task class to get json by making HTTP call
     * it formats the data and fills out the listview objects.
     */
    private class GetContacts extends AsyncTask<Void, Void, Void> {
        //this just opens a dialog box so we have user interface feedback
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        //this function gets the data and parses the data...
        //We don't care how long it takes, we waits...
        @Override
        protected Void doInBackground(Void... arg0) {

            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);

            //Starting the Parsing
            Log.d("Response: ", "> " + jsonStr);

            //This lastDay variable keeps the Weekday from displaying except on change...
            String lastDay = "";

            //Here's a not working attempt to use SharedPreferences to store the JSON offline for offline viewing...
            /*if (jsonStr != null) {
                SharedPreferences settings = getSharedPreferences("RVANA",android.content.Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString( "jsondata", jsonStr);
                editor.commit();
            } else {

                SharedPreferences sharedPref = getSharedPreferences("RVANA", android.content.Context.MODE_PRIVATE);
                jsonStr = sharedPref.getString("jasondata","" );
                Log.w("Response: prefs ", "> " + jsonStr);
            }
            */

            //WE GOT DATA, lets PARSE!
            if (jsonStr != null) {
                try {
                    //Since the data is already an Array object
                    // Getting JSON Array node
                    JSONArray meetings = new JSONArray(jsonStr);

                    // looping through All Contacts
                    // (Could we use this count to change user feedback?  or would that break Asynchronicity?
                    for (int i = 0; i < meetings.length(); i++) {

                        //Gimmie ONE MEETINGs worth of Data
                        JSONObject c = meetings.getJSONObject(i);

                        //Get the MeetingName
                        String name = c.getString(TAG_NAME);

                        //Start Time (Catch Nulls)
                        String start_time;
                        if (c.isNull(TAG_START)) {
                            start_time = " ";
                        } else {
                            start_time = c.getString(TAG_START);
                        }

                        //Meeting Format Strings (Catch Nulls)
                        String formats;
                        if (c.isNull(TAG_FORMATS)) {
                            formats = " ";
                        } else {
                            formats = c.getString(TAG_FORMATS);
                        }

                        //Meeting Address (Catch Nulls)
                        String address;
                        if (c.isNull(TAG_ADDRESS)) {
                            address = " ";
                        } else {
                            address = c.getString(TAG_ADDRESS);
                        }

                        //Meeting City (Catch Nulls)
                        String city;
                        if (c.isNull(TAG_CITY)) {
                            city = " ";
                        } else {
                            city = c.getString(TAG_CITY);
                        }

                        //Meeting State (Catch Nulls)
                        String state;
                        if (c.isNull(TAG_STATE)) {
                            state = "VA";
                        } else {
                            state = c.getString(TAG_STATE);
                        }

                        //Meeting Zipcode (Catch Nulls)
                        String zip;
                        if (c.isNull(TAG_ZIP)) {
                            zip = " ";
                        } else {
                            zip = c.getString(TAG_ZIP);
                        }

                        //Meeting Location (Hibbs Hall) (Catch Nulls)
                        String location;
                        if (c.isNull(TAG_LOCATION)){
                            location = " ";
                        } else {
                            location = c.getString(TAG_LOCATION);
                        }

                        //Meeting Weekday (Catch Nulls)
                        //this one we decode.  Maybe refactor into a function?
                        //Maybe redo all null checks into functions?
                        String weekday;
                        if (c.isNull(TAG_WEEKDAY)) {
                            weekday = " ";
                        } else {
                            weekday = c.getString(TAG_WEEKDAY);
                        }

                        //Meeting Duration (Catch Nulls)
                        // Duration is in time units (01:00:00) = 1 hour
                        String duration;
                        if (c.isNull(TAG_DURATION)) {
                            duration = " ";
                        } else {
                            duration = c.getString(TAG_DURATION);
                        }

                        // tmp hashmap for single contact  (This object we throw into the ARRAY)
                        HashMap<String, String> contact = new HashMap<String, String>();


                        //OMG day of week from Weekday numeral
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

                        //Here I PUT the data into the CONTACT object
                        if (lastDay.equals(weekday)) {
                            contact.put(TAG_WEEKDAY, " ");
                        } else {
                            contact.put(TAG_WEEKDAY, dayOfWeek);
                            lastDay = weekday;
                        }

                        //Here I PUT the data into the CONTACT object
                        String longitude;
                        if (c.isNull(TAG_LONGITUDE)){
                            longitude = "0";
                        } else {
                            longitude = c.getString(TAG_LONGITUDE);
                        }

                        //Here I PUT the data into the CONTACT object
                        String latitude;
                        if (c.isNull(TAG_LATITUDE)){
                            latitude = "0";
                        } else {
                            latitude = c.getString(TAG_LATITUDE);
                        }

                        //Here I PUT the data into the CONTACT object
                        contact.put(TAG_NAME, name);

                        //Here I PUT the Start and End Times into the CONTACT object
                        //I had to resort to using joda and java.util.date libs
                        //It was a complete pain in the ass
                        //So be careful, you break, you fix...
                        //First I import the strings from BMLT using SIMPLEDATEFORMAT into DATE
                        //Then I import the StartDate Time into LocalTime (joda)
                        //Then I append using a Period Formatter the duration as a PERIOD
                        //Then I use Joda to add the LocalTime StartTime now named test to the
                        //duration to get end_time.  then I have to convert the endTime to date again
                        //then I parse it out into human readable 12HR with am/pm
                        //LIKE I SAID, DON"T BREAK IT!!!!
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
                            //Here I put the Data into Contact...
                            contact.put(TAG_START, conv_start_time + " to " + conv_end_time);
                        } catch (final java.text.ParseException e)
                            {
                                //Or if it fails for not being a date, I just put what we have...
                                e.printStackTrace();
                                contact.put(TAG_START,start_time + " For " + duration + " Hours");
                        }

                        //Here I put the rest of the data into the Contact thingy.
                        //I add up stuff into one string where it makes sense....
                        contact.put(TAG_FORMATS, formats);
                        contact.put(TAG_LOCATION,location);
                        contact.put(TAG_ADDRESS, address);
                        contact.put(TAG_CITY, city + ", " + state + " " + zip);
                        //MapAddress is a hidden field for the Google Maps display (so we have a human readable search address)
                        String mapaddress = location + ", " + address + ", " + city + ", " + state + " " + zip;
                        contact.put(TAG_MAPADDRESS, mapaddress);
                        //This is raw coordinates.
                        contact.put(TAG_LONGITUDE,longitude);
                        contact.put(TAG_LATITUDE, latitude);
                        //Log.w("myApp", weekday);
                        // adding contact to contact list
                        //So we stuff the meat into the sausage...
                        meetingList.add(contact);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                //Maybe here's where I should put the DAMN CACHED CODE??!?
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            return null;
        }
        //This function gets called after the meetings are all loaded into the meetinglist object Array...
        //It pushes the data into the ListView object...
        //WE STUFF THE SAUSAGE INTO THE BUN!!!
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