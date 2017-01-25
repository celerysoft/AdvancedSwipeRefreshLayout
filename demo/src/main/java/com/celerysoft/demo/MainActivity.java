package com.celerysoft.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View tvRecyclerView = findViewById(R.id.tv_recycler_view);
        tvRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RecyclerViewActivity.class));
            }
        });

        View tvRecyclerViewWithCustomHeader = findViewById(R.id.tv_recycler_view_with_custom_header);
        tvRecyclerViewWithCustomHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RecyclerViewActivity2.class));
            }
        });
    }
}
