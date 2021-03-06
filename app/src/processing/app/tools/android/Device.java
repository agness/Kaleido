/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2009-10 Ben Fry and Casey Reas

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.app.tools.android;

import java.io.IOException;
import processing.app.Base;
import processing.core.PApplet;

public class Device {

  /** Name of this device. */
  String name;

  /** "android-4" or "Google Inc.:Google APIs:4" */
  String target;

  /**
   * Default virtual device used by Processing, intended to be similar to a
   * device like a T-Mobile G1 or myTouch 3G. Uses Android 1.6 (Donut) APIs, and
   * the screen is 480x320 pixels, or HVGA (Half VGA).
   */
  // static Device avdDonut =
  // // Using the generic AVD causes a prompt to show up on the console,
  // // so using the Google version instead which doesn't ask for a profile.
  // new Device("Processing-Donut", "Google Inc.:Google APIs:4", 480, 320);
  /**
   * Default virtual device used by Processing, designed to be similar to a
   * device like the Moto Droid. Uses Android 2.0 APIs, and the screen is set to
   * WVGA854 (854x480), the same aspect ratio (with rounding), as 1920x1080, or
   * 16:9.
   */
  static Device avdEclair = new Device("Processing-Eclair",
                                       "Google Inc.:Google APIs:5", 854, 480);

  static final String AVD_CREATE_ERROR = "An error occurred while running “android create avd”\n"
      + "to set up the default Android emulator. Make sure that the\n"
      + "Android SDK is installed properly, and that the Android\n"
      + "and Google APIs are installed for level 5.";

  static final String ADB_DEVICES_ERROR = "Received unfamiliar output from “adb devices”.\n"
      + "The device list may have errors.";

  static final int DEFAULT_WIDTH = 320;

  static final int DEFAULT_HEIGHT = 480;

  Device(final String name, final String target, final int width,
         final int height) {
    this.name = name;
    this.target = target;
  }

  static boolean checkDefaults() {
    try {
      // if (!avdDonut.exists()) {
      // if (!avdDonut.create()) {
      // Base.showWarning("Android Error",
      // "An error occurred while running “android create avd”\n" +
      // "to set up the default Android emulator.", null);
      // }
      // }
      if (!avdEclair.exists()) {
        if (!avdEclair.create()) {
          Base.showWarning("Android Error", AVD_CREATE_ERROR, null);
        }
      }
      return true;

    } catch (final Exception e) {
      Base.showWarning("Android Error", AVD_CREATE_ERROR, e);
    }
    return false;
  }

  protected boolean exists() throws IOException {
    final ProcessHelper p = new ProcessHelper(Android.toolName, "list", "avds");
    try {
      if (p.execute() == 0) {
        for (final String line : p.getStdout().split("\n")) {
          final String[] m = PApplet.match(line, "\\s+Name:\\s+(\\S+)");
          // PApplet.println(m);
          if (m != null) {
            if (m[1].equals(name)) {
              return true;
            }
          }
        }
      } else {
        p.dump();
      }
    } catch (final InterruptedException ie) {
    }
    return false;
  }

  protected boolean create() throws IOException {
    final ProcessHelper p = new ProcessHelper(Android.toolName, "create",
                                              "avd", "-n", name, "-t", target,
                                              "-c", "64M");

    try {
      if (p.execute(true, false) == 0) {
        return true;
      }
      System.out.println("Attempted: '"
          + PApplet.join(new String[] {
            Android.toolName, "create", "avd", "-n", name, "-t", target, "-c",
            "64M" }, " ") + "'");
      p.dump();
    } catch (final InterruptedException ie) {
    }

    return false;
  }

  static void sendMenuButton(final String device) throws IOException {
    // adb -s emulator-5566 shell getevent

    // back on G1
    // /dev/input/event3: 0001 008b 00000001
    // /dev/input/event3: 0001 008b 00000000

    // back on the emulator
    // /dev/input/event0: 0001 00e5 00000001
    // /dev/input/event0: 0001 00e5 00000000

    if (device.startsWith("emulator-")) {
      sendKey(device, 0x00e5);
    } else {
      sendKey(device, 0x008b);
    }
  }

  static void sendHomeButton(final String device) throws IOException {
    sendKey(device, 0x0066); // 102

    // home on the G1
    // /dev/input/event3: 0001 0066 00000001
    // /dev/input/event3: 0001 0066 00000000

    // home on the emulator
    // /dev/input/event0: 0001 0066 00000001
    // /dev/input/event0: 0001 0066 00000000
  }

  static void sendKey(final String device, final int key) throws IOException {
    final String inputDevice = device.startsWith("emulator") ? "/dev/input/event0"
        : "/dev/input/event3";
    final String[] cmd = new String[] {
      "adb", "-s", device, "shell", "sendevent", inputDevice, "1",
      String.valueOf(key), "1" // start with
    // key down
    };
    try {
      int result;
      int attempts = 0;
      do {
        result = Runtime.getRuntime().exec(cmd).waitFor();
        attempts++;
      } while (result != 0 && attempts < 5);

      attempts = 0;
      cmd[cmd.length - 1] = "0"; // send key up
      do {
        result = Runtime.getRuntime().exec(cmd).waitFor();
        attempts++;
      } while (result != 0 && attempts < 5);

    } catch (final InterruptedException ie) {
    }
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static protected String[] list() throws IOException {
    final ProcessHelper p = new ProcessHelper("adb", "devices");
    try {
      final int result = p.execute(false);
      if (result != 0) {
        System.err.println(p.getStderr());
        return null;
      }
      final String[] lines = p.getStdout().split("\n");
      // First line starts "List of devices"

      // when an emulator is started with a debug port, then it shows up
      // in the list of devices.

      // List of devices attached
      // HT91MLC00031 device
      // emulator-5554 offline

      // List of devices attached
      // HT91MLC00031 device
      // emulator-5554 device

      // for (String s : lines) {
      // System.out.println("Device.list(): '" + s + ".");
      // }

      if (lines.length == 0) {
        // result was 0, so we're ok, but this is odd.
        System.out.println("No devices found.");
        return new String[] {};
      }

      int startIndex = 0;

      if (lines[startIndex]
          .startsWith("* daemon not running. starting it now *")) {
        // just pretend he didn't say that.
        startIndex++;
      }

      if (lines[startIndex].startsWith("* daemon started successfully *")) {
        // ignore that too
        startIndex++;
      }

      // might read "List of devices attached"
      if (!lines[startIndex].startsWith("List of devices")) {
        Base.showWarning("Android Error", ADB_DEVICES_ERROR, null);
      }
      startIndex++;
      String[] devices = new String[lines.length - startIndex];
      int deviceIndex = 0;
      for (int i = startIndex; i < lines.length - 1; i++) {
        final String line = lines[i];
        final int tab = line.indexOf('\t');
        if (tab != -1) {
          devices[deviceIndex++] = line.substring(0, tab);
        } else if (line.trim().length() != 0) {
          System.out.println("Unknown “adb devices” response: " + line);
        }
      }
      devices = PApplet.subset(devices, 0, deviceIndex);
      return devices;
    } catch (final InterruptedException ie) {
      // ignored, just other thread fun
    }
    return null;
  }
}