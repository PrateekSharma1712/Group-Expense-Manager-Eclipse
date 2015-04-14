package com.prateek.gem.views;

import static com.prateek.gem.AppConstants.EXTRA_MESSAGE;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.prateek.gem.AppConstants;
import com.prateek.gem.AppConstants.ServiceIDs;
import com.prateek.gem.R;
import com.prateek.gem.model.Users;
import com.prateek.gem.persistence.DBAdapter.TMembers;
import com.prateek.gem.services.ServerUtilities;
import com.prateek.gem.services.ServiceHandler;
import com.prateek.gem.services.WakeLocker;
import com.prateek.gem.utils.Utils;
import com.prateek.gem.views.MainActivity.PlaceholderFragment.UserLoginTask;

public class MainActivity extends ActionBarActivity {

	private static Button registerButton,signInButton;
	static EditText phoneNumberView,mCodeView,mPasswordView;
	static AsyncTask<String,Void,Boolean> checkRegistrationTask;
	static AsyncTask<Void, Void, Boolean> registrationTask;
	static AsyncTask<Void, Void, Void> mRegisterTask;
	public static String name,email,number,passwordValue,regId;
	static Intent groupsLandingIntent;
	private static UserLoginTask mAuthTask;
	static String mPhoneNumber;
	static String mPassword;
	static Users user;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getRegisteredStatus()){
			Intent self = getIntent();
			groupsLandingIntent = new Intent(MainActivity.this,GroupsLandingActivity.class);
			if(self != null) {
				if(self.getIntExtra("notId", 0) != 0) {
					int notId = self.getIntExtra("notId", 0);
					
				}
			}
			finish();
			startActivity(groupsLandingIntent);
		}else{
			setContentView(R.layout.activity_main);
			if (savedInstanceState == null) {
				getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
			}
		}		
		
	}
	
	public boolean getRegisteredStatus(){
		boolean result = false;
		SharedPreferences preferences = getSharedPreferences(AppConstants.CUSTOM_PREFERENCE, 0);
		if(preferences.getString(AppConstants.ADMIN_PHONE, null) != null){
			result = true;
		}
		return result;
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {			
			View rootView = inflater.inflate(R.layout.fragment_login, container,false);
			initUIForLogin(rootView);				
			return rootView;			
		}
		
		private void initUIForLogin(View rootView) {			
			System.out.println("1");
			registerButton = (Button) rootView.findViewById(R.id.registerButton);
			signInButton = (Button) rootView.findViewById(R.id.loginButton);
			phoneNumberView = (EditText) rootView.findViewById(R.id.phoneNumber);
			mCodeView = (EditText) rootView.findViewById(R.id.code);
			mPasswordView = (EditText) rootView.findViewById(R.id.password);
			mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

						@Override
						public boolean onEditorAction(TextView v, int id,KeyEvent event) {
							if (id == R.id.login || id == EditorInfo.IME_NULL) {
								attemptLogin();
								return true;
							}
							return false;
						}
					});
			registerButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					getFragmentManager().beginTransaction().replace(R.id.container, new RegisterFragment()).commit();					
				}
			});
			
			signInButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(Utils.hasConnection(getActivity())){
						attemptLogin();
					}else{
						Utils.showToast(getActivity(), getString(R.string.nonetwork));
					}	
				}
			});
			
			
		}
		
		/**
		 * Attempts to sign in or register the account specified by the login form.
		 * If there are form errors (invalid email, missing fields, etc.), the
		 * errors are presented and no actual login attempt is made.
		 */
		public void attemptLogin() {
			if (mAuthTask != null) {
				return;
			}

			// Reset errors.
			phoneNumberView.setError(null);
			mPasswordView.setError(null);

			// Store values at the time of the login attempt.
			mPhoneNumber = mCodeView.getText().toString() + phoneNumberView.getText().toString();
			mPassword = mPasswordView.getText().toString();

			boolean cancel = false;
			View focusView = null;

			// Check for a valid password.
			if (TextUtils.isEmpty(mPassword)) {
				mPasswordView.setError(getString(R.string.error_field_required));
				focusView = mPasswordView;
				cancel = true;
			} else if (mPassword.length() < 4) {
				mPasswordView.setError(getString(R.string.error_invalid_password));
				focusView = mPasswordView;
				cancel = true;
			}

			// Check for a valid email address.
			if (TextUtils.isEmpty(mPhoneNumber)) {
				phoneNumberView.setError(getString(R.string.error_field_required));
				focusView = phoneNumberView;
				cancel = true;
			} else if(mPhoneNumber.length() != 13){
				phoneNumberView.setError(getString(R.string.wrongnumber));
				focusView = phoneNumberView;
				cancel = true;
			}

			if (cancel) {
				// There was an error; don't attempt login and focus the first
				// form field with an error.
				focusView.requestFocus();
			} else {
				// Show a progress spinner, and kick off a background task to
				// perform the user login attempt.		
				
				mAuthTask = new UserLoginTask();
				mAuthTask.execute((Void) null);
			}
		}
		
		/**
		 * Represents an asynchronous login/registration task used to authenticate
		 * the user.
		 */
		public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
			
			private int errorCode = 1;
			ServiceHandler handler = new ServiceHandler();
			String message;
			MyProgressDialog pd = new MyProgressDialog(getActivity(), true, "Signing in...");
			
			@Override
			protected Boolean doInBackground(Void... params) {			
				List<NameValuePair> list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair(AppConstants.SERVICE_ID, ""+ServiceIDs.LOGIN));
				list.add(new BasicNameValuePair(TMembers.PHONE_NUMBER, mPhoneNumber));
				list.add(new BasicNameValuePair("mPassword", mPassword));
				String json = handler.makeServiceCall(AppConstants.URL_API, AppConstants.REQUEST_METHOD_POST,list);			
				
				JSONObject object = null;
				boolean result = false;
				if(json != null){
					try{
						System.out.println("try");
						object = new JSONObject(json);
						if(!object.getBoolean("error")){
							user = new Users();
							user.setGcmRegId(object.getString(Users.GCM_REG_ID));
							user.setUserName(object.getString(Users.USERNAME));
							user.setPhoneNumber(mPhoneNumber);
							user.setPassword(mPassword);
							user.setEmail(object.getString(Users.EMAIL));
							result = true;
						}else{
							message = object.getString("message");
							result = false;
							
						}
					}catch(JSONException e){
						e.printStackTrace();
					}
				}else{
					result = false;
				}
				
				return result;
			}

			@Override
			protected void onPostExecute(final Boolean success) {				
				pd.dismiss();
				if(success){					
					SharedPreferences preferences = getActivity().getSharedPreferences(AppConstants.CUSTOM_PREFERENCE, 0);
					Editor editor = preferences.edit();
					editor.putString(AppConstants.ADMIN_ID, user.getGcmRegId());
					editor.putString(AppConstants.ADMIN_NAME, user.getUserName());
					editor.putString(AppConstants.ADMIN_EMAIL, user.getEmail());
					editor.putString(AppConstants.ADMIN_PHONE, user.getPhoneNumber());												
					editor.putString(AppConstants.ADMIN_PASSWORD, user.getPassword());
					editor.commit();
					getActivity().finish();
					groupsLandingIntent = new Intent(getActivity(),GroupsLandingActivity.class);
					getActivity().startActivity(groupsLandingIntent);
				}
				else{
					System.out.println("Message "+message);
					if(message.equals("passwordwrong")){
						mPasswordView.setError(getString(R.string.error_incorrect_password));
					}else if(message.equals("usernotfound")){
						phoneNumberView.setError(getString(R.string.notregistered));
					}
				}
				mAuthTask = null;
				/*if (success) {
					System.out.println("success 1");
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("registeringId", mPhoneNumber);
					System.out.println(mPhoneNumber);
					editor.commit();
					// Setting Admin
					GEMApp.getInstance().setAdmin(admin);	
					System.out.println(GEMApp.getInstance().getAdmin());	
					finish();
					intent = new Intent(SetupActivity.this,MainActivity.class);
					startActivity(intent);
					
				} else {
					if(errorCode == 2){
						System.out.println("2");
						phoneNumberView.setError(getString(R.string.notregistered));
						phoneNumberView.requestFocus();
					}else if(errorCode == 1){
						System.out.println("1");
						mPasswordView.setError(getString(R.string.error_incorrect_password));
						mPasswordView.requestFocus();
					}
				}*/
			}

			@Override
			protected void onPreExecute() {
				// TODO Auto-generated method stub
				super.onPreExecute();
				pd.show();
			}

			@Override
			protected void onCancelled() {
				mAuthTask = null;
				pd.dismiss();
			}
		}
	}
	
	
	
	
	
	public static class RegisterFragment extends Fragment {
		
		private AccountManager mAccountManager;
		private Spinner emailSpinner;		
		private Button registerButton;
		private EditText adminName,adminNumber,numbercode,password,confirmPassword;
		private Context context;
		
		public RegisterFragment() {
			
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {			
			
			View rootView = inflater.inflate(R.layout.fragment_register, container,false);
			initUI(rootView);
			if(!Utils.hasConnection(context)){
				Utils.showToast(context, getString(R.string.nonetwork));
			}
			ArrayAdapter<String> emailAdapter = new ArrayAdapter<String>(getActivity(), R.layout.my_simple_spinner_item, getAccountNames());
	    	emailAdapter.setDropDownViewResource(R.layout.listitem_dropdown);
	    	emailSpinner.setAdapter(emailAdapter);
			registerButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					name = Utils.stringify(adminName.getText());
					email = Utils.stringify(emailSpinner.getSelectedItem().toString());
					String code = Utils.stringify(numbercode.getText());
					number = code + Utils.stringify(adminNumber.getText());
					passwordValue = Utils.stringify(password.getText());
					final String confirmPasswordValue = Utils.stringify(confirmPassword.getText());
					
					if(name.isEmpty()){
						adminName.setError(getString(R.string.cannotempty));
						adminName.requestFocus();
					}else if(number.isEmpty()){
						adminNumber.setError(getString(R.string.cannotempty));
						adminNumber.requestFocus();
					}else if(number.length() != 13){
						adminNumber.setError(getString(R.string.wrongnumber));
						adminNumber.requestFocus();
					}else if(code.isEmpty()){
						numbercode.setError(getString(R.string.cannotempty));
						numbercode.requestFocus();
					}else if(passwordValue.isEmpty()){
						password.setError(getString(R.string.cannotempty));
						password.requestFocus();
					}else if(passwordValue.length() < 4){
						password.setError(getString(R.string.password));
						password.requestFocus();				
					}else if(confirmPasswordValue.isEmpty()){
						confirmPassword.setError(getString(R.string.cannotempty));
						confirmPassword.requestFocus();
					}else if(!passwordValue.equals(confirmPasswordValue)){
						confirmPassword.setError(getString(R.string.notmatch));
						confirmPassword.requestFocus();
					}else{
						System.out.println(name);
						System.out.println(email);
						System.out.println(number);						
						System.out.println(passwordValue);
						System.out.println(confirmPasswordValue);						
						
						checkRegistrationTask = new AsyncTask<String, Void, Boolean>(){

							MyProgressDialog pd = new MyProgressDialog(context, true, getString(R.string.load_checking_number));
							
							@Override
							protected void onPreExecute() {
								super.onPreExecute();
								pd.show();
							}

							@Override
							protected Boolean doInBackground(String... params) {
								ServiceHandler handler = new ServiceHandler();
								List<NameValuePair> list = new ArrayList<NameValuePair>();
								list.add(new BasicNameValuePair("number", params[0]));
								list.add(new BasicNameValuePair(AppConstants.SERVICE_ID, ""+ServiceIDs.CHECK_USER_REGISTERED));
								String jsonResponse = handler.makeServiceCall(AppConstants.URL_API, AppConstants.REQUEST_METHOD_POST, list);
								System.out.println(jsonResponse);
								if(jsonResponse != null){
									try {
										JSONObject jsonObject = new JSONObject(jsonResponse);
										if(!jsonObject.getBoolean("error")){
											return jsonObject.getBoolean("isregistered");
										}
									} catch (JSONException e) {									
										e.printStackTrace();
									}
								}
								
								return false;
							}
							
							@Override
							protected void onPostExecute(Boolean result) {
								// TODO Auto-generated method stub
								super.onPostExecute(result);
								System.out.println("Resgistered "+result);
								
								pd.dismiss();
								if(!result){
									System.out.println("enter");
									registrationTask.execute(null,null,null);
								}else{
									adminNumber.setError(getString(R.string.alreadyregistered));
									adminNumber.requestFocus();
								}
								pd = null;
								
							}
							
						};
						
						registrationTask = new AsyncTask<Void, Void, Boolean>(){

							MyProgressDialog pd = new MyProgressDialog(context, true, getString(R.string.registering));
							
							@Override
							protected Boolean doInBackground(Void... params) {
								// Make sure the device has the proper dependencies.
								GCMRegistrar.checkDevice(getActivity());

								// Make sure the manifest was properly set - comment out this line
								// while developing the app, then uncomment it when it's ready.
								GCMRegistrar.checkManifest(getActivity());
								getActivity().registerReceiver(mHandleMessageReceiver, new IntentFilter(
										AppConstants.DISPLAY_MESSAGE_ACTION));
								// Get GCM registration id
								final String regId = GCMRegistrar.getRegistrationId(getActivity());

								// Check if regid already presents
								if (regId.equals("")) {
									// Registration is not present, register now with GCM		
									System.out.println("regid is ''");
									GCMRegistrar.register(getActivity(), AppConstants.SENDER_ID);
								} else {
									// Device is already registered on GCM
									System.out.println("Device is already registered on GCM");
									if (GCMRegistrar.isRegisteredOnServer(getActivity())) {
										// Skips registration.				
										//Toast.makeText(getActivity(), "Already registered with GCM", Toast.LENGTH_LONG).show();
									} else {
										// Try to register again, but not in the UI thread.
										// It's also necessary to cancel the thread onDestroy(),
										// hence the use of AsyncTask instead of a raw thread.
										mRegisterTask.execute(null, null, null);
										
										mRegisterTask = new AsyncTask<Void, Void, Void>() {

											@Override
											protected Void doInBackground(Void... params) {
												// Register on our server
												// On server creates a new user												
												ServerUtilities.register(context, name, email, number, passwordValue, regId);
												return null;
											}

											@Override
											protected void onPostExecute(Void result) {
												mRegisterTask = null;
												
											}

										};
									}
								}
								return null;
							}

							@Override
							protected void onPreExecute() {
								super.onPreExecute();
								pd.show();
							}

							@Override
							protected void onPostExecute(Boolean result) {
								super.onPostExecute(result);
								pd.dismiss();
								registrationTask = null;
								
							}
						};
						
						if(Utils.hasConnection(context)){
							checkRegistrationTask.execute(number,null,null);
						}
						//new AddAdmintask().execute(new String[]{name,number,code,passwordValue});
					}
				}
			});
			
			
			return rootView;
		}
		
		/**
		 * Receiving push messages
		 * */
		private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
				// Waking up mobile if it is sleeping
				WakeLocker.acquire(getActivity());
				
				/**
				 * Take appropriate action on this message
				 * depending upon your app requirement
				 * For now i am just displaying it on the screen
				 * */
				
				// Showing received message
				//lblMessage.append(newMessage + "\n");			
				Toast.makeText(getActivity(), "New Message: " + newMessage, Toast.LENGTH_LONG).show();
				
				// Releasing wake lock
				WakeLocker.release();
				
				
				SharedPreferences preferences = getActivity().getSharedPreferences(AppConstants.CUSTOM_PREFERENCE, 0);
				Editor editor = preferences.edit();
				editor.putString(AppConstants.ADMIN_ID, newMessage);
				editor.putString(AppConstants.ADMIN_NAME, name);
				editor.putString(AppConstants.ADMIN_EMAIL, email);
				editor.putString(AppConstants.ADMIN_PHONE, number);												
				editor.putString(AppConstants.ADMIN_PASSWORD, passwordValue);
				editor.commit();
				getActivity().finish();
				groupsLandingIntent = new Intent(getActivity(),GroupsLandingActivity.class);
				getActivity().startActivity(groupsLandingIntent);
				
			}
		};

		@Override
		public void onDestroy() {
			if (mRegisterTask != null) {
				mRegisterTask.cancel(true);
			}
			try {
				getActivity().unregisterReceiver(mHandleMessageReceiver);
				GCMRegistrar.onDestroy(getActivity());
			} catch (Exception e) {
				Log.e("UnRegister Receiver Error", "> " + e.getMessage());
			}
			super.onDestroy();
		}

		private void initUI(View rootView) {
			context = getActivity();
			emailSpinner = (Spinner) rootView.findViewById(R.id.email);
			adminName = (EditText) rootView.findViewById(R.id.adminName);
			adminNumber = (EditText) rootView.findViewById(R.id.adminNumber);
			numbercode = (EditText) rootView.findViewById(R.id.code);
			password = (EditText) rootView.findViewById(R.id.password);
			confirmPassword = (EditText) rootView.findViewById(R.id.confirmPassword);
			registerButton = (Button) rootView.findViewById(R.id.addregistrationButton);
		}
		
		private String[] getAccountNames() {
		    mAccountManager = AccountManager.get(this.getActivity());
		    Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);		    
		    String[] names = new String[accounts.length];
		    for (int i = 0; i < names.length; i++) {
		        names[i] = accounts[i].name;
		        System.out.println("Account "+i+" \nName"+names[i]);
		        System.out.println("Type"+accounts[i].type);	        
		    }
		    return names;
		}
		
		
	}

	@Override
	public void onBackPressed() {		
		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
		if(fragment.getClass().getSimpleName().equalsIgnoreCase("registerfragment")){
			getSupportFragmentManager().beginTransaction().replace(R.id.container, new PlaceholderFragment()).commit();
		}else{
			super.onBackPressed();
		}
	}
	
	
	
}
