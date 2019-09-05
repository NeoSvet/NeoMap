package ru.neosvet.neomap;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class MarkersActivity extends AppCompatActivity {
    private ListView lvMarkers;
    private View fabEdit;
    private ArrayAdapter<String> adMarkers;
    private List<Double> mLat = new ArrayList<Double>();
    private List<Double> mLng = new ArrayList<Double>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markers);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fabEdit = findViewById(R.id.fabEdit);
        initList();
        loadList();
        initPermission();

//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    private void initList() {
        List<String> liMarkers = new ArrayList<String>();
        adMarkers = new ArrayAdapter<String>(MarkersActivity.this, android.R.layout.simple_list_item_1, liMarkers);
        lvMarkers = (ListView)findViewById(R.id.lvMarkers);
        lvMarkers.setAdapter(adMarkers);
    }

    private void loadList() {
        DataBase db = new DataBase(this);
        SQLiteDatabase sq = db.getWritableDatabase();
        Cursor cursor = sq.query(DataBase.TABLE, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            int iName = cursor.getColumnIndex(DataBase.NAME);
            int iLat = cursor.getColumnIndex(DataBase.LAT);
            int iLng = cursor.getColumnIndex(DataBase.LNG);
            do {
                adMarkers.add(cursor.getString(iName));
                mLat.add(cursor.getDouble(iLat));
                mLng.add(cursor.getDouble(iLng));
            } while (cursor.moveToNext());
        }
        db.close();
        adMarkers.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.markers_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item != null ? item.getItemId() : 0;
        String file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath() + "/neo_markers.txt";
        try {
            if (id == R.id.menu_export) {
                File f = new File(file);
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                for (int i = 0; i < mLat.size(); i++) {
                    bw.write("[");
                    bw.write(mLng.get(i).toString());
                    bw.write(",");
                    bw.write(mLat.get(i).toString());
                    bw.write(",\"");
                    bw.write(adMarkers.getItem(i));
                    bw.write("\",\"\"]");
                    bw.newLine();
                    bw.flush();
                }
                bw.close();
                Snackbar.make(fabEdit, getResources().getString(R.string.export_done),
                        Snackbar.LENGTH_LONG).show();
            } else if (id == R.id.menu_import) {
                File f = new File(file);
                if (!f.exists()) return false;
                BufferedReader br = new BufferedReader(new FileReader(f));
                DataBase db = new DataBase(this);
                SQLiteDatabase sq = db.getWritableDatabase();
                ContentValues cv;
                String s;
                int i;
                while ((s = br.readLine()) != null) {
                    i = s.indexOf((","));
                    cv = new ContentValues();
                    cv.put(DataBase.LNG, Double.parseDouble(s.substring(1, i)));
                    i++;
                    cv.put(DataBase.LAT, Double.parseDouble(s.substring(i, s.indexOf(",", i))));
                    i = s.indexOf(("\"")) + 1;
                    s = s.substring(i, s.indexOf("\"", i));
                    cv.put(DataBase.NAME, s);
                    int r = sq.update(DataBase.TABLE, cv, DataBase.NAME + " = ?", new String[]{s});
                    if (r == 0) // no update
                        sq.insert(DataBase.TABLE, null, cv);
                }
                br.close();
                db.close();
                Snackbar.make(fabEdit, getResources().getString(R.string.ready),
                        Snackbar.LENGTH_LONG).show();
                adMarkers.clear();
                mLat.clear();
                mLng.clear();
                loadList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(fabEdit, getResources().getString(R.string.error),
                    Snackbar.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //http://stackoverflow.com/questions/35484767/activitycompat-requestpermissions-not-showing-dialog-box
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Permission Allowed
        } else {
            //Permission Denied
            Snackbar.make(fabEdit, getResources().getString(R.string.storage_denied),
                    Snackbar.LENGTH_LONG).show();
        }
    }
}
