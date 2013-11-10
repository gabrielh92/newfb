package com.example.cvloc;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import au.com.bytecode.opencsv.CSVWriter;

public class BasicMain extends Activity implements SensorEventListener {

	final double THRESHOLD = 10.7;
	final double SMOOTHING_ACC = 1.0;
	final double SMOOTHING_GYR = 2.0;
	final double SMOOTHING_MAG = 2.0;
	final double SMOOTHING_LIG = 2.0;

	int stepCount;
	long prevAccTime, prevGyrTime, prevMagTime, prevLigTime;
	long accCounter, gyrCounter, magCounter, ligCounter;
	long accTimeDiff, gyrTimeDiff, magTimeDiff, ligTimeDiff;
	long accTime, gyrTime, magTime, ligTime;
	float accX, accY, accZ;
	float prevAccX, prevAccY, prevAccZ;
	float gyrX, gyrY, gyrZ;
	float prevGyrX, prevGyrY, prevGyrZ;
	float magX, magY, magZ;
	float prevMagX, prevMagY, prevMagZ;
	float lig;
	float prevLig;

	float[] aValues;
	float[] mValues;

	CSVWriter writer;

	TextView t;
	TextView timestampLabel;
	TextView accLabelX, accLabelY, accLabelZ;
	TextView gyroLabelX, gyroLabelY, gyroLabelZ;
	TextView magLabelX, magLabelY, magLabelZ;
	TextView lightLabel;
	TextView stepCountLabel;
	TextView azimuthText;
	TextView orientationText;
	TextView wifiText;

	SensorManager sManager;
	WifiManager wifiManager;
	Sensor accel;
	Sensor gyro;
	Sensor mag;
	Sensor light;

	boolean checkAccel;
	boolean checkGyro;
	boolean checkMag;
	boolean checkLight;

