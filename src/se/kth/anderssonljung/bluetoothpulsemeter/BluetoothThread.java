package se.kth.anderssonljung.bluetoothpulsemeter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class BluetoothThread implements Runnable {
	private static final byte[] FORMAT_8 = { 0x02, 0x70, 0x04, 0x02, 0x08,
		0x00, (byte) 0x7E, 0x03 };
	BluetoothSocket socket;
	File file;
	TextView textview;
	InputStream blueInputStream;
	OutputStream blueOutputStream;
	PrintWriter writer;
	int pulse = 0;
	private boolean running;
	private boolean cancelled;
	private Handler handler;

	public BluetoothThread(BluetoothSocket socket, TextView view,
			File filesdir, Handler handler) {
		running = false;
		this.handler=handler;
		this.socket = socket;
		this.textview = view;
		Random random = new Random();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		
		file = new File(filesdir, "pulse-"+dateFormat.format(date) + "-"
				+ random.nextFloat());
	}
	
	public void setRunning(boolean running){
		this.running=running;
	}
	
	@Override
	public void run() {
		running = true;
		Log.d("ReadBluetooth", "Background thread started");
		try {
			Long startTime = (long) 0;
			socket.connect();
			Log.d("ReadBluetooth", "Connect success");
			blueInputStream = socket.getInputStream();
			blueOutputStream = socket.getOutputStream();

			blueOutputStream.write(FORMAT_8);
			blueOutputStream.flush();
			int response = blueInputStream.read();
			Log.d("ReadBluetooth", "Response: " + response);
			if (response != 6) {
				return;// Did not receive ACK
			}
			writer = new PrintWriter(file);
			byte[] buffer = new byte[4];
			Long relativeTimeStamp;
			while (running&&(!isCancelled())) {
				blueInputStream.read(buffer);
				if (startTime == 0) {
					startTime = System.currentTimeMillis();
					relativeTimeStamp = (long) 0;
				} else {
					relativeTimeStamp = System.currentTimeMillis()
							- startTime;
				}
				pulse = unsignedByteToInt(buffer[1]);
				byte b1 = buffer[0];
				if (isBitSet(b1, 0)) {
					pulse += 128;
				}
				writer.println(pulse + " | " + relativeTimeStamp + "\r\n");
				//publishProgress();
				Message msg = new Message();
			      Bundle b = new Bundle();
			      b.putString("pulse", pulse+"");
			      msg.setData(b);
				handler.sendMessage(msg);
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
		
	}
	
	public void setCancelled(boolean cancelled){
		this.cancelled=cancelled;
	}
	
	public boolean isCancelled(){
		return cancelled;
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

	// Stolen from
	// stackoverflow.com/questions/2431732/checking-if-a-bit-is-set-or-not
	private static boolean isBitSet(byte b, int pos) {
		return (b & (1 << pos)) != 0;
	}
	
	public File getFile(){
		return file;
	}

}
