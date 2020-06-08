package com.dfrobot.angelo.blunobasicdemo;

//https://github.com/DFRobot/BlunoBasicDemo
//https://github.com/jjoe64/GraphView/wiki/Realtime-chart

import android.view.Gravity;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.*;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MainActivity  extends BlunoLibrary  implements SensorEventListener
{
	/*  Variables responsible for acquireing and storing the accelerometer data from phone on which the app is running  */
	private SensorManager sensorManager;
	private Vector3 phoneAcceleration;

	/*  Reference varialbe for the button which allows the user to connect to the Bluno and displays the status to the user  */
	private Button buttonScan;

	/*  View responsible for displaying analytical information in the for of text strings to the user  */
	private TextView analyticsTextView;

	/*  Variables responsible for determining when the recording was initialized and ended as well as the data to be saved  */
	private double startRecordTime = -1;
	private double stopRecordTime = -1;
	private LineGraphSeries<DataPoint> recordPressureSeries = new LineGraphSeries<>();
	private LineGraphSeries<DataPoint> recordAngleXSeries = new LineGraphSeries<>();
	private LineGraphSeries<DataPoint> recordAngleYSeries = new LineGraphSeries<>();
	private LineGraphSeries<DataPoint> recordAngleZSeries = new LineGraphSeries<>();
	private boolean recording = false;
	private double recordingTimer = 0;

	/*  Variables for controlling the drop-down menu and its contents  */
	private Spinner dataSelectionSpinner;
	ArrayList<String> spinnerList = new ArrayList<>();

	private int selectedPosition = 0;

	/*  Variables for the displaying the graph information to the user and storing the data displayed  */
	private GraphView pressureGraph, rotationGraph;
	private ArrayList<LineGraphSeries<DataPoint>>[] graphSeries;
	private double graphXValue = 0;
	private int numOfPoints = 511;
	private boolean scrollToEndOfGraph = true;

	/*  Varialbe for the view which displays the paddle angles to the user  */
	private OrientationViewer ov;

	/*  Variables for controlling which views are visible to the user  */
	int displayedView = 0;
	private TableLayout displayTable,
		analyticsTable;

	/*  Variables used in calibrating the incoming sensor data from the Bluno; accumulation of all sensor inputs  */
	private boolean calibrating = false;
	private int calibNumPoints = 0;
	private double calibTimer = 0;
	private double calibGyroX = 0,
			calibGyroY = 0,
			calibGyroZ = 0;
	private double calibPressure = 0,
			calibPressure2 = 0;

	/*  Variables used after calibration to offset incoming data back to zero  */
	private double paddleAngleX = 0,
			paddleAngleY = 0,
			paddleAngleZ = 0;
	private double averageAccX = 0,
			averageAccY = 0,
			averageAccZ = 0;
	private double averageGyroX = 0,
			averageGyroY = 0,
			averageGyroZ = 0;
	private double averagePressure,
			averagePressure2;

	/*  Variable used by onSerialReceived to ensure no data is lost when the input data is segmented between different calls of onSerialReceived  */
	String wholeLine = "";

	/*  Variables used to store information about the information received from the Bluno and analytic info displayed in the view  */
	double timeReceived,
			pressureReceived,
			pressureReceivedLast,
			pressureReceived2,
			forceExertedTotal;
	Vector3 accReceived = new Vector3(),
			gyroReceived = new Vector3();
	Vector3 waterAngleEnter = new Vector3(), waterAngleExit = new Vector3(),
			averageWaterAngleEnter = new Vector3(), averageWaterAngleExit = new Vector3(),
			sumWaterAngleEnter = new Vector3(), sumWaterAngleExit = new Vector3();
	int waterAngleEnterN = 0, waterAngleExitN = 0;
	double waterTimeEnter = 0, waterTimeExit = 0, waterTimeEnterLast, waterTimeExitLast,
			strokeDuration, strokeFrequency;
	boolean inWater = false;
	double averageOutwardAngle = 0, sumOutwardAngle = 0;
	int averageOutwardAngleN = 0;

	/*    */
	static double k = 0.02;

	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case 1: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.w("PKM", "Access granted");
				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					Log.w("PKM", "Access denied");
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);


		setContentView(R.layout.activity_main);
		onCreateProcess();														//onCreate Process by BlunoLibrary

		serialBegin(115200);

		ov = findViewById(R.id.orient_view);

		analyticsTextView = findViewById(R.id.analytics_text);

		pressureGraph = findViewById(R.id.graph);
		rotationGraph = findViewById(R.id.graph2);
		/*  Initializing the graph series with empty data, setting appropriate colors, and setting bounds  */
		graphSeries = new ArrayList[4];
		for(int i = 0; i < 4; i++) {
			graphSeries[i] = new ArrayList<>();
			graphSeries[i].add(new LineGraphSeries<DataPoint>());
		}
		graphSeries[1].get(0).setColor(0xffff0000);
		graphSeries[2].get(0).setColor(0xff00ff00);
		graphSeries[3].get(0).setColor(0xff0000ff);
		pressureGraph.getViewport().setXAxisBoundsManual(true);
		pressureGraph.getViewport().setYAxisBoundsManual(true);
		pressureGraph.getViewport().setMinX(0);
		pressureGraph.getViewport().setMaxX(5);
		pressureGraph.getViewport().setMinY(0);
		pressureGraph.getViewport().setMaxY(23);

		rotationGraph.getViewport().setXAxisBoundsManual(true);
		rotationGraph.getViewport().setYAxisBoundsManual(true);
		rotationGraph.getViewport().setMinX(0);
		rotationGraph.getViewport().setMaxX(5);
		rotationGraph.getViewport().setScrollable(true);
		rotationGraph.getViewport().setMinY(-210);
		rotationGraph.getViewport().setMaxY(210);
		rotationGraph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
			@Override
			public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
				if (selectedPosition != 0)
				{
					float x = (float)(maxX + minX)/2;
					double[] data = new double[3];
					for(int j = 0; j < 3; j++) {
						Iterator<DataPoint> dp = graphSeries[1+j].get(selectedPosition).getValues(x-timeReceived, x+timeReceived);
						if (dp != null && dp.hasNext())
							data[j] = dp.next().getY();
					}
					ov.setAngles(data[0] * Math.PI / 180, data[1] * Math.PI / 180);
					ov.invalidate();
				}




			}


			/*
				AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
					@Override
					public void onResult(UserStateDetails userStateDetails) {
						try {
							Amplify.addPlugin(new AWSS3StoragePlugin());
							Amplify.configure(getApplicationContext());
							Log.i("StorageQuickstart", "All set and ready to go!");
						} catch (Exception e) {
							Log.e("StorageQuickstart", e.getMessage());
						}
					}

					@Override
					public void onError(Exception e) {
						Log.e("StorageQuickstart", "Initialization error.", e);
					}
				});

*/


		});
		DisplayMetrics mets = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mets);
		final int width = mets.widthPixels;

		recordPressureSeries.setColor(0xffff0000);
		recordAngleXSeries.setColor(0xff00ffff);
		recordAngleYSeries.setColor(0xffff00ff);
		recordAngleZSeries.setColor(0xffffff00);

		spinnerList.add("Live");
		List<String> fileNames = getAllFilenames();
		for(int i = 0; fileNames != null && i < fileNames.size(); i++)
		{
			for(int j = 0; j < 4; j++)
				graphSeries[j].add(new LineGraphSeries<DataPoint>());
			graphSeries[1].get(i+1).setColor(0xffff0000);
			graphSeries[2].get(i+1).setColor(0xff00ff00);
			graphSeries[3].get(i+1).setColor(0xff0000ff);
			String fileName = fileNames.get(i);
			spinnerList.add(fileName.substring(0, fileName.length() - 4));
		}
		dataSelectionSpinner = findViewById(R.id.data_spinner);
		updateSpinnerList();
		dataSelectionSpinner.setBackgroundResource(android.R.drawable.btn_default);
		dataSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
			{
				selectedPosition = position;

				scrollToEndOfGraph = (selectedPosition == 0);
				pressureGraph.removeAllSeries();
				rotationGraph.removeAllSeries();
				pressureGraph.addSeries(graphSeries[0].get(selectedPosition));
				rotationGraph.addSeries(graphSeries[1].get(selectedPosition));
				rotationGraph.addSeries(graphSeries[2].get(selectedPosition));
				rotationGraph.addSeries(graphSeries[3].get(selectedPosition));
				if (selectedPosition != 0)
				{
					pressureGraph.getViewport().setScrollable(true);
					rotationGraph.getViewport().setScrollable(true);
					for (int i = 0; i < 4; i++)
						graphSeries[i].get(selectedPosition).resetData(new DataPoint[] {});
					graphSeries[1].get(selectedPosition).setColor(0xffff0000);
					graphSeries[2].get(selectedPosition).setColor(0xff00ff00);
					graphSeries[3].get(selectedPosition).setColor(0xff0000ff);
					List<String> strData = readFromDataFile(spinnerList.get(selectedPosition));
					scrollToEndOfGraph = true;
					double fileXVal = 0,
						lastPressure = 0;
					double pressureExertedTotal = 0;
					ArrayList<Vector3> anglesEntering = new ArrayList<Vector3>(), anglesExiting = new ArrayList<Vector3>();
					boolean inWaterRecording = false;
					for(String line : strData) {
						Log.w("reading", line);
						List<String> splitLine = Arrays.asList(line.split(" "));
						int tIndex = splitLine.indexOf("t:");
						int pIndex = splitLine.indexOf("p:");
						int dIndex = splitLine.indexOf("d:");
						if (tIndex != -1) {
							try {
								double deltaTime = Double.parseDouble(splitLine.get(tIndex + 1));
								if (pIndex != -1 && dIndex != -1) {
									double pressure = Double.parseDouble(splitLine.get(pIndex + 1));
									graphSeries[0].get(selectedPosition).appendData(new DataPoint(fileXVal, pressure), false, numOfPoints);
									pressureExertedTotal += getDeltaForce(deltaTime, pressure, lastPressure);

									double angleX = Double.parseDouble(splitLine.get(dIndex + 1)),
											angleY = Double.parseDouble(splitLine.get(dIndex + 2)),
											angleZ = Double.parseDouble(splitLine.get(dIndex + 3));

									if (pressure > 4 && !inWaterRecording) {
										inWaterRecording = true;
										anglesEntering.add(new Vector3(angleX, angleY, angleZ));
									} else if (pressure < 4 && inWaterRecording) {
										inWaterRecording = false;
										anglesExiting.add(new Vector3(angleX, angleY, angleZ));
									}
									lastPressure = pressure;

									graphSeries[1].get(selectedPosition).appendData(new DataPoint(fileXVal, (angleX > 180 ? angleX -360 : (angleX < -180 ? angleX + 360 : angleX))), false, numOfPoints);
									graphSeries[2].get(selectedPosition).appendData(new DataPoint(fileXVal, (angleY > 180 ? angleY -360 : (angleY < -180 ? angleY + 360 : angleY))), false, numOfPoints);
									graphSeries[3].get(selectedPosition).appendData(new DataPoint(fileXVal, (angleZ > 180 ? angleZ -360 : (angleZ < -180 ? angleZ + 360 : angleZ))), false, numOfPoints);
								}
								fileXVal += deltaTime;
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							}
						}
					}
					Vector3[] enteringArray = new Vector3[anglesEntering.size()];
					Vector3[] exitingArray = new Vector3[anglesExiting.size()];
					for(int i = 0; i < anglesEntering.size(); i++)
						enteringArray[i] = anglesEntering.get(i);
					for(int i = 0; i < anglesExiting.size(); i++)
						exitingArray[i] = anglesExiting.get(i);
							Vector3 averageEntering = averageAngles(enteringArray),
								averageExiting = averageAngles(exitingArray);
							String analyticsString = "";
							analyticsString += "Total force exerted: " + pressureExertedTotal + "V\n";
							analyticsString += "Average entering angle: " + String.format("%.2f", averageEntering.x) + ", " + String.format("%.2f", averageEntering.y) + ", " + String.format("%.2f", averageEntering.z) + "\n";
							analyticsString += "Average exiting angle: " + String.format("%.2f", averageExiting.x) + ", " + String.format("%.2f", averageExiting.y) + ", " + String.format("%.2f", averageExiting.z) + "\n";
								analyticsTextView.setText(analyticsString);
						}
				else {
					pressureGraph.getViewport().setScrollable(false);
					rotationGraph.getViewport().setScrollable(false);
					pressureGraph.addSeries(recordPressureSeries);
					rotationGraph.addSeries(recordAngleXSeries);
					rotationGraph.addSeries(recordAngleYSeries);
					rotationGraph.addSeries(recordAngleZSeries);
				}
			}

			public void onNothingSelected(AdapterView<?> arg0)
			{

			}
		});

		buttonScan = (Button)findViewById(R.id.buttonScan);					//initial the button for scanning the BLE device
		buttonScan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				buttonScanOnClickProcess();										//Alert Dialog for selecting the BLE device
			}
		});

		Button LogoutButton = findViewById(R.id.button2);
		LogoutButton.setOnClickListener(new View.OnClickListener()

		{
			@Override
			public void onClick(View view)
			{
				AWSMobileClient.getInstance().signOut();
				Intent i = new Intent(MainActivity.this, LaunchScreen.class);
				startActivity(i);
			}

		});

		Button saveButton = findViewById(R.id.savebutton);
		saveButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				final EditText input = new EditText(MainActivity.this);
				input.setInputType(InputType.TYPE_CLASS_TEXT);

				new AlertDialog.Builder(MainActivity.this).setTitle("Save Data As").setView(input).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						ArrayList<String> dataToSave = new ArrayList<>();
						double lastTime = -1;
						Iterator<DataPoint> iter = recordPressureSeries.getValues(startRecordTime, stopRecordTime);
						Iterator<DataPoint> iterX = recordAngleXSeries.getValues(startRecordTime, stopRecordTime);
						Iterator<DataPoint> iterY = recordAngleYSeries.getValues(startRecordTime, stopRecordTime);
						Iterator<DataPoint> iterZ = recordAngleZSeries.getValues(startRecordTime, stopRecordTime);
						DataPoint currentPoint;
						while (iter.hasNext())
						{
							currentPoint = iter.next();
							if(lastTime == -1)
								lastTime = currentPoint.getX();
							String toSave = "t: " + (currentPoint.getX() - lastTime) + " p: " + currentPoint.getY();
							if (iterX.hasNext() && iterY.hasNext() && iterZ.hasNext())
								toSave += " d: " + iterX.next().getY() + " " + iterY.next().getY() + " " + iterZ.next().getY();
							dataToSave.add(toSave);
							lastTime = currentPoint.getX();
						}
						if (writeToDataFile(dataToSave, input.getText().toString()))
						{
							if (!spinnerList.contains(input.getText().toString())) {
								for(int i = 0; i < 4; i++)
									graphSeries[i].add(new LineGraphSeries<DataPoint>());
								graphSeries[1].get(selectedPosition).setColor(0xffff0000);
								graphSeries[2].get(selectedPosition).setColor(0xff00ff00);
								graphSeries[3].get(selectedPosition).setColor(0xff0000ff);
								spinnerList.add(input.getText().toString());
								updateSpinnerList();				}
							recordPressureSeries.resetData(new DataPoint[] {});
							recordAngleXSeries.resetData(new DataPoint[] {});
							recordAngleYSeries.resetData(new DataPoint[] {});
							recordAngleZSeries.resetData(new DataPoint[] {});
						}
					}
				}).setNegativeButton("Cancel", null).show();
			}
		});

		final Button startButton = findViewById(R.id.startrecordbutton);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!recording) {
					startRecordTime = graphXValue;
					recordPressureSeries.resetData(new DataPoint[]{});
					recordAngleXSeries.resetData(new DataPoint[]{});
					recordAngleYSeries.resetData(new DataPoint[]{});
					recordAngleZSeries.resetData(new DataPoint[]{});
					recording = true;
					startButton.setText("Stop Recording");
				}
				else {
					stopRecordTime = graphXValue;
					recording = false;
					startButton.setText("Start Recording");
				}
			}
		});

		Button downloadButton = findViewById(R.id.downloadButton);
		downloadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				final EditText input = new EditText(MainActivity.this);
				input.setInputType(InputType.TYPE_CLASS_TEXT);

				new AlertDialog.Builder(MainActivity.this).setTitle("File to download:").setView(input).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						downloadWithTransferUtility(input.getText().toString());
						updateSpinnerList();

					}
				}).setNegativeButton("Cancel", null).show();
			}

									   });


			Button closeButton = findViewById(R.id.closebuttton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				String name = dataSelectionSpinner.getSelectedItem().toString();
				if (dataSelectionSpinner.getSelectedItemPosition() != 0) {
					new AlertDialog.Builder(MainActivity.this).setTitle("Are you sure you want to delete \"" + name + "\" forever?")
							.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									String name = dataSelectionSpinner.getSelectedItem().toString();
									if (deleteFileFromDir(name)) {
										int index = spinnerList.indexOf(name);
										for(int j = 0; j < 4; j++)
											graphSeries[j].remove(index);
										spinnerList.remove(name);
										updateSpinnerList();
									}
									else
										Toast.makeText(MainActivity.this, "Could not delete file", Toast.LENGTH_SHORT).show();
								}
							}).setNegativeButton("No", null)
							.show();
				}
				else
					Toast.makeText(MainActivity.this, "Cannot close the live feed", Toast.LENGTH_SHORT).show();
			}
		});

		findViewById(R.id.calibratebutton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view)
			{
				calibrate();
			}
		});

		findViewById(R.id.button3).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (displayedView == 0)
				{
					displayedView = 1;
					displayTable.setVisibility(View.INVISIBLE);
					analyticsTable.setVisibility(View.VISIBLE);
				}
				else if (displayedView == 1)
				{
					displayedView = 0;
					displayTable.setVisibility(View.VISIBLE);
					analyticsTable.setVisibility(View.INVISIBLE);
				}
			}
		});

		displayTable = findViewById(R.id.display_layout);
		analyticsTable = findViewById(R.id.analytics_layout);


		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{

	}

	/*
	 * Purpose: Called when there is a new sensor input and updates the values of phoneAcceleration based on the received acceleration of the phone
	 * Input: event = the SensorEvent
	 * Output: None
	 * */
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
		{
			phoneAcceleration = new Vector3(event.values[0], event.values[1], event.values[2]);
		}
	}
	/*
	 * Purpose: Initializes variables needed to calibrate the sensor input;
	 * 			calibration is performed automatically upon call of parseReceived after calibration call
	 * Input: None
	 * Output: None
	 * */
	private void calibrate()
	{
		forceExertedTotal = 0;
		averageWaterAngleEnter = new Vector3();
		averageWaterAngleExit = new Vector3();


		calibrating = true;
		calibTimer = 0;

		calibGyroX = 0;
		calibGyroY = 0;
		calibGyroZ = 0;
		calibNumPoints = 0;

		calibPressure = 0;
		calibPressure2 = 0;
	}

	/*
	 * Purpose: Lerps a single axis of the hyroscopic and acceleration angles received based on field k
	 * Input: gyroAngle = the angle of the device obtained by the accumulation of the gyroscopic changes
	 *			accAngle = the angle of the device as observed from the accelerometer's gravitational pull
	 * Output: the lerp of the two angles
	 * */
	public static double SmoothAngle(double gyroAngle, double accAngle)
	{
		if ((accAngle+360-gyroAngle) < (gyroAngle - accAngle) && (gyroAngle - accAngle) >= 0)
		{
				return Lerp(accAngle + 360, gyroAngle, k) % 360;
		}
		else if ((gyroAngle+360-accAngle) < (accAngle - gyroAngle) && (accAngle - gyroAngle) >= 0)
		{
			return Lerp(accAngle, gyroAngle + 360, k) % 360;
		}
		return Lerp(gyroAngle, accAngle, k);
	}

	/*
	 * Purpose: Gives a linear interpolation between two scalars by the given weight
	 * Input: a = scalar returned when t = 0
	 * 			b = scalar returned when t = 1
	 * 			t = the weight between 0 and 1 of the two scalars
	 * Output: linear interpolation between a and b by t
	 * */
	public static double Lerp(double a, double b, double t)
	{
		return a + (b - a) * t;
	}

	@Override
	protected void onResume(){
		super.onResume();
		System.out.println("BLUNOActivity onResume");
		onResumeProcess();														//onResume Process by BlunoLibrary
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		onPauseProcess();														//onPause Process by BlunoLibrary
	}

	@Override
	protected void onStop() {
		super.onStop();
		onStopProcess();														//onStop Process by BlunoLibrary
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		onDestroyProcess();														//onDestroy Process by BlunoLibrary
	}

	@Override
	public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
		switch (theConnectionState) {                                            //Four connection state
			case isConnected:
				buttonScan.setText("Connected");
				break;
			case isConnecting:
				buttonScan.setText("Connecting");
				break;
			case isToScan:
				buttonScan.setText("Scan");
				break;
			case isScanning:
				buttonScan.setText("Scanning");
				break;
			case isDisconnecting:
				buttonScan.setText("isDisconnecting");
				break;
			default:
				break;
		}
	}

	/*
	 * Purpose: Processes a single data entry;
	 * 			if not calibrating, updates the paddle data fields with the received data, updates orientation view, and updates analytic data and view
	 * 			if calibrating, updates the calibration fields
	 * Input: line = the line to be parsed (ex. "p 152")
	 * 			position = the element of graphSeries that is to be appended to with the received data
	 * Output: None
	 * */
	private void parseReceived (String line, int position)
	{
		try {
			if (line.length() > 0) {
				Log.w("parse_received", "[]" + line);
				List<String> lineData = Arrays.asList(line.split(" "));
				switch (line.charAt(0)) {
					case 't':
						if (lineData.size() > 1)
							timeReceived = Double.parseDouble(lineData.get(1));

						if (!calibrating)
						{
							DataPoint receivedPoint = new DataPoint(graphXValue, pressureReceived);
							graphSeries[0].get(position).appendData(receivedPoint, (selectedPosition == 0 ? scrollToEndOfGraph : false), numOfPoints);
							graphSeries[1].get(position).appendData(new DataPoint(graphXValue, paddleAngleX/*(paddleAngleX > 180 ? paddleAngleX -360 : (paddleAngleX < -180 ? paddleAngleX + 360 : paddleAngleX))*/), (selectedPosition == 0 ? scrollToEndOfGraph : false), numOfPoints);
							graphSeries[2].get(position).appendData(new DataPoint(graphXValue, (paddleAngleY > 180 ? paddleAngleY -360 : (paddleAngleY < -180 ? paddleAngleY + 360 : paddleAngleY))), (selectedPosition == 0 ? scrollToEndOfGraph : false), numOfPoints);
							graphSeries[3].get(position).appendData(new DataPoint(graphXValue, (paddleAngleZ > 180 ? paddleAngleZ -360 : (paddleAngleZ < -180 ? paddleAngleZ + 360 : paddleAngleZ))), (selectedPosition == 0 ? scrollToEndOfGraph : false), numOfPoints);
							if (position == 0 && recording)
							{
								recordPressureSeries.appendData(receivedPoint, scrollToEndOfGraph, numOfPoints);
								recordAngleXSeries.appendData(new DataPoint(graphXValue, paddleAngleX), false, numOfPoints);
								recordAngleYSeries.appendData(new DataPoint(graphXValue, paddleAngleY), false, numOfPoints);
								recordAngleZSeries.appendData(new DataPoint(graphXValue, paddleAngleZ), false, numOfPoints);
							}
							graphXValue += timeReceived;
							if (recording)
							    recordingTimer += timeReceived;
							else recordingTimer = 0;

							paddleAngleX -= (gyroReceived.x) * timeReceived;
							paddleAngleY += (gyroReceived.y) * timeReceived;
							paddleAngleZ += (gyroReceived.z) * timeReceived;

							double forceMag = accReceived.len();
							if (forceMag >= -20 && forceMag <= 20) {
								double accAngleX = -Math.atan2(-accReceived.y, accReceived.z) * 180 / Math.PI;
								paddleAngleX = SmoothAngle(paddleAngleX, accAngleX);
								double accAngleY;
								if (accReceived.z != 0)
									accAngleY = Math.atan(accReceived.x / accReceived.z) * 180 / Math.PI;
								else accAngleY = Math.PI/2;
								paddleAngleY = SmoothAngle(paddleAngleY, accAngleY);
								double accAngleZ = -Math.atan2(accReceived.x, accReceived.y) * 180 / Math.PI;
								paddleAngleZ = SmoothAngle(paddleAngleZ, accAngleZ);
							}

							if (selectedPosition == 0) {
								ov.setAngles(paddleAngleX * Math.PI / 180, -paddleAngleY * Math.PI / 180);
								ov.invalidate();
							}

							String analyticsString = "";
                            analyticsString += "Recording for " + getTimeString(recordingTimer) + "\n";

							double forceExerted = getDeltaForce(timeReceived, pressureReceived, pressureReceivedLast);
							forceExertedTotal += forceExerted;
							String forceString = ""+forceExerted,
									totalForceString = "" + forceExertedTotal;
							analyticsString += "Force Exerted: " + forceString + "V\n"
									+ "Total Force Exerted: " + totalForceString + "V\n";

							if (!inWater && pressureReceived > 4)
							{
								inWater = true;
								waterAngleEnter = new Vector3(paddleAngleX, paddleAngleY, paddleAngleZ);
								waterAngleEnterN++;
								sumWaterAngleEnter.add(waterAngleEnter);
								averageWaterAngleEnter = Vector3.div(sumWaterAngleEnter, waterAngleEnterN);
								waterTimeEnter = graphXValue;
								strokeFrequency = 1.0/(waterTimeEnter - waterTimeEnterLast);

								waterTimeEnterLast = waterTimeEnter;
							}
							else if (inWater && pressureReceived < 4)
							{
								inWater = false;
								waterAngleExit = new Vector3(paddleAngleX, paddleAngleY, paddleAngleZ);
								waterAngleExitN++;
								sumWaterAngleExit.add(waterAngleExit);
								averageWaterAngleExit = Vector3.div(sumWaterAngleExit, waterAngleExitN);
								waterTimeExit = graphXValue;
								strokeDuration = waterTimeExit - waterTimeEnter;
								strokeFrequency = (strokeFrequency + 1.0/(waterTimeExit - waterTimeExitLast))/2;

								waterTimeExitLast = waterTimeExit;
							}
							analyticsString += "Last entering angle: " + String.format("%.2f", waterAngleEnter.x) + ", " + String.format("%.2f", waterAngleEnter.y) + ", " + String.format("%.2f", waterAngleEnter.x) + "\n" +
									"Average entering angle: " + String.format("%.2f", averageWaterAngleEnter.x) + ", " + String.format("%.2f", averageWaterAngleEnter.y) + ", " + String.format("%.2f", averageWaterAngleEnter.z) + "\n";
							analyticsString += "Last exiting angle: " + String.format("%.2f", waterAngleExit.x) + ", " + String.format("%.2f", waterAngleExit.y) + ", " + String.format("%.2f", waterAngleExit.z) + "\n" +
									"Average exiting angle: " + String.format("%.2f", averageWaterAngleExit.x) + ", " + String.format("%.2f", averageWaterAngleExit.y) + ", " + String.format("%.2f", averageWaterAngleExit.z) + "\n";
							analyticsString += "Last stroke duration: " + String.format("%.2f", strokeDuration) + "s\n";
							analyticsString += "Last stroke frequency: " + String.format("%.2f", strokeFrequency) + "s^-1\n";
                            analyticsString += "Average outward angle: " + String.format("%.2f", (averageWaterAngleEnter.x + averageWaterAngleExit.x) / 2) + " degrees\n";
                            analyticsString += "Velocity of the phone: (" + String.format("%.2f", phoneAcceleration.x) + ", " + String.format("%.2f", phoneAcceleration.y) + ", " + String.format("%.2f", phoneAcceleration.z) + ")\n";

							pressureReceivedLast = pressureReceived;

							if (selectedPosition == 0)
								analyticsTextView.setText(analyticsString);

						} else {
							calibTimer += timeReceived;

							calibPressure += pressureReceived;
							calibPressure += pressureReceived2;
							++calibNumPoints;

							calibGyroX += gyroReceived.x;
							calibGyroY += gyroReceived.y;
							calibGyroZ += gyroReceived.z;

							if (calibTimer >= 3) {
								averagePressure = calibPressure / calibNumPoints;
								averagePressure2 = calibPressure2 / calibNumPoints;

								averageGyroX = calibGyroX / calibNumPoints;
								averageGyroY = calibGyroY / calibNumPoints;
								averageGyroZ = calibGyroZ / calibNumPoints;

								paddleAngleX = 0;
								paddleAngleY = 0;
								paddleAngleZ = 0;

								calibrating = false;
							}
						}

						break;
					case 'p':
						if (lineData.size() > 1)
							pressureReceived = Double.parseDouble(lineData.get(1)) - (calibrating ? 0 : averagePressure);
						break;
					case 'q':
						if (lineData.size() > 1)
							pressureReceived2 = Double.parseDouble(lineData.get(1)) - (calibrating ? 0 : averagePressure2);
						break;
					case 'a':
						if (lineData.size() > 3) {
							accReceived.x = Double.parseDouble(lineData.get(1));
							accReceived.y = Double.parseDouble(lineData.get(2));
							accReceived.z = Double.parseDouble(lineData.get(3));
						}
						break;
					case 'g':
						if (lineData.size() > 3) {
							gyroReceived.x = Double.parseDouble(lineData.get(1)) - (calibrating ? 0 : averageGyroX);
							gyroReceived.y = Double.parseDouble(lineData.get(2)) - (calibrating ? 0 : averageGyroY);
							gyroReceived.z = Double.parseDouble(lineData.get(3)) - (calibrating ? 0 : averageGyroZ);
						}
						break;
					default:

						break;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/*
	 * Purpose: Calculates the total amount of force exterted over the timestep;
	 * 			Function is more accurate with lower deltaTime and higher number of calls
	 * Input: deltaTime = the amount of time that's elapsed since the last function call
	 * 			force = the amount of force being exerted at function call
	 * 			lastForce = the amount of force exerted at last function call
	 * Output: the total amount of force that has been exerted over the given period of time
	 * */
	public double getDeltaForce(double deltaTime, double force, double lastForce)
	{
		return (force + lastForce) / 2 * deltaTime;
	}

	/*
	 * Purpose: Computes the mean of an array of Vector3
	 * Input: angles = the array of Vector3
	 * Output: the mean of the array
	 * */
	public Vector3 averageAngles(Vector3[] angles)
	{
		Vector3 sum = new Vector3();
		for(Vector3 v : angles)
			sum.add(v);
		sum.div(angles.length);
		return sum;
	}

	/*
	 * Purpose: Updating the text of the view containing analytic data
	 * Input: analyticsString = the string to be set as the text data for the view
	 * Output: None
	 * */
	public void updateAnalyticsText(String analyticsString)
	{
		analyticsTextView.setText(analyticsString);
	}

	/*
	 * Purpose: Function call when the app receives data from the bluetooth source
	 * 				Runs through each character of the line recording each until a sentinel character is observed then calls parseReceived on the recorded data before resetting and repeating
	 * Input: line = the data received from the bluetooth source
	 * Output: None
	 * */
	@Override
	public void onSerialReceived(String line)
	{
		for(char c : line.toCharArray())
		{
			if (c == 't' || c == 'p' || c == 'a' || c == 'g')
			{
				parseReceived(wholeLine, 0);
				wholeLine = "";
			}
			wholeLine += c;
		}
	}

	/*
	 * Purpose: Converts from total number of seconds to an English representation in days, hours, minutes, seconds
	 * Input: time = the elapsed time in seconds to convert
	 * Output: A string where the time is in terms X day(s), Y hour(s), Z minute(s), W second(s) (omitting if any are equal to zero)
	 * */
	private String getTimeString(double time)
	{
		int d = 0, h = 0, m = 0, s = 0;
		s = (int)(time % 60);
		if (time >= 60)
		{
			m = (int)(time / 60);
			if (m >= 60)
			{
				h = m / 60;
				m = m % 60;
				if (h >= 24)
				{
					d = h / 24;
					h = h % 24;
				}
			}
		}
		return (d > 0 ? d + " day" + (d>1?"s,":",") : "") + (h > 0 ? " " + h + " hour"+(h>1?"s,":",") : "") + (m > 0 ? " " + m + " minute"+(m>1?"s,":",") : "") + " " + s + " second"+(s>1?"s":"");
	}

	/*
	 * Purpose: Creates a new file with each element of data being a new line
	 * Input: data = a list of strings representing the data at each timestep
	 * 			fileName = the name of the file(no path, no extension)
	 * Output: A list of strings containing raw data for each timestep
	 * */
	private boolean writeToDataFile(ArrayList<String> data, String fileName)
	{
		File parentFolder = null;
		File outFile = null;
		boolean fileWorked = true;
		try
		{
			parentFolder = getDir("dataset", MODE_PRIVATE);
			outFile = new File(parentFolder, fileName + ".txt");
			FileOutputStream fos = new FileOutputStream(outFile);
			for(int i = 0; i < data.size(); i++)
				fos.write((data.get(i) + "\n").getBytes());
			fos.close();
			Log.w("Writing", "Writing to data log file");
			fileWorked = true;
		}
		catch (IOException e)
		{
			Log.w("Exception", "Error writing to file: " + e.toString());
			e.printStackTrace();
			fileWorked =  false;
			Toast mrT = Toast.makeText(MainActivity.this, "Could not create file with the selected name. Please try again with a different file name", Toast.LENGTH_LONG);;
			mrT.setGravity(Gravity.TOP, 0, 0);
			mrT.show();
		}

		try
		{
			uploadWithTransferUtility(outFile);
			fileWorked = true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fileWorked = false;
			Toast mrT = Toast.makeText(MainActivity.this, "Failed to upload the file to the cloud server", Toast.LENGTH_LONG);;
			mrT.setGravity(Gravity.TOP, 0, 0);
			mrT.show();
		}
		return fileWorked;
	}

	/*
	 * Purpose: Retrieves the raw data within the requested file
	 * Input: fileName = the name of the file(no path, no extension)
	 * Output: A list of strings containing raw data for each timestep
	 * */
	private ArrayList<String> readFromDataFile(String fileName)
	{
		try
		{
			File parentFolder = getDir("dataset", MODE_PRIVATE);
			File outFile = new File(parentFolder, fileName + ".txt");
			FileInputStream fis = new FileInputStream(outFile);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			ArrayList<String> logData = new ArrayList<>();
			String line;
			while((line = br.readLine()) != null)
				logData.add(line);
			fis.close();
			Log.w("Writing", "Reading from data log file");
			return logData;
		}
		catch (IOException e)
		{
			Log.w("Exception", "Error reading from file");
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	/*
	 * Purpose: Get a list of names of all files in the dataset directory
	 * Input: None
	 * Output: List of files within the dataset directory; if none exist, returns empty list
	 * */
	private List<String> getAllFilenames ()
	{
		List<String> fileNames = new ArrayList<>();
		File dir = getDir("dataset", MODE_PRIVATE);
		if (dir != null && dir.list() != null)
			Collections.addAll(fileNames, dir.list());
		else
			return new ArrayList<>();
		return fileNames;
	}

	/*
	* Purpose: Deletes a .txt file from the directory based on the name
	* Input: fileName = Name of the file (no path, no extension)
	* Output: true if the file was deleted; otherwise false
	* */
	private boolean deleteFileFromDir (String fileName)
	{
		File dir = getDir("dataset", MODE_PRIVATE);
		File f = new File(dir, fileName + ".txt");
		return f.delete();
	}

	/*
	 * Purpose: Updates the values of the spinner to those within spinnerList
	 * Input: None
	 * Output: None
	 * */
	private void updateSpinnerList()
	{
		ArrayAdapter<String> adp1 = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, spinnerList);
		adp1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		if (!adp1.equals(dataSelectionSpinner.getAdapter()))
			dataSelectionSpinner.setAdapter(adp1);
	}

	public void uploadWithTransferUtility(File file) {

		TransferUtility transferUtility =
				TransferUtility.builder()
						.context(getApplicationContext())
						.awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
						.s3Client(new AmazonS3Client(AWSMobileClient.getInstance()))
						.build();


		TransferObserver uploadObserver =
				transferUtility.upload(
						AWSMobileClient.getInstance().getIdentityId()+"/"+file.getName(),
						file);

		// Attach a listener to the observer to get state update and progress notifications
		uploadObserver.setTransferListener(new TransferListener() {

			@Override
			public void onStateChanged(int id, TransferState state) {
				if (TransferState.COMPLETED == state) {
					Log.w("AWS", "uploaded file");
				}
			}

			@Override
			public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
				float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
				int percentDone = (int)percentDonef;

				Log.d("AWS", "ID:" + id + " bytesCurrent: " + bytesCurrent
						+ " bytesTotal: " + bytesTotal + " " + percentDone + "%");
			}

			@Override
			public void onError(int id, Exception ex) {
				Log.w("AWS", ex);
			}

		});

		// If you prefer to poll for the data, instead of attaching a
		// listener, check for the state and progress in the observer.
		if (TransferState.COMPLETED == uploadObserver.getState()) {
			// Handle a completed upload.
		}

		Log.d("AWS", "Bytes Transferred: " + uploadObserver.getBytesTransferred());
		Log.d("AWS", "Bytes Total: " + uploadObserver.getBytesTotal());
	}



    private void downloadWithTransferUtility(String fileName) {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

		//str.substring(0, str.length() - 1)
        File parentFolder = getDir("dataset", MODE_PRIVATE);
        File file = new File(parentFolder, fileName.substring(0, fileName.length() - 4) + ".cloud.txt");

        // Initiate the download
        //TransferObserver observer = transferUtility.download(AWSMobileClient.getInstance().getIdentityId()+"/"+fileName, file);


		Log.w("AWS", "trying to get: " + AWSMobileClient.getInstance().getIdentityId()+"/"+fileName);

        TransferObserver downloadObserver =
                transferUtility.download(
                        AWSMobileClient.getInstance().getIdentityId()+"/"+fileName,
                        file);

        // Attach a listener to the observer to get state update and progress notifications
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
					Log.w("AWS", "fileDownloaded ");
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d("AWS", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
				Log.w("AWS", ex);
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == downloadObserver.getState()) {
            // Handle a completed upload.

        }

        Log.d("AWS", "Bytes Transferrred: " + downloadObserver.getBytesTransferred());
        Log.d("AWS", "Bytes Total: " + downloadObserver.getBytesTotal());
    }

}