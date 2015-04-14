package com.prateek.gem;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.prateek.gem.views.CustomView;

public class A extends ActionBarActivity {

	private Button add = null;
	private LinearLayout fieldContainer = null;
	private Context context = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_a);
		
		context = this;
		
		add = (Button) findViewById(R.id.save);
		fieldContainer = (LinearLayout) findViewById(R.id.fieldContainer);
		
		add.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View view) {
				CustomView customView = new CustomView(context);
				fieldContainer.addView(customView.getView());
			}
		});
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu., menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
