package ru.neosvet.neomap;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

public class MarkersActivity extends AppCompatActivity {
    private ListView lvMarkers;
    private ArrayAdapter<String> adMarkers;
    private List<Double> mLat = new ArrayList<Double>();
    private List<Double> mLng = new ArrayList<Double>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markers);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initList();
        loadList();

//        FloatingActionButton fab = findViewById(R.id.fabEdit);
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
        if (id == R.id.menu_export) {

        } else if (id == R.id.menu_import) {

        }
        return super.onOptionsItemSelected(item);
    }
}
