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
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final byte[] FORMAT_8 = { 0x02, 0x70, 0x04, 0x02, 0x08,
			0x00, (byte) 0x7E, 0x03 };
	private static final UUID uuid = UUID
			.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private BluetoothAdapter bluetoothAdapter;
	public TextView textview;
	private Button button;
	private BluetoothDevice device;
	UploadTask uploadTask;
	ReadBlueTooth bluetoothtask;

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

	public void onButtonClick(View view) {
		Log.d("Button", "Button Clicked");
		if (button.getText().toString().equalsIgnoreCase("START")) {
			button.setText("Stop");
			Log.d("Button", "Button said start -> Now starting");
			// Start measuring
			try {
				BluetoothSocket socket = device
						.createInsecureRfcommSocketToServiceRecord(uuid);
				bluetoothtask = new ReadBlueTooth(socket, textview,
						getFilesDir());
				bluetoothtask.execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			bluetoothtask.setRunning(false);
			button.setText("Start");
			Log.d("Button", "Button did not say start -> Now stopping");
		}
	}

	/**
	 * Given method that may come in handy?
	 * 
	 * @param b
	 * @return
	 */
	private static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
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

	private class ReadBlueTooth extends AsyncTask<Void, Void, Void> {
		BluetoothSocket socket;
		File file;
		TextView textview;
		InputStream blueInputStream;
		OutputStream blueOutputStream;
		PrintWriter writer;
		int pulse = 0;
		private boolean running;

		public ReadBlueTooth(BluetoothSocket socket, TextView view,
				File filesdir) {
			running=false;
			this.socket = socket;
			this.textview = view;
			// pulse-2014-12-11-11-59-00000
			Date date = new Date(System.currentTimeMillis());
			Random random = new Random();
			file = new File(filesdir, "pulse-" + date.getYear()
					+ date.getMonth() + date.getDate() + "-"
					+ random.nextFloat());
		}
		
		public void setRunning(boolean running){
			this.running=running;
		}

		@Override
		protected Void doInBackground(Void... params) {
			running=true;
			Log.d("ReadBluetooth", "Background thread started");
			try {
				socket.connect();
				Log.d("ReadBluetooth", "Connect success");
				blueInputStream = socket.getInputStream();
				blueOutputStream = socket.getOutputStream();

				blueOutputStream.write(FORMAT_8);
				blueOutputStream.flush();
				int response = blueInputStream.read();
				Log.d("ReadBluetooth", "Response: " + response);
				if (response != 6) {
					return null; // Did not receive ACK
				}
				writer = new PrintWriter(file);
				byte[] buffer = new byte[4];

				while (running) {
					blueInputStream.read(buffer);
					pulse = unsignedByteToInt(buffer[1]);
					writer.println(pulse);
					publishProgress();
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d("ReadBluetooth", "Connect failed");
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
					if (socket != null) {
						socket.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... params) {
			super.onProgressUpdate();
			textview.setText("" + pulse);
		}

		@Override
		protected void onPostExecute(Void result) {
			uploadTask = new UploadTask(file);
			uploadTask.execute();
		}

	}

	private class UploadTask extends AsyncTask<Void, Void, Void> {
		File file;

		public UploadTask(File file) {
			this.file = file;
		}

		@Override
		protected Void doInBackground(Void... params) {
			FileInputStream fis = null;
			OutputStream os=null;
			Log.d("UploadTask", "Reached doInBackground");
			try {
				fis = new FileInputStream(file);
				Socket socket = new Socket("193.10.37.94", 1337);
				byte[] buffer = new byte[1024];

				int bytesRead;
				os = socket.getOutputStream();

				while ((bytesRead = fis.read(buffer)) != -1) {
					os.write(buffer);
				}

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
					if(os!=null){
						try {
							os.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				
				}
			}

			Log.d("UploadTask", "Reached doInBackground");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.d("UploadTask", "Reached onPostExecute");
		}

	}
}
