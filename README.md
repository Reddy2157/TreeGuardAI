# TreeGuard AI — Urban Forest Telemetry & Health Management Platform

TreeGuard AI is a production-grade Android application built with **Kotlin** and **Jetpack Compose (Material Design 3)** for urban foresters, municipal arborists, and environmental planners. It combines real-time GPS telemetry, CameraX image screening, historical **Storm Memory** vision analysis, an explainable tree inspection-priority scoring engine ($R$), and Firebase cloud persistence.

---

## 🌟 Key Features

### 1. 📡 Command Centre (Telemetry & Health Trends)
- **Urban Forest Metrics**: Live tracking of total canopy assets, critical health alerts, inspection-priority distribution, and Stress Debt accumulation.
- **30-Day Health Score Trends**: Clean visual trajectory chart representing forest health degradation and recovery across monsoonal weather cycles.
- **Registered Photo Archive**: Visual gallery of high-resolution bark and canopy photographs stored on-device and in Firestore.
- **Voice Intelligence (TTS)**: One-tap audio briefings powered by Gemini AI and Android TextToSpeech.

### 2. 🗺️ Interactive Canopy Map (Google Maps & GIS Radar)
- **Multi-Engine GIS**: Toggle between **Google Maps Compose** (with color-coded priority markers) and a **Midnight Forest Radar GIS** canvas.
- **Explainable Inspection Priority Color-Coding**:
  - 🔴 **Critical Priority**: Immediate arborist intervention required.
  - 🟠 **High Priority**: Structural compromise or storm damage.
  - 🟡 **Moderate Priority**: Elevated stress debt or pest symptoms.
  - 🟢 **Low Priority**: Healthy canopy architecture.
- **Marker Telemetry Overlay**: Tapping any tree displays its stored photograph, GPS coordinates, species, and a direct link to open its **Digital Tree Passport**.

### 3. 📸 Scan & CameraX Tree Registration
- **CameraX & Photo Picker**: Capture live camera photos or select botanical images with auto-focus, zoom, and torch support.
- **Real GPS Location Acquisition**: Integrated `play-services-location` (`FusedLocationProviderClient`) automatically tags precise latitude and longitude fixes to each registered tree photograph.
- **AI Tree Doctor (Gemini 3.8 Flash)**: Multimodal botanical screening analyzing crown health, fungal fissures, limb splitting, and observable hazards.
- **Arborist Maintenance Logging**: Append field interventions (crown thinning, cabling, antifungal soil drenches) with estimated cost in INR (₹) and projected benefit ratings directly to Firestore.
- **Stored Images Archive**: Horizontal gallery displaying saved inspection frames with GPS coordinates, capture dates, and image resolution.

### 4. 🧠 Intelligence & Environmental Stress Debt
- **Stress Debt Accumulation Engine ($TD_t$)**:
  $$TD_t = TD_{t-1} \cdot \lambda + S_t$$
  Tracks compounding environmental fatigue across monsoons, cyclones, and heatwaves using half-life decay ($\lambda = 2^{-1/30}$).
- **Municipal Budget Optimizer**: Dynamic integer knapsack algorithm allocating municipal funds (e.g. ₹50,000 to ₹500,000) to maximize forest health benefits per rupee spent.
- **Storm Memory Visual Comparison**: Side-by-side computer vision comparison evaluating baseline photographs vs. post-storm condition to detect crown loss and lean angle shifts.
- **Grounded Search Intelligence**: Google Search-grounded queries providing real-time botanical guidelines for South Indian tropical trees (Neem, Banyan, Gulmohar, Rain Tree, Ashoka).

### 5. 🪪 Permanent Digital Tree Passport
- **Unique Botanical QR Matrix**: Generates a shareable QR identity code for each tree in the urban registry.
- **Longitudinal Archive**: Permanent record tracking every dated observation, weather snapshot, and arborist maintenance action.
- **Interactive Risk Factor Tuning**: Live sensitivity adjustments for structural ($S$), exposure ($E$), decline ($D$), weather ($W$), and history ($T$) scores.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: 100% Jetpack Compose with Material Design 3 (Midnight Forest Console theme).
- **Architecture**: MVVM with unidirectional data flow (Kotlin Coroutines & `StateFlow`).
- **Backend & Cloud**:
  - **Firebase Firestore**: Real-time cloud synchronization with strict zero-trust security rules (`firestore.rules`).
  - **Firebase Authentication**: User data gating and Google Sign-In via Jetpack Credential Manager.
- **Device Hardware & Sensors**:
  - **CameraX** (`androidx.camera:camera-*` 1.4.1)
  - **Google Play Services Location** (`com.google.android.gms:play-services-location` 21.3.0)
  - **Google Maps Compose** (`com.google.maps.android:maps-compose` 4.4.1)
- **Local Storage**: `TreeImageStorage` with 32MB in-memory LRU cache and persistent app disk storage (`filesDir/tree_photos/`).
- **Testing**: Robolectric JVM unit tests verifying Firestore security rules, priority engines, and navigation CUJs.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Hedgehog 2023.1.1 or newer)
- **JDK 17+**
- **Android SDK API 34** (Minimum SDK: 26)

### Installation & Build

1. **Clone the repository**:
   ```bash
   git clone <your-github-repo-url>
   cd treeguard-ai
   ```

2. **Configure API Keys**:
   - Create a `.env` file in the project root:
     ```env
     GEMINI_API_KEY=your_gemini_api_key_here
     MAPS_API_KEY=your_google_maps_api_key_here
     ```
   - *Note*: In Google AI Studio, these are automatically managed via the **Secrets** panel.

3. **Build the APK**:
   ```bash
   gradle assembleDebug
   ```

4. **Run Unit & Robolectric Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 🔒 Security & Privacy
- Zero-trust security rules ensure users can only access and modify their authenticated urban forest inventories.
- API keys are injected via `BuildConfig` using the Secrets Gradle Plugin; no credentials are hardcoded into source code.
- Photo selection utilizes the zero-permission Android Photo Picker contract alongside CameraX least-privilege permissions.
