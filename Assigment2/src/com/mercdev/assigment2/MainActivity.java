package com.mercdev.assigment2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class MainActivity extends Activity implements LoaderCallbacks<Void> {

	private static final String IMAGE_URL = "http://upload.wikimedia.org/wikipedia/commons/0/06/%D0%A0%D0%B0%D1%81%D1%88%D0%B8%D1%80%D0%B5%D0%BD%D0%B8%D0%B5-%D0%92%D1%81%D0%B5%D0%BB%D0%B5%D0%BD%D0%BD%D0%BE%D0%B9.png";
	private final static String TAG = "ImageManager";
	private ProgressBar progressBar;
	private final int LOADER_ID = 1;
	private static HttpURLConnection conn;
	private static Bitmap bitmap = null;
	private static BufferedOutputStream fileOutpStream;
	private Button button;
	private static final String FILE_NAME = "downloadedImage.png";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_main_activity);

		button = (Button) findViewById(R.id.button);
		OnClickListener onClickButton = new OnClickListener() {
			@Override
			public void onClick(View v) {
				LoaderManager lm = getLoaderManager();
				lm.initLoader(LOADER_ID, null, MainActivity.this);
				ImageDownloader.mActivity = new WeakReference<MainActivity>(
						MainActivity.this);
			}
		};
		button.setOnClickListener(onClickButton);

		try {
			fileOutpStream = new BufferedOutputStream(openFileOutput(FILE_NAME,
					MODE_WORLD_READABLE));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		progressBar.setProgress(1);

	}

	/**
	 * Class extending Loader. This class will download picture and check for
	 * progress in background
	 * 
	 */
	static class ImageDownloader extends AsyncTaskLoader<Void> {
		/**
		 * Needs for updating progressBar
		 */
		static WeakReference<MainActivity> mActivity;

		ImageDownloader(MainActivity activity) {
			super(activity);
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public Void loadInBackground() {
			try {
				Log.v(TAG, "Starting loading image by URL: " + IMAGE_URL);
				conn = (HttpURLConnection) new URL(IMAGE_URL).openConnection();
				conn.setDoInput(true);
				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.connect();
				// Size of picture
				int size = conn.getContentLength();
				byte[] buffer = new byte[1024];
				double downloadedSize = 0;
				int bufferLength = 0;
				boolean connectionClosed = false;
				while (!connectionClosed) {
					try {
						bufferLength = conn.getInputStream().read(buffer);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (bufferLength > 0) {
						// Save data to file
						fileOutpStream.write(buffer, 0, bufferLength);
						fileOutpStream.flush();
						downloadedSize += bufferLength;
						int progress = (int) Math
								.floor((downloadedSize / size) * 100);
						mActivity.get().progressBar.setProgress(progress);
					} else {
						connectionClosed = true;
					}
				}
				
				fileOutpStream.close();
				conn.disconnect();
				fileOutpStream = null;
				conn = null;
			} catch (MalformedURLException ex) {
				Log.e(TAG, "Url parsing was failed: " + IMAGE_URL);
			} catch (IOException ex) {
				Log.d(TAG, IMAGE_URL + " does not exists" + ex.getMessage());
			} catch (OutOfMemoryError e) {
				Log.w(TAG, "Out of memory!!!");
				return null;
			} finally {
				if (fileOutpStream != null)
					try {
						fileOutpStream.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				if (conn != null)
					conn.disconnect();
			}
			return null;
		}
	}

	/*
	 * Creates Loader class (ImageLoader)
	 */
	@Override
	public Loader<Void> onCreateLoader(int id, Bundle args) {
		AsyncTaskLoader<Void> asyncTaskLoader = new ImageDownloader(this);
		asyncTaskLoader.forceLoad();
		return asyncTaskLoader;
	}

	/*
	 * Caused when Loader was finished its work
	 */
	@Override
	public void onLoadFinished(Loader<Void> loader, Void arg2) {
		switch (loader.getId()) {
		case LOADER_ID:
			File imageFile = new File (getFilesDir() + "/" + FILE_NAME);
			Intent i2 = new Intent();
			i2.setAction(android.content.Intent.ACTION_VIEW);
			Uri uri = Uri.fromFile(imageFile);
			i2.setDataAndType(uri, "image/*");
			startActivity(i2);
            break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Void> loader) {
		Log.v(TAG, "Loader reset");
	}
}
