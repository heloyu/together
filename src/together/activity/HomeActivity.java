package together.activity;import java.io.IOException;import java.util.ArrayList;import java.util.HashMap;import java.util.List;import org.apache.http.client.ClientProtocolException;import org.json.JSONException;import org.json.JSONObject;import together.connectivity.JsonHandler;import together.connectivity.MySimpleAdapter;import together.connectivity.ServerResponse;import together.models.EventMsg;import together.utils.AssetsUtil;import together.utils.MyConstants;import together.utils.MyLocation;import together.widgets.PullToRefreshList;import android.annotation.SuppressLint;import android.app.ListActivity;import android.app.ProgressDialog;import android.content.Context;import android.content.Intent;import android.content.SharedPreferences;import android.content.pm.ActivityInfo;import android.content.pm.PackageManager.NameNotFoundException;import android.location.Location;import android.location.LocationManager;import android.os.AsyncTask;import android.os.Bundle;import android.os.Handler;import android.os.Message;import android.util.Log;import android.view.View;import android.view.View.OnClickListener;import android.widget.AdapterView;import android.widget.Toast;import android.widget.AdapterView.OnItemClickListener;import android.widget.TextView;@SuppressLint("ParserError")public class HomeActivity extends ListActivity {	private MySimpleAdapter listAdapter;	private ProgressDialog progressDialog;	private Context context;	private View loadMoreView;	private TextView loadMoreButton;	private TextView sendText;	private PullToRefreshList listView;	private ArrayList<HashMap<String, Object>> listArray = new ArrayList<HashMap<String, Object>>();	private HashMap<String, Object> map_use;	private boolean bottomFlag = false;		private String UID;	public void onDestroy() {		if (progressDialog != null)			progressDialog.dismiss();		super.onDestroy();	}	@Override	public void onCreate(Bundle savedInstanceState) {		super.onCreate(savedInstanceState);		setContentView(R.layout.home);		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);				//获取UID		UID = getSharedPreferences("user", Context.MODE_PRIVATE).getString("uid", null);  		context = this;		initUI();		try {			progressDialog.show();			//载入界面时向服务器更新自己的位置			new SendLocation().execute();			buildlist();		} catch (Exception e) {			e.printStackTrace();		}	}	private void initUI() {		listView = ((PullToRefreshList) getListView());		listView.setCacheColorHint(0);		progressDialog = new ProgressDialog(context);		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);		progressDialog.setIcon(R.drawable.loading);		progressDialog.setMessage(getString(R.string._loading));		loadMoreView = getLayoutInflater().inflate(R.layout.loadmore, null);		loadMoreButton = (TextView) loadMoreView				.findViewById(R.id.loadMoreButton);		loadMoreButton.setText(R.string._more);		sendText = (TextView) findViewById(R.id.sendText);		sendText.setOnClickListener(new OnClickListener() {			@Override			public void onClick(View v) {				Intent itent = new Intent(HomeActivity.this,						SendMessageActivity.class);				startActivity(itent);			}		});		loadMoreButton.setOnClickListener(new View.OnClickListener() {			@Override			public void onClick(View v) {				loadMoreButton.setText(R.string._loading);				progressDialog.show();				new ClickGetDataTask().execute();			}		});		listView.addFooterView(loadMoreView);		listView.setOnRefreshListener(new together.widgets.PullToRefreshList.OnRefreshListener() {			@Override			public void onRefresh() {				new PullGetDataTask().execute();				/*每次刷新时像服务器更新自己的位置*/				new SendLocation().execute();			}		});	}		/**	 * 向服务器更新自己的位置	 * @author hElo	 *	 */	private class SendLocation extends AsyncTask<Void, Void, Boolean> {		protected Boolean doInBackground(Void... params) {			LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);			Location location = new MyLocation(locationManager).getLoaction();			String longitude = Double.toString(location.getLongitude());			String latitude = Double.toString(location.getLatitude());			String uid = UID;			String url = MyConstants.SITE + getString(R.string.UpdateUserLocation);			JSONObject json = new JSONObject();			try {				json.put("uid", uid);				json.put("longitude", longitude);				json.put("latitude", latitude);			} catch (JSONException e) {				e.printStackTrace();			}			String result = null;			try {				result = ServerResponse.getResponse(url, json);			} catch (ClientProtocolException e) {				e.printStackTrace();			} catch (IOException e) {				e.printStackTrace();			}			if(result.contains("success"))				return true;			else				return false;		}				 protected void onPostExecute(Boolean result) {			 if(result)				 Toast.makeText(getApplicationContext(), "send location success", Toast.LENGTH_SHORT).show();			 else				 Toast.makeText(getApplicationContext(), "send location fail", Toast.LENGTH_SHORT).show();	     }	}	private class PullGetDataTask extends AsyncTask<Void, Void, String[]> {		@Override		protected String[] doInBackground(Void... params) {			try {				bottomFlag = false;				buildlist();			} catch (JSONException e) {				e.printStackTrace();			} catch (IOException e) {				e.printStackTrace();			}			return null;		}		@Override		protected void onPostExecute(String[] result) {			// Call onRefreshComplete when the list has been refreshed.			((PullToRefreshList) getListView()).onRefreshComplete();			super.onPostExecute(result);		}	}	/**	 * async task , used when click load more button of list view to refresh data	 * */	private class ClickGetDataTask extends AsyncTask<Void, Void, String[]> {		@Override		protected String[] doInBackground(Void... params) {			// Simulates a background job.			bottomFlag = true;			loadMoreData(true);// add to bottom			return null;		}		@Override		protected void onPostExecute(String[] result) {			// Call onRefreshComplete when the list has been refreshed.			progressDialog.cancel();			super.onPostExecute(result);		}	}	private String msgGet = "";	private Handler listHandler = new Handler() {// this is used to generate the													// groups listview		public void handleMessage(Message msg) {			switch (msg.what) {			case MyConstants.MSG_SUCCESS1:				@SuppressWarnings("unchecked")				final ArrayList<HashMap<String, Object>> array = (ArrayList<HashMap<String, Object>>) msg.obj;				loadMoreButton.setText(R.string._more);				listAdapter = new MySimpleAdapter(context, array,						R.layout.list_item, new String[] { "uid", "place", "time",								"type","description" }, new int[] { R.id.sItemUid, R.id.sItemTitle,								R.id.sItemTime, R.id.sItemInfo,R.id.sItemDescription }, 120);				listView.setAdapter(listAdapter);				listArray = array;				listView.setVisibility(View.GONE);				listAdapter.notifyDataSetChanged();				listView.setVisibility(View.VISIBLE);				if (bottomFlag)					listView.setSelectionFromTop(							listAdapter.GetmData().size() - 1, 150);				progressDialog.cancel();				listView.setOnItemClickListener(new OnItemClickListener() {					@Override					public void onItemClick(AdapterView<?> arg0, View arg1,							int arg2, long arg3) {						itemClick(arg2);					}					private void itemClick(int arg2) {						if (arg2 > listArray.size()) {							return;						}						Intent intent = new Intent();						if (arg2 > 0)							arg2--;						intent.putExtra("eid", (String) listArray.get(arg2)								.get("eid"));						intent.putExtra("place", (String) listArray.get(arg2)								.get("place"));						intent.putExtra("uid", (String) listArray.get(arg2)								.get("uid"));						intent.putExtra("type", (String) listArray.get(arg2)								.get("type"));												intent.putExtra("description", (String) listArray.get(arg2)								.get("description"));						intent.putExtra("longitude", (String) listArray.get(arg2)								.get("longitude"));						intent.putExtra("latitude", (String) listArray.get(arg2)								.get("latitude"));						intent.putExtra("time", (String) listArray.get(arg2)								.get("time"));						intent.setClass(HomeActivity.this,								FollowedMessageActivity.class);						startActivity(intent);					}				});				break;			case MyConstants.MSG_FAILURE:				progressDialog.cancel();				if (msgGet == null || msgGet.equals("[]")) {					loadMoreButton.setText(R.string.no_more);					if (bottomFlag)						listView.setSelectionFromTop(listAdapter.GetmData()								.size() - 1, 150);				} else					loadMoreButton.setText(R.string._more);				break;			}		}	};	private void loadMoreData(boolean bottom) {		try {			add_list(bottom);		} catch (JSONException e) {			e.printStackTrace();		} catch (IOException e) {			e.printStackTrace();		}	}	private void buildlist() throws JSONException, IOException {		final ArrayList<HashMap<String, Object>> Array = new ArrayList<HashMap<String, Object>>();		new Thread() {			public void run() {				String s = null;				try {					JSONObject jo = new JSONObject();					//TODO 添加真实的用户信息					jo.put("uid", UID);					jo.put("radius", "0");					/*从服务器获取event信息*/					String url = MyConstants.SITE + getString(R.string.ListEvent);					Log.i("together", "befor http");					s = ServerResponse.getResponse(url, jo);					Log.i("together", "after http");					if (s == null) {						msgGet = s;						listHandler.obtainMessage(MyConstants.MSG_FAILURE)								.sendToTarget();						return;					}					if (s.equals("[]")) {						// 说明没有更多新闻了，此时msgGet="[]"						msgGet = s;						listHandler.obtainMessage(MyConstants.MSG_FAILURE)								.sendToTarget();						return;					}					JsonHandler jsonHandler = new JsonHandler();					List<EventMsg> msgs = jsonHandler.getEventMessages(s, "event");					HashMap<String, Object> map;					for (EventMsg p : msgs) {						map = getMapForShow(p);						Array.add(map);					}				} catch (JSONException e) {					listHandler.obtainMessage(MyConstants.MSG_FAILURE)							.sendToTarget();					e.printStackTrace();					Log.i("together", e.toString());				} catch (ClientProtocolException e) {					e.printStackTrace();					Log.i("together", e.toString());				} catch (IOException e) {					e.printStackTrace();					Log.i("together", e.toString());				}				if (Array.size() == 0) {					Log.i("together", "build fail");					listHandler.obtainMessage(MyConstants.MSG_FAILURE)							.sendToTarget();				} else {					Log.i("together", "build success");					listHandler.obtainMessage(MyConstants.MSG_SUCCESS1, Array)							.sendToTarget();				}			}		}.start();	}	private HashMap<String, Object> getMapForShow(EventMsg p) {		//TODO 获取正确信息		HashMap<String, Object> map;		map = new HashMap<String, Object>();		map.put("eid", p.getEid());		map.put("place", p.getPlace());		map.put("uid", p.getUid());		map.put("type",  p.getType());		map.put("description",  p.getDescription());		map.put("longitude", p.getLongitude());		map.put("latitude",  p.getLatitude());		map.put("time", p.getStartTime());		return map;	}	private void add_list(final boolean bottom) throws JSONException,			IOException {		String s = AssetsUtil.getFromAssets(context, "star.json");		// String http_url = "/stars.json?start=" + last_star_id + "&count="		// + MyConstants.STAR;		// s = MyHttpResponse.getResponse(http_url);		if (s == null) {			Log.d("StarActivity", "null response");			msgGet = s;			listHandler.obtainMessage(MyConstants.MSG_FAILURE).sendToTarget();			return;		}		if (s.equals("[]")) {			// 说明没有更多新闻了，此时msgGet="[]"			msgGet = s;			listHandler.obtainMessage(MyConstants.MSG_FAILURE).sendToTarget();			return;		}		JsonHandler jsonHandler = new JsonHandler();		List<EventMsg> msgs = jsonHandler.getMessages(s);		ArrayList<HashMap<String, Object>> array = new ArrayList<HashMap<String, Object>>();		for (int i = 0; i < listAdapter.GetmData().size(); i++)			array.add(listAdapter.GetmData().get(i));		for (EventMsg p : msgs) {			map_use = getMapForShow(p);			array.add(map_use);		}		// last_id += msgs.size();		if (msgs.size() == 0) {			listHandler.obtainMessage(MyConstants.MSG_FAILURE).sendToTarget();		} else {			listHandler.obtainMessage(MyConstants.MSG_SUCCESS1, array)					.sendToTarget();		}	}	 }