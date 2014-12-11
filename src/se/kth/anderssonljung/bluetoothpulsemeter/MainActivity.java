package se.kth.anderssonljung.bluetoothpulsemeter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
	private static final byte[] FORMAT_2 =  { 0x02, 0x70, 0x04, 0x02, 0x08, 0x00, (byte) 0x7E, 0x03 };
	private static final String address= "";
	private static final UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private BluetoothAdapter bluetoothAdapter;
	public TextView textview;
	private Button button;
	private BluetoothDevice device;
	private BluetoothSocket socket;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("onCreate", "Application started");
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
        Log.d("onCreate", "Adapter==null");
        }
        button=(Button) findViewById(R.id.button1);
        textview=(TextView) findViewById(R.id.textView1);
        
        button.setText("Start");
        textview.setText("-");
        
        Set<BluetoothDevice> devices= bluetoothAdapter.getBondedDevices();
        
        for(BluetoothDevice device:devices){
        	this.device=device;
        	break;
        }
        
    }

	public void onButtonClick(View view){
		Log.d("Button", "Button Clicked");
		if(button.getText().toString().equalsIgnoreCase("START")){
			Log.d("Button", "Button said start -> Now starting");
			//Start measuring
		try {
			BluetoothSocket	socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
			ReadBlueTooth task = new ReadBlueTooth(socket,textview);
			task.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		else{
			// Stop measuring
			Log.d("Button", "Button did not say start -> Now stopping");
		}
	}
	
	/**
	 * Given method that may come in handy?
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
	
	private static class ReadBlueTooth extends AsyncTask<Void, Void, Void>{
		BluetoothSocket socket;
		TextView textview;
		InputStream is;
		OutputStream up;
		int pulse=0;
		int oxygenLevel=0;
		public ReadBlueTooth(BluetoothSocket socket, TextView view){
			this.socket=socket;
			this.textview=view;
		}
		@Override
		protected Void doInBackground(Void... params) {
			Log.d("ReadBluetooth", "Background thread started");
			try {
				socket.connect();
				Log.d("ReadBluetooth", "Connect success");
				is = socket.getInputStream();
				up=socket.getOutputStream();
				
				up.write(FORMAT_2);
				up.flush();
				int response = is.read();
				Log.d("ReadBluetooth", "Response: "+response);
				byte[] buffer = new byte[4];
				
				while(true){
					is.read(buffer);
					pulse=unsignedByteToInt(buffer[1]);
					oxygenLevel=pulse=unsignedByteToInt(buffer[2]);
					publishProgress();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d("ReadBluetooth", "Connect failed");
			}
			finally{
				try {
					socket.close();
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
	            textview.setText(""+pulse);
	        }
		
		@Override
		protected void onPostExecute(Void result) {
		}
		
	}
}
