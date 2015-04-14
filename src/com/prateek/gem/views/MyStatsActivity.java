package com.prateek.gem.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

import com.prateek.gem.AppConstants;
import com.prateek.gem.GEMApp;
import com.prateek.gem.R;
import com.prateek.gem.AppConstants.JSONConstants;
import com.prateek.gem.model.ExpenseOject;
import com.prateek.gem.model.GiverTakerObject;
import com.prateek.gem.model.Group;
import com.prateek.gem.model.SettlementObject;
import com.prateek.gem.persistence.DBAdapter;
import com.prateek.gem.utils.Utils;

public class MyStatsActivity extends ActionBarActivity {

	private TextView totalSpentView,totalSpentViewByMember,howmuchgetfromothers,howmuchgivetoothers; 
	private Context context;
	private DBAdapter db;
	private SharedPreferences preferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mystats);
		
		initUI();
		
		String memberName = preferences.getString(AppConstants.ADMIN_NAME, null);
		System.out.println("Member Name"+memberName);
		
		db.open();
		Float totalExpenseInGroup = db.getExpenseTotal(GEMApp.getInstance().getCurr_group().getGroupIdServer());
		GEMApp.getInstance().getCurr_group().setTotalOfExpense(totalExpenseInGroup);
		Float totalExpenseByMember = db.getExpenseTotalByMember(GEMApp.getInstance().getCurr_group().getGroupIdServer(), memberName);
		db.close();
		float percentage = (totalExpenseByMember/totalExpenseInGroup)*100;
		if(GEMApp.getInstance().getExpensesList() != null && GEMApp.getInstance().getExpensesList().size() != 0) {
			totalSpentView.setText(getString(R.string.inr)+" "+totalExpenseInGroup.toString());
			totalSpentViewByMember.setText(getString(R.string.inr)+" "+totalExpenseByMember.toString()+" -  "+Utils.round(percentage,2)+"%");
			Map<GiverTakerObject, Float> hisabMap = new HashMap<GiverTakerObject, Float>();
			JSONArray jsonArray;		
			try{
				for(ExpenseOject expenseObject:GEMApp.getInstance().getExpensesList()){
					jsonArray = new JSONArray(expenseObject.getParticipants());
					for(int i = 0;i<jsonArray.length();i++){
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						if(!expenseObject.getExpenseBy().equals(jsonObject.getString(JSONConstants.MEMBERNAME))){ // If expense by and participant are not same
							// for checking the to and fro kharcha(Choti-Ankur, Ankur-Choti)
							GiverTakerObject giverTakerObject = new GiverTakerObject(expenseObject.getExpenseBy(), jsonObject.getString(JSONConstants.MEMBERNAME));
							GiverTakerObject reverseObject = new GiverTakerObject(jsonObject.getString(JSONConstants.MEMBERNAME), expenseObject.getExpenseBy());							
							
							if(hisabMap.containsKey(reverseObject)){
								float newshare = hisabMap.get(reverseObject) - expenseObject.getShare();										
								hisabMap.put(reverseObject, Utils.round(newshare, 2));								
							}
							else if(hisabMap.containsKey(giverTakerObject)){
								float newshare = hisabMap.get(giverTakerObject) + expenseObject.getShare();
								hisabMap.put(giverTakerObject, Utils.round(newshare, 2));
							}
							else{
								hisabMap.put(giverTakerObject, expenseObject.getShare());
							}						
						}					
					}				
				}
				
				System.out.println("hisab +++++"+GEMApp.getInstance().getSettlements());
				//Handling Settlements if any
				if(GEMApp.getInstance().getSettlements().size() > 0){
					for(SettlementObject object:GEMApp.getInstance().getSettlements()){
						GiverTakerObject giverTakerObject = new GiverTakerObject(object.getGivenBy(), object.getTakenBy());
						GiverTakerObject reverseObject = new GiverTakerObject(object.getTakenBy(), object.getGivenBy());
						if(hisabMap.containsKey(reverseObject)){
							float newshare = hisabMap.get(reverseObject) - object.getAmount();										
							hisabMap.put(reverseObject, Utils.round(newshare, 2));								
						}
						else if(hisabMap.containsKey(giverTakerObject)){
							float newshare = hisabMap.get(giverTakerObject) + object.getAmount();
							hisabMap.put(giverTakerObject, Utils.round(newshare, 2));
						}
						else{
							hisabMap.put(giverTakerObject, object.getAmount());
						}		
					}
				}
				
				//Refining the Final Map... Removing entries having amount as 0 since it is not needed
				Set<Entry<GiverTakerObject, Float>> entrySet = hisabMap.entrySet();
				GEMApp.getInstance().setGiverTakermap(new HashMap<GiverTakerObject, Float>());
				for(Entry<GiverTakerObject, Float> entry: entrySet){
					if(entry.getValue() != 0){
						GEMApp.getInstance().getGiverTakermap().put(entry.getKey(), entry.getValue());
					}
				}
			}catch(JSONException e){
				e.printStackTrace();
			}
			
			
			float amounttogive = 0f,amounttotake = 0f;
			List<String> hisabArray = new ArrayList<String>();
			Set<Entry<GiverTakerObject, Float>> entries = GEMApp.getInstance().getGiverTakermap().entrySet();
			for(Entry<GiverTakerObject, Float> entry:entries){
				if(entry.getValue() >= 0){
					hisabArray.add(entry.getKey().getWhoUsed()+":"+entry.getKey().getWhoSpent()+":"+entry.getValue());
				}
				else{
					float value = Math.abs(entry.getValue());
					hisabArray.add(entry.getKey().getWhoSpent()+":"+entry.getKey().getWhoUsed()+":"+value);
				}
			}
			
			for(String s:hisabArray){
				if(s.split(":")[0].equalsIgnoreCase(memberName)){
					amounttogive += Float.parseFloat(s.split(":")[2]);
				}else if(s.split(":")[1].equalsIgnoreCase(memberName)){
					amounttotake += Float.parseFloat(s.split(":")[2]);
				}
			}
			
			
			howmuchgetfromothers.setText(getString(R.string.inr)+" "+Utils.round(amounttotake,2));
			howmuchgivetoothers.setText(getString(R.string.inr)+" "+Utils.round(amounttogive,2));

		}
				
	}

	private void initUI() {
		context = this;
		db  =  new DBAdapter(context);
		preferences = getSharedPreferences(AppConstants.CUSTOM_PREFERENCE, 0);
		totalSpentView = (TextView) findViewById(R.id.totalSpentView);
		totalSpentViewByMember = (TextView) findViewById(R.id.totalSpentViewByMember);
		howmuchgetfromothers = (TextView) findViewById(R.id.howmuchgetfromothers);
		howmuchgivetoothers = (TextView) findViewById(R.id.howmuchgivetoothers);
	}
}
