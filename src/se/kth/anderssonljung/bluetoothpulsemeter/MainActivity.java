package se.kth.anderssonljung.bluetoothpulsemeter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final UUID uuid = UUID
			.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private BluetoothAdapter bluetoothAdapter;
	public TextView textview;
	private Button button;
	private BluetoothDevice device;
	UploadTask uploadTask;
	BluetoothThread bThread;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			String pulse = b.getString("pulse");
			textview.setText(pulse);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d("onCreate", "Application started");

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Log.d("onCreate", "Adapter==null");
		}
		button = (Button) findViewById(R.id.button1);
		textview = (TextView) findViewById(R.id.textView1);

		button.setText("Start");
		textview.setText("-");

		Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

		for (BluetoothDevice device : devices) {
			this.device = device;
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		uploadTask.cancel(true);
	}

	public void onButtonClick(View view) {
		Log.d("Button", "Button Clicked");
		if (button.getText().toString().equalsIgnoreCase("START")) {
			button.setText("Stop");
			Log.d("Button", "Button said start -> Now starting");
			// Start measuring
			try {
				BluetoothSocket socket = device
						.createInsecureRfcommSocketToServiceRecord(uuid);
				bThread = new BluetoothThread(socket, textview,
						getExternalFilesDir(null), handler);
				new Thread(bThread).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			File mostRecentFile = bThread.getFileU();
			bThread.setRunning(false);
			button.setText("Start");
			UploadTask task = new UploadTask(mostRecentFile);
			task.execute();
			Log.d("Button", "Button did not say start -> Now stopping");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	private class UploadTask extends AsyncTask<Void, Void, Void> {
		File file;

		public UploadTask(File file) {
			this.file = file;
		}

		@Override
		protected Void doInBackground(Void... params) {
			FileInputStream fis = null;
			OutputStream os = null;
			Socket socket = null;
			Log.d("UploadTask", "Reached doInBackground");
			try {
				fis = new FileInputStream(file);
				Log.d("UploadTask", "FileInputstream opened");
				socket = new Socket("193.10.37.92", 1337);
				Log.d("UploadTask", "Socket defined");
				byte[] buffer = new byte[1024];

				int bytesRead;
				os = socket.getOutputStream();
				Log.d("UploadTask", "outputstream opened");
				int loopcounter = 0;
				while ((bytesRead = fis.read(buffer)) != -1) {
					loopcounter++;
					if (isCancelled()) {
						break;
					}
					os.write(buffer);
				}
				Log.d("UploadTask", "Finished reading after # of loops: "
						+ loopcounter);

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (os != null) {
						try {
							os.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
			}

			Log.d("UploadTask", "Finished do in background");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			showToast("Upload completed");
			Log.d("UploadTask", "Reached onPostExecute");
		}

	}

	private void showToast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT)
				.show();

	}

}
