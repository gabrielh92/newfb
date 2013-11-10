package com.example.cvloc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class FragmentMainMenuDummy extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main_menu_dummy);
    	
    }
    
    public void runExercise2(View v)
    {
        Intent intent = new Intent(FragmentMainMenuDummy.this, BasicMain.class);
        startActivity(intent);
    }
}