	long time;
	float[] measures; // Accel = [0,1,2],
						// Gyro = [3,4,5],
						// Mag = [6,7,8],
						// Light = [9];
						// Compass = [10];
						// Sound = [11];
	String measuresString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_basic_main);
		t = (TextView) findViewById(R.id.textView0);
		timestampLabel = (TextView) findViewById(R.id.textView11);
		accLabelX = (TextView) findViewById(R.id.textView12);
		accLabelY = (TextView) findViewById(R.id.textView13);
		accLabelZ = (TextView) findViewById(R.id.textView14);
		gyroLabelX = (TextView) findViewById(R.id.textView15);
		gyroLabelY = (TextView) findViewById(R.id.textView16);
		gyroLabelZ = (TextView) findViewById(R.id.textView17);
		magLabelX = (TextView) findViewById(R.id.textView18);
		magLabelY = (TextView) findViewById(R.id.textView19);
		magLabelZ = (TextView) findViewById(R.id.textView20);
		lightLabel = (TextView) findViewById(R.id.textView21);
		stepCountLabel = (TextView) findViewById(R.id.textView23);
		azimuthText = (TextView) findViewById(R.id.textView24);
		orientationText = (TextView) findViewById(R.id.textView27);
		wifiText = (TextView) findViewById(R.id.textView28);
		
		measures = new float[12];
		aValues = new float[3];
		mValues = new float[3];

		try {
			writer = new CSVWriter(new FileWriter(getExternalFilesDir(null)
					.toString() + "/hw1.csv"), ',', '"', "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		writer.writeNext(new String[] { "Timestamp", "Accel_x", "Accel_y",
				"Accel_z", "Gyro_x", "Gyro_y", "Gyro_z", "Mag_x", "Mag_y",
				"Mag_z", "Light", "Compass", "Sound", "Wifi"});

		accCounter = gyrCounter = magCounter = ligCounter = 1;

		sManager = (SensorManager) this.getApplicationContext()
				.getSystemService(SENSOR_SERVICE);
		wifiManager = (WifiManager) this.getApplicationContext()
				.getSystemService(Context.WIFI_SERVICE);
		accel = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyro = sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mag = sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		light = sManager.getDefaultSensor(Sensor.TYPE_LIGHT);

		sManager.registerListener(this, accel,
				SensorManager.SENSOR_DELAY_NORMAL);
		sManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
		sManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL);
		sManager.registerListener(this, light,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void onResume() {
		super.onResume();
		sManager.registerListener(this, accel,
				SensorManager.SENSOR_DELAY_NORMAL);
		sManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
		sManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL);
		sManager.registerListener(this, light,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void onPause() {
		super.onPause();
		sManager.unregisterListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.basic_main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	public float lowPassFilter(float prevValue, float newValue,
			double smoothing, long timeSinceLastUpdate) {
		return (float) (prevValue + ((newValue - prevValue) / (smoothing / (double) timeSinceLastUpdate)));
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (checkAccel && checkGyro && checkMag && checkLight) {
			checkAccel = checkGyro = checkMag = checkLight = false;
			time = System.currentTimeMillis();
			timestampLabel.setText(Long.toString(time));
			measuresString = Arrays.toString(measures);
			String[] entries = (Long.toString(time) + ", " + measuresString
					.substring(1, measuresString.length() - 1)).split(", ");
			String[] entriesWithWifi = new String[entries.length+1];
			for(int i = 0; i < entries.length; ++i) entriesWithWifi[i] = entries[i];
			entriesWithWifi[entriesWithWifi.length-1] = wifiText.getText().toString();
			writer.writeNext(entriesWithWifi);
		}
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			aValues = event.values.clone();
			prevAccTime = accTime;
			accTime = System.currentTimeMillis();
			accTimeDiff += ((accTime - prevAccTime) - accTimeDiff) / accCounter;
			if (prevAccTime == 0) {
				prevAccTime = System.currentTimeMillis();
				accTime = System.currentTimeMillis();
				return;
			}
			prevAccX = accX;
			prevAccY = accY;
			prevAccZ = accZ;
			accX = lowPassFilter(prevAccX, event.values[0], SMOOTHING_ACC,
					(accTime - prevAccTime) / accTimeDiff);
			accY = lowPassFilter(prevAccY, event.values[1], SMOOTHING_ACC,
					(accTime - prevAccTime) / accTimeDiff);
			accZ = lowPassFilter(prevAccZ, event.values[2], SMOOTHING_ACC,
					(accTime - prevAccTime) / accTimeDiff);
			if (Float.toString(accX).equals("Infinity")) {
				accX += 1;
			}
			accLabelX.setText(Float.toString(accX) + " m/s^2");
			accLabelY.setText(Float.toString(accY) + " m/s^2");
			accLabelZ.setText(Float.toString(accZ) + " m/s^2");
			if (!checkAccel) {
				measures[0] = accX;
				measures[1] = accY;
				measures[2] = accZ;
				checkAccel = true;
			}

			// Threshold is approx value.
			// Barely no change in X except for turning.
			if (Math.sqrt(accY * accY + accZ * accZ) >= THRESHOLD) {
				stepCount++;

				// Print step count.
				stepCountLabel.setText(Integer.toString(stepCount));
			}
			++accCounter;
			break;
		case Sensor.TYPE_GYROSCOPE:
			prevGyrTime = gyrTime;
			gyrTime = System.currentTimeMillis();
			gyrTimeDiff += ((gyrTime - prevGyrTime) - gyrTimeDiff) / gyrCounter;
			if (prevGyrTime == 0) {
				prevGyrTime = System.currentTimeMillis();
				gyrTime = System.currentTimeMillis();
				return;
			}
			prevGyrX = gyrX;
			prevGyrY = gyrY;
			prevGyrZ = gyrZ;
			gyrX = lowPassFilter(prevGyrX, event.values[0], SMOOTHING_GYR,
					(gyrTime - prevGyrTime) / gyrTimeDiff);
			gyrY = lowPassFilter(prevGyrY, event.values[1], SMOOTHING_GYR,
					(gyrTime - prevGyrTime) / gyrTimeDiff);
			gyrZ = lowPassFilter(prevGyrZ, event.values[2], SMOOTHING_GYR,
					(gyrTime - prevGyrTime) / gyrTimeDiff);
			gyroLabelX.setText(Float.toString(gyrX) + " m/s^2");
			gyroLabelY.setText(Float.toString(gyrY) + " m/s^2");
			gyroLabelZ.setText(Float.toString(gyrZ) + " m/s^2");
			if (!checkGyro) {
				measures[3] = gyrX;
				measures[4] = gyrY;
				measures[5] = gyrZ;
				checkGyro = true;
			}
			++gyrCounter;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mValues = event.values.clone();
			prevMagTime = gyrTime;
			magTime = System.currentTimeMillis();
			prevMagX = magX;
			prevMagY = magY;
			prevMagZ = magZ;
			magTimeDiff += ((magTime - prevMagTime) - magTimeDiff) / magCounter;
			if (prevMagTime == 0) {
				prevMagTime = System.currentTimeMillis();
				magTime = System.currentTimeMillis();
				return;
			}
			magX = lowPassFilter(prevMagX, event.values[0], SMOOTHING_MAG,
					(magTime - prevMagTime) / magTimeDiff);
			magY = lowPassFilter(prevMagY, event.values[1], SMOOTHING_MAG,
					(magTime - prevMagTime) / magTimeDiff);
			magZ = lowPassFilter(prevMagZ, event.values[2], SMOOTHING_MAG,
					(magTime - prevMagTime) / magTimeDiff);
			magLabelX.setText(Float.toString(magX) + " uT");
			magLabelY.setText(Float.toString(magY) + " uT");
			magLabelZ.setText(Float.toString(magZ) + " uT");
			if (!checkMag) {
				measures[6] = magX;
				measures[7] = magY;
				measures[8] = magZ;
				checkMag = true;
			}
			++magCounter;
			break;
		case Sensor.TYPE_LIGHT:
			prevLigTime = ligTime;
			ligTime = System.currentTimeMillis();
			prevLig = lig;
			ligTimeDiff += ((ligTime - prevLigTime) - ligTimeDiff) / ligCounter;
			if (prevLigTime == 0) {
				prevLigTime = System.currentTimeMillis();
				ligTime = System.currentTimeMillis();
				return;
			}
			lig = lowPassFilter(prevLig, event.values[0], SMOOTHING_LIG,
					(ligTime - prevLigTime) / ligTimeDiff);
			;
			lightLabel.setText(Float.toString(lig) + " L");
			if (!checkLight) {
				measures[9] = lig;
				checkLight = true;
			}
			++ligCounter;
			break;
		}

		float[] R = new float[16];
		float[] orientationValues = new float[3];

		SensorManager.getRotationMatrix(R, null, aValues, mValues);
		SensorManager.getOrientation(R, orientationValues);

		orientationValues[0] = (float) Math.toDegrees(orientationValues[0]);
		orientationValues[1] = (float) Math.toDegrees(orientationValues[1]);
		orientationValues[2] = (float) Math.toDegrees(orientationValues[2]);

		// Orientation.
		float orient = orientationValues[0];
		if (orient < 0)
			orient += 360.0;
		if (315 < orient || orient <= 45)
			orientationText.setText("N");
		else if (45 < orient && orient <= 135)
			orientationText.setText("E");
		else if (135 < orient && orient <= 225)
			orientationText.setText("S");
		else
			/* 225 < orient && orient <= 315) */orientationText.setText("W");
		measures[10] = orient;
		azimuthText.setText(Float.toString(orient));
		
		
		//************************************//
		//************************************//
		
		// Sound must be stored in measures[11]!
		
		//************************************//
		//************************************//
		
		// Wifi.
		int counter = 0;
		StringBuffer text = new StringBuffer();
		List<ScanResult> wifis = wifiManager.getScanResults();
		Collections.sort(wifis, new Comparator<ScanResult>(){
			@Override
			public int compare(ScanResult arg0, ScanResult arg1) {
				return arg0.level - arg1.level;
			}
		});
		if(!wifis.isEmpty()){
			text.append(wifis.get(0).SSID);
			for(int i = 1; i < wifis.size() && counter < 10 ; ++i){
				if(!wifis.get(i).SSID.isEmpty()) {
					text.append(",\n");
					text.append(wifis.get(i).SSID);
					++counter;
				}
			}
		}
		wifiText.setText(text.toString());
	}

	public void stopExercise2(View v) {
		Intent intent = new Intent(BasicMain.this, FragmentMainMenuDummy.class);
		startActivity(intent);
	}
}
