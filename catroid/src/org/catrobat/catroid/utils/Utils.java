/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Copyright for original "String buildPath" held by:
 * 	Copyright (C) 2008 Rob Manning
 * 	manningr@users.sourceforge.net
 * Source: http://www.java2s.com/Code/Java/File-Input-Output/Autilityclassformanipulatingpaths.htm
 */
package org.catrobat.catroid.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.common.StandardProjectHandler;
import org.catrobat.catroid.common.Values;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.SetLookBrick;
import org.catrobat.catroid.content.bricks.WaitBrick;
import org.catrobat.catroid.io.StorageHandler;
import org.catrobat.catroid.ui.dialogs.ErrorDialogFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Utils {

	private static final String TAG = Utils.class.getSimpleName();
	private static long uniqueLong = 0;
	private static Semaphore uniqueNameLock = new Semaphore(1);
	public static final int PICTURE_INTENT = 1;
	public static final int FILE_INTENT = 2;
	private static boolean isUnderTest;

	public static boolean externalStorageAvailable() {
		String externalStorageState = Environment.getExternalStorageState();
		return externalStorageState.equals(Environment.MEDIA_MOUNTED)
				&& !externalStorageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
	}

	public static boolean checkForExternalStorageAvailableAndDisplayErrorIfNot(final Context context) {
		if (!externalStorageAvailable()) {
			Builder builder = new AlertDialog.Builder(context);

			builder.setTitle(context.getString(R.string.error));
			builder.setMessage(context.getString(R.string.error_no_writiable_external_storage_available));
			builder.setNeutralButton(context.getString(R.string.close), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((Activity) context).moveTaskToBack(true);
				}
			});
			builder.show();
			return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public static void updateScreenWidthAndHeight(Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		Values.SCREEN_WIDTH = display.getWidth();
		Values.SCREEN_HEIGHT = display.getHeight();
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	/**
	 * Constructs a path out of the pathElements.
	 * 
	 * @param pathElements
	 *            the strings to connect. They can have "/" in them which will be de-duped in the result, if necessary.
	 * @return
	 *         the path that was constructed.
	 */
	static public String buildPath(String... pathElements) {
		StringBuilder result = new StringBuilder("/");

		for (String pathElement : pathElements) {
			result.append(pathElement).append("/");
		}

		String returnValue = result.toString().replaceAll("/+", "/");

		if (returnValue.endsWith("/")) {
			returnValue = returnValue.substring(0, returnValue.length() - 1);
		}

		return returnValue;
	}

	static public String buildProjectPath(String projectName) {
		return buildPath(Constants.DEFAULT_ROOT, deleteSpecialCharactersInString(projectName));
	}

	/**
	 * @param projectFileName
	 * @return the project name without the default file extension, else returns unchanged string
	 */
	//	public static String getProjectName(String projectFileName) {
	//		if (projectFileName.endsWith(Constants.PROJECT_EXTENTION)) {
	//			return projectFileName.substring(0, projectFileName.length() - Constants.PROJECT_EXTENTION.length());
	//		}
	//		return projectFileName;
	//	}

	public static void displayErrorMessageFragment(FragmentManager fragmentManager, String errorMessage) {
		DialogFragment errorDialog = ErrorDialogFragment.newInstance(errorMessage);
		errorDialog.show(fragmentManager, ErrorDialogFragment.DIALOG_FRAGMENT_TAG);
	}

	public static String md5Checksum(File file) {
		if (!file.isFile()) {
			return null;
		}

		MessageDigest messageDigest = getMD5MessageDigest();

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte[] buffer = new byte[Constants.BUFFER_8K];

			int length = 0;

			while ((length = fis.read(buffer)) != -1) {
				messageDigest.update(buffer, 0, length);
			}
		} catch (IOException e) {
			Log.w(TAG, "IOException thrown in md5Checksum()");
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				Log.w(TAG, "IOException thrown in finally block of md5Checksum()");
			}
		}

		return toHex(messageDigest.digest());
	}

	public static String md5Checksum(String string) {
		MessageDigest messageDigest = getMD5MessageDigest();

		messageDigest.update(string.getBytes());

		return toHex(messageDigest.digest());
	}

	public static String getUniqueName() {
		uniqueNameLock.acquireUninterruptibly();
		String uniqueName = String.valueOf(uniqueLong++);
		uniqueNameLock.release();
		return uniqueName;
	}

	private static String toHex(byte[] messageDigest) {
		StringBuilder md5StringBuilder = new StringBuilder(2 * messageDigest.length);

		for (byte b : messageDigest) {
			md5StringBuilder.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4));
			md5StringBuilder.append("0123456789ABCDEF".charAt((b & 0x0F)));
		}

		return md5StringBuilder.toString();
	}

	private static MessageDigest getMD5MessageDigest() {
		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Log.w(TAG, "NoSuchAlgorithmException thrown in getMD5MessageDigest()");
		}

		return messageDigest;
	}

	public static int getVersionCode(Context context) {
		int versionCode = -1;
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
					PackageManager.GET_META_DATA);
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException nameNotFoundException) {
			Log.e(TAG, "Name not found", nameNotFoundException);
		}
		return versionCode;
	}

	public static String getVersionName(Context context) {
		String versionName = "unknown";
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
					PackageManager.GET_META_DATA);
			versionName = packageInfo.versionName;
		} catch (NameNotFoundException nameNotFoundException) {
			Log.e(TAG, "Name not found", nameNotFoundException);
		}
		return versionName;
	}

	public static int getPhysicalPixels(int densityIndependentPixels, Context context) {
		final float scale = context.getResources().getDisplayMetrics().density;
		int physicalPixels = (int) (densityIndependentPixels * scale + 0.5f);
		return physicalPixels;
	}

	public static void saveToPreferences(Context context, String key, String message) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = sharedPreferences.edit();
		edit.putString(key, message);
		edit.commit();
	}

	public static void loadProjectIfNeeded(Context context, ErrorListenerInterface errorListener) {
		if (ProjectManager.getInstance().getCurrentProject() == null) {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			String projectName = sharedPreferences.getString(Constants.PREF_PROJECTNAME_KEY, null);

			if (projectName != null) {
				ProjectManager.getInstance().loadProject(projectName, context, errorListener, false);
			} else if (ProjectManager.INSTANCE.canLoadProject(context.getString(R.string.default_project_name))) {
				ProjectManager.getInstance().loadProject(context.getString(R.string.default_project_name), context,
						errorListener, false);
			} else {
				ProjectManager.getInstance().initializeDefaultProject(context, errorListener);
			}
		}
	}

	public static String deleteSpecialCharactersInString(String stringToAdapt) {
		return stringToAdapt.replaceAll("[\"*/:<>?\\\\|]", "");
	}

	public static String getUniqueLookName(String name) {
		return searchForNonExistingLookName(name, 0);
	}

	private static String searchForNonExistingLookName(String name, int nextNumber) {
		String newName;
		ArrayList<LookData> lookDataList = ProjectManager.getInstance().getCurrentSprite().getLookDataList();
		if (nextNumber == 0) {
			newName = name;
		} else {
			newName = name + nextNumber;
		}
		for (LookData lookData : lookDataList) {
			if (lookData.getLookName().equals(newName)) {
				return searchForNonExistingLookName(name, ++nextNumber);
			}
		}
		return newName;
	}

	public static String getUniqueSoundName(String title) {
		return searchForNonExistingSoundTitle(title, 0);
	}

	public static Project findValidProject() {
		Project loadableProject = null;

		List<String> projectNameList = UtilFile.getProjectNames(new File(Constants.DEFAULT_ROOT));
		for (String projectName : projectNameList) {
			if (ProjectManager.getInstance().canLoadProject(projectName)) {
				loadableProject = StorageHandler.getInstance().loadProject(projectName);
				break;
			}
		}
		return loadableProject;
	}

	private static String searchForNonExistingSoundTitle(String title, int nextNumber) {
		// search for sounds with the same title
		String newTitle;
		ArrayList<SoundInfo> soundInfoList = ProjectManager.getInstance().getCurrentSprite().getSoundList();
		if (nextNumber == 0) {
			newTitle = title;
		} else {
			newTitle = title + nextNumber;
		}
		for (SoundInfo soundInfo : soundInfoList) {
			if (soundInfo.getTitle().equals(newTitle)) {
				return searchForNonExistingSoundTitle(title, ++nextNumber);
			}
		}
		return newTitle;
	}

	public static boolean isApplicationDebuggable(Context context) {
		if (isUnderTest) {
			return false;
		} else {
			return (context.getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		}
	}

	public static Pixmap getPixmapFromFile(File imageFile) {
		Pixmap pixmap = null;
		try {
			GdxNativesLoader.load();
			pixmap = new Pixmap(new FileHandle(imageFile));
		} catch (GdxRuntimeException e) {
			return null;
		} catch (Exception e1) {
			return null;
		}
		return pixmap;
	}

	public static void setBottomBarActivated(Activity activity, boolean isActive) {
		LinearLayout bottomBarLayout = (LinearLayout) activity.findViewById(R.id.bottom_bar);

		if (bottomBarLayout != null) {
			bottomBarLayout.findViewById(R.id.button_add).setClickable(isActive);
			bottomBarLayout.findViewById(R.id.button_play).setClickable(isActive);
		}
	}

	public static boolean compareToStandardProject(Project projectToCheck, Context context) {
		try {
			Project standardProject = StandardProjectHandler.createAndSaveStandardProject(
					context.getString(R.string.default_project_name), context);

			List<Sprite> standardProjectSprites = standardProject.getSpriteList();
			List<Sprite> projectToCheckSprites = projectToCheck.getSpriteList();

			if (standardProjectSprites.size() != projectToCheckSprites.size()) {
				Log.v("FALSE", "Returning false because the number of sprites is not ok");
				return false;
			}

			Sprite standardSprite;
			Sprite checkSprite;
			Script standardScript;
			Script checkScript;
			ArrayList<Brick> standardBricks;
			ArrayList<Brick> checkBricks;
			Brick standardBrick;
			Brick checkBrick;

			for (int spriteIndex = 0; spriteIndex < standardProjectSprites.size(); spriteIndex++) {
				standardSprite = standardProjectSprites.get(spriteIndex);
				checkSprite = projectToCheckSprites.get(spriteIndex);

				if (standardSprite.getNumberOfScripts() != checkSprite.getNumberOfScripts()) {
					return false;
				}

				for (int scriptIndex = 0; scriptIndex < standardSprite.getNumberOfScripts(); scriptIndex++) {
					standardScript = standardSprite.getScript(scriptIndex);
					checkScript = checkSprite.getScript(scriptIndex);

					if (standardScript.getScriptBrick().getClass() != checkScript.getScriptBrick().getClass()) {
						return false;

					}

					standardBricks = standardScript.getBrickList();
					checkBricks = checkScript.getBrickList();

					if (standardBricks.size() != checkBricks.size()) {
						return false;
					}

					for (int brickIndex = 0; brickIndex < standardBricks.size(); brickIndex++) {
						standardBrick = standardBricks.get(brickIndex);
						checkBrick = checkBricks.get(brickIndex);

						if (standardBrick.getClass() != checkBrick.getClass()) {
							return false;
						}

						if (standardBrick instanceof SetLookBrick) {
							SetLookBrick standardLookBrick = (SetLookBrick) standardBrick;
							SetLookBrick checkLookBrick = (SetLookBrick) checkBrick;

							if (!standardLookBrick.getImagePath().equals(checkLookBrick.getImagePath())) {
								return false;
							}

						} else if (standardBrick instanceof WaitBrick) {
							WaitBrick standardWaitBrick = (WaitBrick) standardBrick;
							WaitBrick checkWaitBrick = (WaitBrick) checkBrick;

							if (standardWaitBrick.getTimeToWait() != checkWaitBrick.getTimeToWait()) {
								return false;
							}

						}

					}
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

}
