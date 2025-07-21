package com.oddlabs.tt;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.oddlabs.tt.render.Display;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.updater.UpdateInfo;

import org.lwjgl.system.Platform;

public final strictfp class Main {
	public final static void fail(Throwable t) {
		try {
			t.printStackTrace();
			if (Display.isCreated())
				Display.destroy();
			while (t.getCause() != null)
				t = t.getCause();
			ResourceBundle bundle = ResourceBundle.getBundle(Main.class.getName());
			String error = Utils.getBundleString(bundle, "error");
			String error_msg = Utils.getBundleString(bundle, "error_message", new Object[]{t.toString(), Globals.SUPPORT_ADDRESS});
			System.out.println(error + " : " + error_msg);
		} finally {
			shutdown();
		}
	}

	public final static void shutdown() {
		System.exit(0);
	}

	public final static void main(String[] args) {
		try {
			System.out.println("Starting game....");
			System.out.flush();
			Main.class.getClassLoader().setDefaultAssertionStatus(true);
			initializeSettings(args);
            Display.create();
			Renderer.runGame(args);
		} catch (Throwable t) {
			fail(t);
		} finally {
			shutdown();
		}
	}

	private static final void initializeSettings(String[] args) {
		System.out.println("Initializing settings...");
		if (args != null) {
			UpdateInfo update_info = null;
			String platform_dir;
			// TODO: Have this get set properly and fed to Renderer.runGame again.
			boolean grab_frames = false;
			boolean eventload = false;
			boolean zipped = false;
			boolean silent = false;
			if (Platform.get() == Platform.MACOSX) {
				platform_dir = "Library/Application Support" + File.separator;
			} else if (Platform.get() == Platform.LINUX) {
				platform_dir = ".";
			} else {
				platform_dir = "";
			}
			String game_dir_path = System.getProperty("user.home") + File.separator + platform_dir + Globals.GAME_NAME;
			File game_dir = new File(game_dir_path);
			Settings settings = new Settings();
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("--grabframes")) {
					grab_frames = true;
				} else if (args[i].equals("--eventload")) {
					eventload = true;
					i++;
					if (args[i].equals("zipped")) {
						zipped = true;
					} else if (args[i].equals("normal")) {
					} else
						throw new RuntimeException("Unknown event load mode: " + args[i]);
				} else if (args[i].equals("--bootstrap")) {
					String java_cmd = args[++i];
					settings.load(Utils.getInstallDir());
					String classpath = args[++i];
					File data_dir = new File(args[++i]);
					update_info = new UpdateInfo(java_cmd, classpath, data_dir);
				} else if (args[i].equals("--silent")) {
					silent = true;
				} else {
					throw new RuntimeException("Unknown command line flag: " + args[i]);
				}
			}
			game_dir.mkdirs();

			// fetch initial settings
			Settings.setSettings(settings);
			settings.load(game_dir);
			
		}

	}
}
