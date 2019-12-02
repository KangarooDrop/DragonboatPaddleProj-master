package com.dfrobot.angelo.blunobasicdemo;

//https://github.com/DFRobot/BlunoBasicDemo
//https://github.com/jjoe64/GraphView/wiki/Realtime-chart

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
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

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserStateDetails;
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
import java.util.Iterator;
import java.util.List;

public class MainActivity  extends BlunoLibrary {

	private Button buttonScan;

	private double startRecordTime = -1;
	private double stopRecordTime = -1;
	private LineGraphSeries<DataPoint> recordPressureSeries = new LineGraphSeries<>();
	private LineGraphSeries<DataPoint> recordAngleXSeries = new LineGraphSeries<>();
	private LineGraphSeries<DataPoint> recordAngleYSeries = new LineGraphSeries<>();
	private LineGraphSeries<DataPoint> recordAngleZSeries = new LineGraphSeries<>();
	private boolean recording = false;

	private Spinner dataSelectionSpinner;
	ArrayList<String> spinnerList = new ArrayList<>();

	private ObjectAnimator firstAnimator;
	private ObjectAnimator secondAnimator;
	private GraphView graph;
	private GraphView transGraph;
	private ArrayList<LineGraphSeries<DataPoint>>[] graphSeries;
	private ArrayList<DataPoint> allGraphSeries;
	private double graphXValue = 0;
	private int selectedPosition = 0;

	private int numOfPoints = 511;

	private OrientationViewer ov;

	private boolean scrollToEndOfGraph = true;

	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

	int displayedView = 0;
	private TableLayout displayTable,
		analyticsTable;

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

		graph = findViewById(R.id.graph);
		graphSeries = new ArrayList[4];
		for(int i = 0; i < 4; i++) {
			graphSeries[i] = new ArrayList<>();
			graphSeries[i].add(new LineGraphSeries<DataPoint>());
		}
		graphSeries[1].get(0).setColor(0xffff0000);
		graphSeries[2].get(0).setColor(0xff00ff00);
		graphSeries[3].get(0).setColor(0xff0000ff);
		graph.getViewport().setXAxisBoundsManual(true);
		graph.getViewport().setMinX(0);
		graph.getViewport().setMaxX(5);
		graph.getViewport().setScrollable(true);
		graph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
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
		});
		DisplayMetrics mets = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mets);
		final int width = mets.widthPixels;

		firstAnimator = ObjectAnimator.ofFloat(graph, "translationX", width);
		firstAnimator.setDuration(200);
		secondAnimator = ObjectAnimator.ofFloat(graph, "translationX", 0);
		secondAnimator.setDuration(200);

		recordPressureSeries.setColor(0xffff0000);
		recordAngleXSeries.setColor(0xffff0000);
		recordAngleYSeries.setColor(0xffff0000);
		recordAngleZSeries.setColor(0xffff0000);

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
		dataSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
			{
				firstAnimator.start();
				selectedPosition = position;

				graph.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						graph.setX(-width);
						secondAnimator.start();

						scrollToEndOfGraph = (selectedPosition == 0);
						graph.removeAllSeries();
//						graph.addSeries(graphSeries[0].get(selectedPosition));
						graph.addSeries(graphSeries[1].get(selectedPosition));
						graph.addSeries(graphSeries[2].get(selectedPosition));
						graph.addSeries(graphSeries[3].get(selectedPosition));
						if (selectedPosition != 0)
						{
							graph.getViewport().setScrollable(true);
							double startX = graphXValue;
							graphXValue = 0;
							graphSeries[0].get(selectedPosition).resetData(new DataPoint[] {});
							List<String> strData = readFromDataFile(spinnerList.get(selectedPosition));
							boolean pastScroll = scrollToEndOfGraph;
							scrollToEndOfGraph = true;
							for(String line : strData) {
								Log.w("reading", line);
								List<String> splitLine = Arrays.asList(line.split(" "));
								int tIndex = splitLine.indexOf("t:");
								int pIndex = splitLine.indexOf("p:");
								int dIndex = splitLine.indexOf("d:");
								if (tIndex != -1) {
									parseReceived(splitLine.get(pIndex) + " " + splitLine.get(pIndex + 1), selectedPosition);
									parseReceived(splitLine.get(tIndex) + " " + splitLine.get(tIndex + 1), selectedPosition);
									if (dIndex != -1) {
										try {
											graphSeries[1].get(selectedPosition).appendData(new DataPoint(graphXValue, Double.parseDouble(splitLine.get(dIndex + 1))), false, numOfPoints);
											graphSeries[2].get(selectedPosition).appendData(new DataPoint(graphXValue, Double.parseDouble(splitLine.get(dIndex + 2))), false, numOfPoints);
											graphSeries[3].get(selectedPosition).appendData(new DataPoint(graphXValue, Double.parseDouble(splitLine.get(dIndex + 3))), false, numOfPoints);
										}
										catch (IllegalArgumentException e)
										{
											e.printStackTrace();
										}
									}
								}
							}
							graphXValue = startX;
							scrollToEndOfGraph = pastScroll;

						}
						else {
							graph.getViewport().setScrollable(false);
							graph.addSeries(recordPressureSeries);
						}
					}
				}, 200);
			}
			@Override
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
						else
							Toast.makeText(MainActivity.this, "Could not create file with the selected name. Please try again with a different file name", Toast.LENGTH_LONG).show();
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
	}


	private boolean calibrating = false;
	private double calibTimer = 0;

	private double calibGyroX = 0,
			calibGyroY = 0,
			calibGyroZ = 0;
	private int calibNumGyroPoints = 0;

	private double calibPressure = 0;
	private int calibNumPressurePoints = 0;

	private double paddleAngleX = 0,
			paddleAngleY = 0,
			paddleAngleZ = 0;

	private double calibAccX = 0,
			calibAccY = 0,
			calibAccZ = 0,
			averageAccX = 0,
			averageAccY = 0,
			averageAccZ = 0;

	private double averageGyroX = 0,
			averageGyroY = 0,
			averageGyroZ = 0,

	averagePressure;

	private void calibrate()
	{
		calibrating = true;
		calibTimer = 0;

		calibGyroX = 0;
		calibGyroY = 0;
		calibGyroZ = 0;
		calibAccX = 0;
		calibAccY = 0;
		calibAccZ = 0;
		calibNumGyroPoints = 0;

		calibPressure = 0;
		calibNumPressurePoints = 0;
	}

	private double[][] mulMatrix(double[][] m1, double[][] m2)
	{
		double[][] result = new double[3][3];
		for(int i = 0; i < 3; i++)
			for(int j = 0; j< 3; j++)
			{
				double sum = 0;
				for (int k = 0; k < 3; k++)
					sum += m1[i][k] * m2[k][j];
				result[i][j] = sum;
			}
		return result;
	}


