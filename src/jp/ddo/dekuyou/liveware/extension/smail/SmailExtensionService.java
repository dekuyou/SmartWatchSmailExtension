/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
 of its contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jp.ddo.dekuyou.liveware.extension.smail;

import java.util.List;

import jp.ddo.dekuyou.android.util.Log;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.SmsMessage;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * The sample extension service handles extension registration and inserts data
 * into the notification database.
 */
public class SmailExtensionService extends ExtensionService {



	/**
	 * Extensions specific id for the source
	 */
	public static final String EXTENSION_SPECIFIC_ID = "EXTENSION_SPECIFIC_ID_POOR_GMAIL_NOTIFIER";

	/**
	 * Extension key
	 */
	public static final String EXTENSION_KEY = "jp.ddo.dekuyou.liveware.extension.gmail.key";

	public SmailExtensionService() {
		super(EXTENSION_KEY);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("onCreate");
	}
	// Our handler.
	private Handler mHandler = null;

	private String phoneNo = "";
	private String subject = "";
	private String body = "";
	private String date = "";

	/**
	 * {@inheritDoc}
	 * 
	 * @see android.app.Service#onStartCommand()
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int retVal = super.onStartCommand(intent, flags, startId);

		Log.initialize(this);

		// Create handler.
		if (mHandler == null) {
			mHandler = new Handler();
		}

		Log.d(
				"SmailPluginService onStart");

		if(intent == null){
			Log.d("intent is null.");
			return START_NOT_STICKY; // FIXME: onStartCommand の戻り値はいったい？
		}
		
		Bundle extras = intent.getExtras();
		if (extras != null) {
			
			phoneNo = "";
			subject = "";
			body = "";
			date = "";

			try {
				if (extras.get("pdus") != null) {
					// SMS
					// pduのデコードとログ出力
					// サンプルのためBroadcastReceiverで処理(本来はServiceで)
					Object[] pdus = (Object[]) extras.get("pdus");
					for (Object pdu : pdus) {
						SmsMessage smsMessage = SmsMessage
								.createFromPdu((byte[]) pdu);
						Log.d(this.getClass().getPackage().getName(), "from:"
								+ smsMessage.getOriginatingAddress());
						Log.d(this.getClass().getPackage().getName(),
								"time:"
										+ Long.toString(smsMessage
												.getTimestampMillis()));
						Log.d(this.getClass().getPackage().getName(),
								"body:"
										+ smsMessage.getMessageBody()
												.replaceAll("\n", "\t"));

						phoneNo = smsMessage.getOriginatingAddress();
						body = body + smsMessage.getMessageBody();
						date = Long.toString(smsMessage.getTimestampMillis());

					}
				} else if (extras.get("deleted_contents") != null) {

					// MMS
					String _id = extras.get("deleted_contents").toString();

					if (_id.startsWith("content://mms/inbox/")) {

						Cursor curPdu = getContentResolver().query(
								Uri.parse("content://mms"), null, null, null,
								null);

						curPdu.moveToFirst();

						String id = curPdu.getString(curPdu
								.getColumnIndex("_id"));
						Log.d(this.getPackageName(), id);

						String thread_id = curPdu.getString(curPdu
								.getColumnIndex("thread_id"));
						Log.d(this.getPackageName(), thread_id);

						// subject =
						// MimeUtility.decodeText(curPdu.getString(curPdu.getColumnIndex("sub")));

						String tmpsbj = curPdu.getString(curPdu
								.getColumnIndex("sub")) != null ? curPdu
								.getString(curPdu.getColumnIndex("sub")) : "";

						subject = new String(tmpsbj.getBytes("ISO8859_1"),
								"utf-8");

						Log.d(this.getPackageName(), String.valueOf(subject));

						date = String.valueOf(curPdu.getLong(curPdu
								.getColumnIndex("date")) * 1000L);
						Log.d(this.getPackageName(), String.valueOf(date));

						_id = String
								.valueOf((new Integer(_id.substring(20)) + 1));
						Log.d(this.getPackageName(), String.valueOf(_id));

						Uri uriAddr = Uri.parse("content://mms/" + _id
								+ "/addr");
						Cursor curAddr = getContentResolver().query(uriAddr,
								null, null, null, null);
						if (curAddr.moveToNext()) {

							phoneNo = curAddr.getString(curAddr
									.getColumnIndex("address"));
							Log.d(this.getPackageName(), String.valueOf(phoneNo));

							Cursor curPart = getContentResolver()
									.query(Uri.parse("content://mms/" + _id
											+ "/part"), null, null, null, null);
							String[] coloumns = null;
							String[] values = null;

							while (curPart.moveToNext()) {
								coloumns = curPart.getColumnNames();
								if (values == null)
									values = new String[coloumns.length];

								for (int i = 0; i < curPart.getColumnCount(); i++) {
									values[i] = curPart.getString(i);
									Log.d(this.getPackageName(), i + ":"
											+ String.valueOf(values[i]));

								}
								String contact_idd = curPart.getString(0);
								Log.d(this.getPackageName(),
										String.valueOf(contact_idd));

								if (values[3].equals("text/plain")) {

									body = values[13].replaceAll("\n", "\t");

								}
							}

							// date = String.valueOf(Calendar.getInstance()
							// .getTimeInMillis());

						}

				

					} else if (_id.startsWith("content://mms/")) {
						Log.d("content://mms/");
						//
						// phonNo = "?";
						// body = "未取得のメッセージ";
						// date = String.valueOf(Calendar.getInstance()
						// .getTimeInMillis());

					} else {
						Log.d("MMS not content://mms/inbox/");
						// this.stopSelf();
						return START_NOT_STICKY; // FIXME: onStartCommand の戻り値はいったい？
					}

				}
			} catch (Exception e) {
				// TODO: handle exception
				Log.d(e);
				// this.stopSelf();
				return START_NOT_STICKY; // FIXME: onStartCommand の戻り値はいったい？
			}

			Log.d("phonNo:" + phoneNo);
			Log.d("subject:" + subject);
			Log.d("body:" + body);
			Log.d("date:" + date);
			
			phoneNo = getName(phoneNo);
			
			Log.d("phonNo_:" + phoneNo);
			
			if(phoneNo == null || phoneNo.equals("")){
				return START_NOT_STICKY; // FIXME: onStartCommand の戻り値はいったい？
			}

			sendAnnounce(phoneNo,subject, body);
		}
		return retVal;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("onDestroy");
	}
	
	@SuppressWarnings("unused")
	private void sendAnnounce(String title, String message){
		sendAnnounce("",title,message);
	}

	private void sendAnnounce(String from, String title, String message) {

		long time = System.currentTimeMillis();
		long sourceId = NotificationUtil.getSourceId(this,
				EXTENSION_SPECIFIC_ID);
		if (sourceId == NotificationUtil.INVALID_ID) {
			Log.e("Failed to insert data");
			return;
		}


		ContentValues eventValues = new ContentValues();
		eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
		
		eventValues.put(Notification.EventColumns.DISPLAY_NAME, from );
		eventValues.put(Notification.EventColumns.TITLE,  title);
		eventValues.put(Notification.EventColumns.MESSAGE, message);
		eventValues.put(Notification.EventColumns.PERSONAL, 0);
		// eventValues.put(Notification.EventColumns.IMAGE_URI, icon);
//		eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI,
//				ExtensionUtils.getUriString(this,
//						R.drawable.widget_default_userpic_bg));
		eventValues.put(Notification.EventColumns.PUBLISHED_TIME, time);
		eventValues.put(Notification.EventColumns.SOURCE_ID, sourceId);
		

		try {
			getContentResolver().insert(Notification.Event.URI, eventValues);
		} catch (IllegalArgumentException e) {
			Log.e("Failed to insert event:¥n " + e);
		} catch (SecurityException e) {
			Log.e("Failed to insert event, is Live Ware Manager installed?:¥n "
					+ e);
		}
	}

	@Override
	protected void onViewEvent(Intent intent) {
		String action = intent
				.getStringExtra(Notification.Intents.EXTRA_ACTION);
		int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID,
				-1);
		if (Notification.SourceColumns.ACTION_1.equals(action)) {
			doAction1(eventId);
		}
	}

	@Override
	protected void onRefreshRequest() {
		// Do nothing here, only relevant for polling extensions, this
		// extension is always up to date
	}

	/**
	 * Show toast with event information
	 * 
	 * @param eventId
	 *            The event id
	 */
	public void doAction1(int eventId) {
		Log.d("doAction1 event id: " + eventId);
		
        PackageManager pm = this.getPackageManager();
        
        
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        
    
        
        //
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        
        Log.d(list.toString());
        
        String pkgNmae = "";
        String clazz = "";
        
        Log.d("ResolveInfo.size():"+list.size());
 
        //パッケージ情報をリストビューに追記
        for (ResolveInfo ai : list) {
            
            Log.d("appname"+ ai.loadLabel(pm).toString());
            Log.d("packageName"+ ai.activityInfo.applicationInfo.packageName);
            Log.d("className"+ ai.activityInfo.name);
            
            if(ai.activityInfo.applicationInfo.packageName.indexOf("jp.softbank.mb.mail") > -1){
        		// jp.softbank.mb.mail
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            	
            }else if(ai.activityInfo.applicationInfo.packageName.indexOf("com.sonyericsson.conversations") > -1){
            	// com.sonyericsson.conversations
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            	
            }else if (ai.activityInfo.applicationInfo.packageName.indexOf("com.android.mms") > -1){
            	// com.android.mms
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            }else if (ai.activityInfo.applicationInfo.packageName.indexOf("com.jb.mms") > -1){
            	// com.jb.mms	
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            }
            
            
            

        }
		

        Log.d("_appname:"+ clazz);
        Log.d("_packageName:"+ pkgNmae);

        try {
			// Open in .
			final Intent browserIntent = new Intent(Intent.ACTION_MAIN);
			browserIntent.setClassName(pkgNmae,clazz);
			browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(browserIntent);
		} catch (Exception e) {
			Log.e(e);
		}
	}

	/**
	 * Called when extension and sources has been successfully registered.
	 * Override this method to take action after a successful registration.
	 */
	@Override
	public void onRegisterResult(boolean result) {
		super.onRegisterResult(result);
		Log.d("onRegisterResult");

	}

	@Override
	protected RegistrationInformation getRegistrationInformation() {
		return new SmailRegistrationInformation(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonyericsson.extras.liveware.aef.util.ExtensionService#
	 * keepRunningWhenConnected()
	 */
	@Override
	protected boolean keepRunningWhenConnected() {
		return false;
	}

	private String getName(String phoneNo) {
		if(phoneNo.indexOf("@") > 0){
			return phoneNo;
		}
		
		String ret = phoneNo;

		String[] proj = new String[] { Phone._ID, Phone.DISPLAY_NAME,
				Phone.NUMBER };

		Uri _uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri
				.encode(phoneNo));

		Cursor _cursor = getContentResolver().query(_uri, proj, null, null,
				null);

		if (_cursor.getCount() > 0) {
			_cursor.moveToFirst();

			ret = _cursor.getString(1);
		}

		_cursor.close();

		return ret;

	}




}
