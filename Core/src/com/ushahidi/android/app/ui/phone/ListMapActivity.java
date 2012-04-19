/**
 ** Copyright (c) 2010 Ushahidi Inc
 ** All rights reserved
 ** Contact: team@ushahidi.com
 ** Website: http://www.ushahidi.com
 **
 ** GNU Lesser General Public License Usage
 ** This file may be used under the terms of the GNU Lesser
 ** General Public License version 3 as published by the Free Software
 ** Foundation and appearing in the file LICENSE.LGPL included in the
 ** packaging of this file. Please review the following information to
 ** ensure the GNU Lesser General Public License version 3 requirements
 ** will be met: http://www.gnu.org/licenses/lgpl.html.
 **
 **
 ** If you have questions regarding the use of this file, please contact
 ** Ushahidi developers at team@ushahidi.com.
 **
 **/

package com.ushahidi.android.app.ui.phone;

import java.io.File;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItem;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;

import com.ushahidi.android.app.Preferences;
import com.ushahidi.android.app.R;
import com.ushahidi.android.app.Settings;
import com.ushahidi.android.app.activities.BaseListActivity;
import com.ushahidi.android.app.adapters.ListMapAdapter;
import com.ushahidi.android.app.models.ListMapModel;
import com.ushahidi.android.app.net.CategoriesHttpClient;
import com.ushahidi.android.app.net.MapsHttpClient;
import com.ushahidi.android.app.net.ReportsHttpClient;
import com.ushahidi.android.app.tasks.ProgressTask;
import com.ushahidi.android.app.util.ApiUtils;
import com.ushahidi.android.app.views.AddMapView;
import com.ushahidi.android.app.views.ListMapView;

/**
 * @author eyedol
 */