/*
	private void addStringToGraph(String line, int index)
	{
		try {
			List<String> lineData = Arrays.asList(line.split(" "));
				int timeStepIndex = lineData.indexOf("t") + 1;
				int pressureIndex = lineData.indexOf("p") + 1;
				int accelIndex = lineData.indexOf("a") + 1;
				int gyroIndex = lineData.indexOf("g") + 1;
				if (timeStepIndex != 0 && timeStepIndex < lineData.size()) {
					double deltaTime = Double.parseDouble(lineData.get(timeStepIndex));
					if (calibrating) {
						calibTimer += deltaTime;

					}
					if (pressureIndex != 0 && pressureIndex < lineData.size()) {
						double pressureVal = Double.parseDouble(lineData.get(pressureIndex));
						if (!calibrating) {
							DataPoint receivedPoint = new DataPoint(graphXValue, pressureVal);
							series.appendData(receivedPoint, scrollToEndOfGraph, numOfPoints);
						} else {
							calibPressure += pressureVal;
							++calibNumPressurePoints;
						}
					}

					if (series.equals(graphSeries.get(0))) {
						if (gyroIndex != 0 && gyroIndex + 3 < lineData.size() && accelIndex != 0 && accelIndex + 3 < lineData.size()) {

							double gyroX = Double.parseDouble(lineData.get(gyroIndex + 0)),
									gyroY = Double.parseDouble(lineData.get(gyroIndex + 1)),
									gyroZ = Double.parseDouble(lineData.get(gyroIndex + 2));
							double accX = Double.parseDouble(lineData.get(accelIndex + 0)),
									accY = Double.parseDouble(lineData.get(accelIndex + 1)),
									accZ = Double.parseDouble(lineData.get(accelIndex + 2));

							Log.w("accel", "" + accZ);
							if (!calibrating) {
								ov.setAngles(paddleAngleX * Math.PI / 180, paddleAngleY * Math.PI / 180);
								ov.invalidate();



							double k = 0.98;
							paddleAngleX += (gyroX - averageGyroX) * deltaTime;
							paddleAngleY += (gyroY - averageGyroY) * deltaTime;
							paddleAngleZ += (gyroZ - averageGyroZ) * deltaTime;

							double forceMag = Math.sqrt(accX*accX + accY*accY + accZ*accZ);
							if (forceMag >= -20 && forceMag <= 20)
							{
								double accAngleX = - Math.atan2(accY, accZ) * 180 / Math.PI;
								paddleAngleX = SmoothAngle(paddleAngleX, accAngleX);
								double accAngleY = Math.atan2(accX, accZ)* 180 / Math.PI;
								paddleAngleY = SmoothAngle(paddleAngleY, accAngleY);
								double accAngleZ = - Math.atan2(accX, accY)* 180 / Math.PI;
								paddleAngleZ = SmoothAngle(paddleAngleZ, accAngleZ);
							}

							//Removing gravity from force vector and double integrating to determine change in position
							double x = -paddleAngleX * Math.PI / 180,
									y = paddleAngleZ * Math.PI / 180,
									z = -paddleAngleY * Math.PI / 180;
							double [][] matX = new double[][]
									{
											{1, 0, 0},
											{0, Math.cos(x), Math.sin(x)},
											{0, -Math.sin(x), Math.cos(x)}
									},
								matY = new double[][]
										{
												{Math.cos(y), 0, -Math.sin(y)},
												{0, 1, 0},
												{Math.sin(y), 0, Math.cos(y)}
										},
								matZ = new double[][]
										{
												{Math.cos(z), Math.sin(z), 0},
												{-Math.sin(z), Math.cos(z), 0},
												{0, 0, 1}
										},
								axes = new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

							axes = mulMatrix(axes, matX);
							axes = mulMatrix(axes, matY);
							axes = mulMatrix(axes, matZ);

							Log.w("gottem", "g=" + axes[1][0]*9.8 + " "  +  axes[1][2]*9.8 + " " + axes[1][1]*9.8);
							Log.w("gottem", "a=" + accX + " " + accY + " " + accZ);


							} else {
								calibGyroX += gyroX;
								calibGyroY += gyroY;
								calibGyroZ += gyroZ;
								calibAccX += accX;
								calibAccY += accY;
								calibAccZ += accZ;
								++calibNumGyroPoints;
							}
						}

					}
						if (calibrating && calibTimer >= 3) {
							averagePressure = calibPressure / calibNumPressurePoints;

							averageGyroX = calibGyroX / calibNumGyroPoints;
							averageGyroY = calibGyroY / calibNumGyroPoints;
							averageGyroZ = calibGyroZ / calibNumGyroPoints;

							paddleAngleX = 0;
							paddleAngleY = 0;
							paddleAngleZ = 0;

							averageAccX = calibAccX / calibNumGyroPoints;
							averageAccY = calibAccY / calibNumGyroPoints;
							averageAccZ = calibAccZ / calibNumGyroPoints;

							calibrating = false;
						}


						if (!calibrating)
							graphXValue += deltaTime;

				}

		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}*/

	static double k = 0.98;
	public static double SmoothAngle(double gyroAngle, double accAngle)
	{
		if ((accAngle+360-gyroAngle) < (gyroAngle - accAngle) && (gyroAngle - accAngle) >= 0)
		{
				return Lerp(gyroAngle, accAngle + 360, k) % 360;
		}
		else if ((gyroAngle+360-accAngle) < (accAngle - gyroAngle) && (accAngle - gyroAngle) >= 0)
		{
			return Lerp(gyroAngle + 360, accAngle, k) % 360;
		}
		return Lerp(gyroAngle, accAngle, k);
	}

	public static double Lerp(double a, double b, double t)
	{
		return a * t + b * (1-t);
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
		switch (theConnectionState) {											//Four connection state
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


	double timeReceived,
			pressureReceived;
	Vector3 accReceived = new Vector3(),
			gyroReceived = new Vector3();

	private void parseReceived (String line, int position)
	{
		try {
			if (line.length() > 0) {
				Log.w("parse_received", "|" + line);
				List<String> lineData = Arrays.asList(line.split(" "));
				switch (line.charAt(0)) {
					case 't':
						if (lineData.size() > 1)
							timeReceived = Double.parseDouble(lineData.get(1));

						if (!calibrating) {
							DataPoint receivedPoint = new DataPoint(graphXValue, pressureReceived);
							graphSeries[0].get(position).appendData(receivedPoint, (position == 0 ? scrollToEndOfGraph : false), numOfPoints);
							if (position == 0 && recording)
							{
								recordPressureSeries.appendData(receivedPoint, scrollToEndOfGraph, numOfPoints);
								recordAngleXSeries.appendData(new DataPoint(graphXValue, paddleAngleX), scrollToEndOfGraph, numOfPoints);
								recordAngleYSeries.appendData(new DataPoint(graphXValue, paddleAngleY), scrollToEndOfGraph, numOfPoints);
								recordAngleZSeries.appendData(new DataPoint(graphXValue, paddleAngleZ), scrollToEndOfGraph, numOfPoints);
							}
							graphXValue += timeReceived;

							double k = 0.95;
							paddleAngleX += (gyroReceived.x - averageGyroX) * timeReceived;
							paddleAngleY += (gyroReceived.y - averageGyroY) * timeReceived;
							paddleAngleZ += (gyroReceived.z - averageGyroZ) * timeReceived;

							double forceMag = accReceived.len();
							if (forceMag >= -20 && forceMag <= 20) {
								double accAngleX = -Math.atan2(accReceived.y, accReceived.z) * 180 / Math.PI;
								paddleAngleX = SmoothAngle(paddleAngleX, accAngleX);
								double accAngleY;
								if (accReceived.z != 0)
									accAngleY = Math.atan(accReceived.x / accReceived.z) * 180 / Math.PI;
								else accAngleY = Math.PI/2;
								paddleAngleY = SmoothAngle(paddleAngleY, accAngleY);
								double accAngleZ = -Math.atan2(accReceived.x, accReceived.y) * 180 / Math.PI;
								paddleAngleZ = SmoothAngle(paddleAngleZ, accAngleZ);
							}

							if (position == 0) {
								graphSeries[1].get(0).appendData(new DataPoint(graphXValue, paddleAngleX), (position == 0 ? scrollToEndOfGraph : false), numOfPoints);
								graphSeries[2].get(0).appendData(new DataPoint(graphXValue, paddleAngleY), (position == 0 ? scrollToEndOfGraph : false), numOfPoints);
								graphSeries[3].get(0).appendData(new DataPoint(graphXValue, paddleAngleZ), (position == 0 ? scrollToEndOfGraph : false), numOfPoints);
							}
							if (selectedPosition == 0) {
								ov.setAngles(paddleAngleX * Math.PI / 180, paddleAngleY * Math.PI / 180);
								ov.invalidate();
							}


						} else {
							calibTimer += timeReceived;

							calibPressure += pressureReceived;
							++calibNumPressurePoints;

							calibGyroX += gyroReceived.x;
							calibGyroY += gyroReceived.y;
							calibGyroZ += gyroReceived.z;
							calibAccX += accReceived.x;
							calibAccY += accReceived.y;
							calibAccZ += accReceived.z;
							++calibNumGyroPoints;

							if (calibTimer >= 3) {
								averagePressure = calibPressure / calibNumPressurePoints;

								averageGyroX = calibGyroX / calibNumGyroPoints;
								averageGyroY = calibGyroY / calibNumGyroPoints;
								averageGyroZ = calibGyroZ / calibNumGyroPoints;

								paddleAngleX = 0;
								paddleAngleY = 0;
								paddleAngleZ = 0;

								averageAccX = calibAccX / calibNumGyroPoints;
								averageAccY = calibAccY / calibNumGyroPoints;
								averageAccZ = calibAccZ / calibNumGyroPoints;

								calibrating = false;
							}
						}

						break;
					case 'p':
						if (lineData.size() > 1)
							pressureReceived = Double.parseDouble(lineData.get(1));
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
							gyroReceived.x = Double.parseDouble(lineData.get(1));
							gyroReceived.y = Double.parseDouble(lineData.get(2));
							gyroReceived.z = Double.parseDouble(lineData.get(3));
						}
						break;
					default:

						break;
				}
			}
		}
		catch(Exception e)
		{

		}
	}



	String wholeLine = "";
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

	private boolean writeToDataFile(ArrayList<String> data, String fileName)
	{
		try
		{
			File parentFolder = getDir("dataset", MODE_PRIVATE);
			File outFile = new File(parentFolder, fileName + ".txt");
			FileOutputStream fos = new FileOutputStream(outFile);
			for(int i = 0; i < data.size(); i++)
				fos.write((data.get(i) + "\n").getBytes());
			fos.close();
			Log.w("Writing", "Writing to data log file");
			return true;
		}
		catch (IOException e)
		{
			Log.w("Exception", "Error writing to file: " + e.toString());
			e.printStackTrace();
			return false;
		}
	}

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

	private boolean deleteFileFromDir (String fileName)
	{
		File dir = getDir("dataset", MODE_PRIVATE);
		File f = new File(dir, fileName + ".txt");
		return f.delete();
	}

	private void updateSpinnerList()
	{
		ArrayAdapter<String> adp1 = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, spinnerList);
		adp1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataSelectionSpinner.setAdapter(adp1);
	}
}