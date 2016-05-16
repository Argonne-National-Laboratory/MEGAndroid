# Getting your phone connected

## Android 6.0

### Helpful Resources
- [Using Hardware Devices](https://developer.android.com/tools/device.html)

### Setup your phone to connect over USB

1. Enable Software from Unknown Sources
   - `Settings > Security > Unknown Sources`
2. Turn on `Developer Options`
   - Go to `Settings > About Phone`
   - Find `Build Number` at the bottom of the screen and tap it 7 times. Developer options should now appear on the previous screen.
   - Navigate to the previous screen and tap on `Developer options`.
       - Turn on `USB debugging`.

### Connect your phone to the development computer

1. Connect your phone to the computer with a USB cable.
   - You should see a dialog asking you to accept an RSA key that allows debugging through this computer. Say, "yes". Check the box that says, "always allow this computer" if you wish.
   - Swipe down to get the Android notifications from the top of your phone screen. You should see a notification that says `USB for charging touch for more options`. Touch it and select `File Transferst (MTP)`. You should now have access to the phone's internal storage con your computer.
   - On linux or mac you can run the command `abd devices` to confirm your device is connected. You should see your device listed in the output. If you don't have the command `abd` you need to install the Android Debug Bridge.