public class ListMapActivity extends
		BaseListActivity<ListMapView, ListMapModel, ListMapAdapter> implements
		LocationListener {

	private final String[] items = { "50", "100", "250", "500", "750", "1000",
			"1500" };

	private static final int DIALOG_DISTANCE = 0;

	private static final int DIALOG_CLEAR_DEPLOYMENT = 1;

	private static final int DIALOG_ADD_DEPLOYMENT = 2;

	private static final int DIALOG_SHOW_MESSAGE = 3;

	private MenuItem refresh;

	private LocationManager mLocationMgr = null;

	private static Location location;

	private String distance = "";

	private Handler mHandler;

	private ListMapView listMapView;

	private int mId = 0;

	private int mapId = 0;

	private ListMapAdapter listMapAdapter;

	private ListMapModel listMapModel;

	private boolean edit = true;

	private String filter = null;

	private String errorMessage = "";

	private ApiUtils apiUtils;

	public ListMapActivity() {
		super(ListMapView.class, ListMapAdapter.class, R.layout.list_map,
				R.menu.list_map, android.R.id.list);
		mHandler = new Handler();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		actionBar.setDisplayHomeAsUpEnabled(false);
		listMapView = new ListMapView(this);
		listMapAdapter = new ListMapAdapter(this);
		listMapModel = new ListMapModel();
		apiUtils = new ApiUtils(this);

		registerForContextMenu(listMapView.mListView);

		listMapView.mTextView.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable arg0) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

				if (!(TextUtils.isEmpty(s.toString()))) {
					filter = s.toString();
					mHandler.post(filterMapList);
				} else {
					mHandler.post(fetchMapList);
				}

			}

		});
	}

	@Override
	public void onResume() {
		super.onResume();
		mHandler.post(fetchMapList);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopLocating();
	}

	/**
	 * Delete individual messages 0 - Successfully deleted. 1 - There is nothing
	 * to be deleted.
	 */
	final Runnable mDeleteMapById = new Runnable() {
		public void run() {
			boolean status = false;
			status = listMapModel.deleteMapById(mId);

			try {
				if (status) {
					toastShort(R.string.map_deleted);
					listMapAdapter.refresh();
				} else {
					toastShort(R.string.map_deleted_failed);
				}
			} catch (Exception e) {
				return;
			}
		}
	};

	/**
	 * Refresh the list view with new items
	 */
	final Runnable fetchMapList = new Runnable() {
		public void run() {
			try {
				listMapAdapter.refresh();
				listMapView.mListView.setAdapter(listMapAdapter);
				listMapView.displayEmptyListText();
				listMapView.mListView.requestFocus();
			} catch (Exception e) {
				return;
			}
		}
	};

	/**
	 * Filter the list view with new items
	 */
	final Runnable filterMapList = new Runnable() {
		public void run() {
			try {

				listMapAdapter.getFilter().filter(filter);

			} catch (Exception e) {
				return;
			}
		}
	};

	/**
	 * Delete all fetched maps
	 */
	final Runnable deleteAllMaps = new Runnable() {
		public void run() {
			boolean status = false;
			status = listMapModel.deleteAllMap(ListMapActivity.this);
			try {
				if (status) {
					toastShort(R.string.map_deleted);
					refreshMapLists();
				} else {
					toastShort(R.string.map_deleted_failed);
				}
			} catch (Exception e) {
				return;
			}
		}
	};

	final Runnable refreshMapList = new Runnable() {
		public void run() {
			try {
				refreshMapLists();
			} catch (Exception e) {
				return;
			}
		}
	};

	public void refreshMapLists() {
		listMapAdapter.refresh();
		listMapView.displayEmptyListText();
	}

	// Context Menu Stuff
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		new MenuInflater(this).inflate(R.menu.list_map_context, menu);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		boolean result = performAction(item, info.position);

		if (!result) {
			result = super.onContextItemSelected(item);
		}

		return (result);
	}

	public boolean performAction(android.view.MenuItem item, int position) {

		mId = listMapAdapter.getItem(position).getId();
		mapId = listMapAdapter.getItem(position).getMapId();

		if (item.getItemId() == R.id.map_delete) {
			// Delete by ID
			edit = false;
			mHandler.post(mDeleteMapById);
			return (true);
		} else if (item.getItemId() == R.id.map_edit) {
			// edit existing map
			edit = true;
			createDialog(DIALOG_ADD_DEPLOYMENT);
			return (true);
		}

		return (false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.clear_map) {
			createDialog(DIALOG_CLEAR_DEPLOYMENT);
			return true;
		} else if (item.getItemId() == R.id.menu_refresh) {
			refresh = item;
			createDialog(DIALOG_DISTANCE);
			return true;
		} else if (item.getItemId() == R.id.menu_add) {
			edit = false;
			createDialog(DIALOG_ADD_DEPLOYMENT);
			return true;
		} else if (item.getItemId() == R.id.app_settings) {
			startActivity(new Intent(this, Settings.class));
			setResult(RESULT_OK);
			return true;
		} else if (item.getItemId() == R.id.app_about) {
			startActivity(new Intent(this, AboutActivity.class));
			setResult(RESULT_OK);
			return true;
		}

		return super.onOptionsItemSelected(item);

	}

	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {

		final int sId = listMapAdapter.getItem(position).getId();

		if (isMapActive(sId)) {
			goToReports();
		} else {
			FetchMapReportTask fetchMapReportTask = new FetchMapReportTask(this);
			fetchMapReportTask.id = sId;
			fetchMapReportTask.execute((String) null);
		}

	}

	/**
	 * Check if a deployment is the active one
	 * 
	 * @param id
	 *            - map's id
	 * @return boolean
	 */

	public boolean isMapActive(long id) {
		Preferences.loadSettings(this);
		if (Preferences.activeDeployment == id) {
			return true;
		}
		return false;

	}

	public void goToReports() {
		Intent launchIntent;
		launchIntent = new Intent(this, ReportTabActivity.class);
		startActivityForResult(launchIntent, 0);
		overridePendingTransition(R.anim.home_enter, R.anim.home_exit);
		setResult(RESULT_OK);
	}

	/**
	 * Clear saved reports
	 */
	public void clearCachedReports() {

		// delete unset photo
		if (Preferences.fileName != null) {
			File f = new File(Preferences.fileName);
			if (f != null) {
				if (f.exists()) {
					f.delete();
				}
			}
		}

		// clear persistent data
		SharedPreferences.Editor editor = getPreferences(0).edit();
		editor.putString("title", "");
		editor.putString("desc", "");
		editor.putString("date", "");
		editor.putString("selectedphoto", "");
		editor.putInt("requestedcode", 0);
		editor.commit();
	}

	/**
	 * Create an alert dialog
	 */

	protected void createDialog(int d) {
		switch (d) {

		case DIALOG_SHOW_MESSAGE:
			AlertDialog.Builder messageBuilder = new AlertDialog.Builder(this);
			messageBuilder.setMessage(errorMessage).setPositiveButton(
					getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog showDialog = messageBuilder.create();
			showDialog.show();
			break;

		case DIALOG_DISTANCE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_distance);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {

					distance = items[item];

					setDeviceLocation();
				}
			});

			AlertDialog alert = builder.create();
			alert.show();
			break;

		case DIALOG_CLEAR_DEPLOYMENT:
			AlertDialog.Builder clearBuilder = new AlertDialog.Builder(this);
			clearBuilder
					.setMessage(getString(R.string.confirm_clear))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.status_yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mHandler.post(deleteAllMaps);
								}
							})
					.setNegativeButton(getString(R.string.status_no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog clearDialog = clearBuilder.create();
			clearDialog.show();

			break;

		case DIALOG_ADD_DEPLOYMENT:
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.add_map, null);
			final AddMapView addMapView = new AddMapView(textEntryView);

			// if edit was selected at the context menu, populate fields
			// with existing map details
			if (edit) {
				final List<ListMapModel> listMap = listMapModel.loadMapById(
						mId, mapId);
				if (listMap != null && listMap.size() > 0) {
					addMapView.setMapName(listMap.get(0).getName());
					addMapView.setMapDescription(listMap.get(0).getDesc());
					addMapView.setMapUrl(listMap.get(0).getUrl());
					addMapView.setMapId(listMap.get(0).getId());
				}
			}

			final AlertDialog.Builder addBuilder = new AlertDialog.Builder(this);

			addBuilder
					.setTitle(R.string.add_map)
					.setView(textEntryView)
					.setNegativeButton(R.string.btn_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialog.cancel();
								}
							})
					.setPositiveButton(R.string.btn_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// edit was selected
									if (edit) {

										if (!addMapView.updateMapDetails())
											toastLong(R.string.fix_error);
										else
											mHandler.post(refreshMapList);
									} else {

										if (!addMapView.addMapDetails())
											toastLong(R.string.fix_error);
										else
											mHandler.post(refreshMapList);
									}

								}
							});

			AlertDialog deploymentDialog = addBuilder.create();
			deploymentDialog.show();
			break;
		}

	}

	/**
	 * Load Map details from the web
	 */
	class LoadMapTask extends ProgressTask {

		protected Boolean status;

		protected Context appContext;

		private MapsHttpClient maps;

		protected String distance;

		protected Location location;

		public LoadMapTask(FragmentActivity activity) {
			super(activity, R.string.loading_);
			// switch to a progress animation
			refresh.setActionView(R.layout.indeterminate_progress_action);
			maps = new MapsHttpClient(ListMapActivity.this);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.cancel();
		}

		@Override
		protected Boolean doInBackground(String... strings) {
			try {
				status = maps.fetchMaps(distance, location);

				Thread.sleep(1000);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
			return status;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (!result) {

				toastShort(R.string.could_not_fetch_data);
			} else {

				toastShort(R.string.deployment_fetched_successful);
			}
			refresh.setActionView(null);
			listMapAdapter.refresh();
			listMapView.mProgressBar.setVisibility(View.GONE);
			listMapView.displayEmptyListText();
		}

	}

	/**
	 * Load the map's report
	 */

	class FetchMapReportTask extends ProgressTask {

		protected int id;

		protected Integer status;

		public FetchMapReportTask(Activity activity) {
			super(activity, R.string.please_wait);
			// pass custom loading message to super call
		}

		@Override
		protected Boolean doInBackground(String... strings) {
			try {
				if (id != 0) {
					listMapModel.activateDeployment(ListMapActivity.this, id);

					if (!apiUtils.isCheckinEnabled()) {

						// fetch categories
						new CategoriesHttpClient(ListMapActivity.this)
								.getCategoriesFromWeb();
						// fetch reports

						status = new ReportsHttpClient(ListMapActivity.this)
								.getAllReportFromWeb();
					}

					// TODO process checkin if there is one

				} else {
					status = 113;
				}

				Thread.sleep(1000);
				return true;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}

		}

		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			if (success) {
				if (status != null) {
					if (status == 0) {
						onLoaded(success);
					} else if (status == 100) {
						errorMessage = getString(R.string.internet_connection);
						createDialog(DIALOG_SHOW_MESSAGE);
					} else if (status == 99) {
						errorMessage = getString(R.string.failed_to_add_report_online_db_error);
						createDialog(DIALOG_SHOW_MESSAGE);
					} else if (status == 112) {
						errorMessage = getString(R.string.network_error);
						createDialog(DIALOG_SHOW_MESSAGE);
					} else {
						errorMessage = getString(R.string.error_occured);
						createDialog(DIALOG_SHOW_MESSAGE);

					}

				} else {
					toastLong(R.string.could_not_fetch_reports);
				}
				
			} else {
				toastLong(R.string.could_not_fetch_reports);
			}
		}
	}

	@Override
	protected void onLoaded(boolean success) {
		try {

			if (success) {
				
				goToReports();

			} else {
				toastLong(R.string.could_not_fetch_reports);
			}
		} catch (IllegalArgumentException e) {
			log(e.toString());
		}
	}

	/** Location stuff **/
	// Fetches the current location of the device.
	protected void setDeviceLocation() {
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Get last known location from either GPS or Network provider
		Location loc = null;
		boolean netAvail = (mLocationMgr
				.getProvider(LocationManager.NETWORK_PROVIDER) != null);
		boolean gpsAvail = (mLocationMgr
				.getProvider(LocationManager.GPS_PROVIDER) != null);
		if (gpsAvail) {
			loc = mLocationMgr
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else if (netAvail) {
			loc = mLocationMgr
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}

		// Just use last location if it's less than 10 minutes old
		if (loc != null
				&& ((new Date()).getTime() - loc.getTime() < 10 * 60 * 1000)) {
			onLocationChanged(loc);
		} else {
			if (gpsAvail) {
				mLocationMgr.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 0, 0, this);
			}
			if (netAvail) {
				mLocationMgr.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 0, 0, this);
			}
		}
	}

	public void stopLocating() {
		if (mLocationMgr != null) {
			try {
				mLocationMgr.removeUpdates(this);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			mLocationMgr = null;
		}
	}

	public void onLocationChanged(Location loc) {
		if (loc != null) {
			location = loc;
			LoadMapTask deploymentTask = new LoadMapTask(this);
			deploymentTask.location = location;
			deploymentTask.distance = distance;
			deploymentTask.execute();
			stopLocating();
		}

	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	/* (non-Javadoc)
	 * @see com.ushahidi.android.app.activities.BaseListActivity#headerView()
	 */
	@Override
	protected View headerView() {
		return null;
	}

}
