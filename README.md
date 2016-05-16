# MEGAndroid
Android App for MEG. Not finished yet

## Installation Instructions

### Helpful Resources
- [How To Get Started With Android Programming](http://x-team.com/2016/01/how-get-started-android-programming/)

### For Normal Users
[Needed]

### For Developers

#### OSX
[Needed]

#### Windows
[Needed]

#### Linux (Distributions may vary)

##### Install the needed software (Linux)

1. Java Development Kit (JDK) 8
2. Android Studio 
3. Extras (includes the Android Debug Bridge)
   - `sudo apt-get install android-tools-adb`

##### Setup the project in Android Studio

1. Connect your phone to your development computer as [explained here](connect-your-phone.md).
2. Launch Android Studio and step through the setup wizard.
   - Click `Next` through the first screen or two (options should be obvious) until you get to the `Installation Type screen`. 
   - On the `Installation Type` screen select `Custom` and click `Next`.
   - Select your IDE theme etc. 
   - On the `SDK Components Setup` screen select all checkboxes  and click `Next`.
   - Click through the remaining screens (if any) until you are presented with a `Finish` button. Click it.
3. Once the Android Studio setup process is finished you can start a new project. Click `Checkout project from Version Control` and select `GitHub`.
   - Enter your GitHub username and password. Click `Login`.    
     *Note: you might need to fork the code from hahnicity/MEGAndroid first if you don't have write access to the repository.*
   - Setup a master password for the database or leave blank to disable password protection.
   - Select the MEGAndroid repository or fork from your GitHub account.
   - Select the location where you want the code to reside. The directory name should already be correct, so don't change it. Click `Clone`.
4. On the `Import Project from Gradle` screen, select `Use default gradle wrapper (recommended)`. Now, before you can continue, you'll need to make a `local.properties` file in your cloned MEGAndroid project.
   - Go to the location where your MEGAndroid project exists. Inside the MEGAndroid folder create a file called `local.properties`.
   - On the first line of the `local.properties` file put something like the following `sdk.dir=/path_to/sdk`.   
     *Note: on Linux this will probably be something like* `sdk.dir=/home/user/Android/Sdk/`
5. Leave the other settings on  the `Import Project from Gradle` screen at their defaults. Click `OK`. 
6. You should now be able to finish out the installation.

##### Install the app on your phone
1. Select `Run > Debug` from the menu (note in some versions of Android Studio you might need to select `installDebug` from the Installation tab). This should trigger the installation on your phone. 
2. After the installation completes, you'll need to give MEG the proper permissions on your phone. Enable the `Camera` by going to `Settings > Apps > MEG > Permissions`.

##### Troubleshooting
1. You get a message that says something to the effect of, *Failed to find Build Tools revision 23.0.1*.
   - Run the command `android list sdk -a` in the terminal. Make note of the list. Find the one with the same version number as the error message, in this case 23.0.1. Remember the line number.
    - If this fails go to step 2 below.
   - Run a command similar to this one: `android update sdk -a -u -t 3` but change the last argument. 
    - IMPORTANT: the last number after `-t` should be the line number from the previous step.
   - Your sdk will now update and you should be able to complete the setup process.
2. If you can't run android commands from the terminal:
   - In the MEGAndroid directory run the following commands in succession:
    - `export ANDROID_HOME=~/Android/Sdk` (change "~" to "$HOME" on Mac)
    - `export PATH=$ANDROID_HOME/tools:$PATH`
   - Test the above by typing `android` in the terminal. If it launches the *Android SDK Manager*, you know it works. Close the *SDK Manager*. You should now be able to continue with your setup.
3. If the project installation still fails:
   - Run the following command: `set ANDROID_HOME=/path_to/sdk`. 




