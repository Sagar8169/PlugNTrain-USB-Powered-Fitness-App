📦 Step 1: Project Setup in Android Studio

1. Open Android Studio.
2. Click on ‘Open’ and select the source code folder (which I have already provided to you).
3. Let it sync completely (Gradle sync).
4. Go to: Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Once done, click ‘Locate’ to get the generated APK.

📲 Step 2: Install APK on Android Device
1. Copy the generated APK to your Android phone.
2. Use any file manager to install the APK.
3. If prompted, allow ‘Install from unknown sources’.

🔌 Step 3: Prepare the USB Pendrive
1. Format your pendrive as FAT32 (if not already).
2. Inside the pendrive, create this structure exactly:

<USB Root>/
├── videos/
│   ├── upper_body.mp4
│   ├── lower_body.mp4
│   └── ... (your video files)
├── config/
│   └── menu.json


✅ I have already shared the videos and config folders with you. Just copy them directly into the root of your USB.

📁 menu.json File Info
This file controls the zones, modes, and button layout in the app.
You do NOT need to create or modify this file yourself.
If needed in the future, you can open and edit menu.json in the config folder using any text editor like Notepad.


🚀 Step 4: Launch the App & Give Permissions
1. Plug your pendrive into the mobile via OTG cable.
2. Open the PrevenFit app.
3. Grant storage permission when prompted.
4. The app will automatically: ✅ Load buttons based on menu.json ✅ Link videos from the videos folder


⚠️ Important Tips
* Don’t rename videos, config, or menu.json.
* Plug in USB before opening the app.
* All video file names mentioned in menu.json must exist in the videos folder.

📞 Feel free to contact me anytime if you need help or clarification.
