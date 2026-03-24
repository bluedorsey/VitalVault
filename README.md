<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/On--Device_AI-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white" />
</p>

<h1 align="center">🏥 VitalVault</h1>

<p align="center">
  <strong>Your private, offline AI assistant for medical records.</strong><br/>
  Scan documents · Ask questions · Get instant answers — all without leaving your device.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/API-24%2B-brightgreen?style=flat-square" />
  <img src="https://img.shields.io/badge/License-Private-blue?style=flat-square" />
  <img src="https://img.shields.io/badge/Network-NONE-red?style=flat-square&label=Internet%20Required" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline-success?style=flat-square" />
</p>

---

## ✨ What is VitalVault?

**VitalVault** is a fully **offline** Android application that lets you scan your medical documents (prescriptions, lab reports, X-rays) and then **ask questions about them in natural language**. Everything — OCR, embeddings, vector search, and AI generation — runs **entirely on-device**. Your health data never touches the internet.

> 🔒 **Zero network permissions.** Not even declared in the manifest.

---

## 🎯 Key Features

| Feature | Description |
|---------|-------------|
| 📷 **Smart Document Scanning** | Pick any image of a medical document — OCR extracts the text instantly using ML Kit |
| 🧠 **On-Device RAG Pipeline** | Full Retrieval-Augmented Generation: your question is embedded, matched against stored chunks, and grounded context is fed to the LLM |
| 💬 **Conversational AI Chat** | Ask questions like *"What was my blood sugar level?"* and get accurate, context-aware answers |
| 🗑️ **Secure Document Deletion** | Delete any document and **all** its associated vector data with one tap — confirmation dialog included |
| 🔐 **Privacy-First Architecture** | No internet, no cloud backup, no telemetry. Your medical data stays on YOUR device |
| ⚡ **Real-time Streaming** | AI responses stream token-by-token for a smooth, responsive chat experience |

---

## 🏗️ Architecture

VitalVault follows the **MVVM** pattern with a clean separation between UI, data, and AI layers:

```
┌──────────────────────────────────────────────────────────────┐
│                        UI Layer                               │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐    │
│  │  MainScreen  │    │ UploadScreen │    │  AppNavGraph  │    │
│  │  (Chat UI)   │    │ (Scan+Index) │    │  (Navigation) │    │
│  └──────┬───────┘    └──────┬───────┘    └───────────────┘    │
├─────────┼───────────────────┼────────────────────────────────┤
│         ▼    ViewModel Layer│                                 │
│  ┌──────────────┐           │                                 │
│  │ ChatViewModel │◄──────────┘                                │
│  │  (MVVM+RAG)  │                                            │
│  └──────┬───────┘                                            │
├─────────┼────────────────────────────────────────────────────┤
│         ▼       AI / ML Layer                                 │
│  ┌──────────┐  ┌────────────┐  ┌──────────┐  ┌───────────┐  │
│  │   OCR    │  │ TextChunker│  │ Embedding│  │  Gemma 2B │  │
│  │ (ML Kit) │  │ (Medical-  │  │  (USE)   │  │   (LLM)   │  │
│  │          │  │  Aware)    │  │          │  │           │  │
│  └──────────┘  └────────────┘  └──────────┘  └───────────┘  │
├──────────────────────────────────────────────────────────────┤
│                     Data Layer                                │
│  ┌───────────────────────────────────────────────────────┐   │
│  │              ObjectBox (Vector DB)                     │   │
│  │   Medicaldata (documents)  ←→  MedicalChunck (HNSW)  │   │
│  └───────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔄 RAG Pipeline Flow

```
  📷 Image                    💬 User Question
     │                              │
     ▼                              ▼
  ┌──────┐                    ┌──────────┐
  │  OCR │                    │ Embed    │
  │ML Kit│                    │ (USE)    │
  └──┬───┘                    └────┬─────┘
     ▼                              ▼
  ┌──────────┐               ┌────────────┐
  │  Chunk   │               │ HNSW Search│
  │(500 char)│               │ (Top 5)    │
  └────┬─────┘               └─────┬──────┘
       ▼                           ▼
  ┌──────────┐               ┌────────────┐
  │  Embed   │               │  Build     │
  │  (USE)   │               │  Context   │
  └────┬─────┘               └─────┬──────┘
       ▼                           ▼
  ┌──────────┐               ┌────────────┐
  │  Store   │               │  Gemma 2B  │
  │ObjectBox │               │  Generate  │
  └──────────┘               └─────┬──────┘
                                    ▼
                              💬 AI Response
```

**Ingestion** (left): Image → OCR → Chunk → Embed → Store  
**Query** (right): Question → Embed → Search → Context → Generate

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Language** | Kotlin | Primary language |
| **UI** | Jetpack Compose + Material 3 | Modern declarative UI |
| **Navigation** | Navigation Compose | Multi-screen routing |
| **LLM** | Gemma 2B (MediaPipe) | On-device text generation |
| **Embeddings** | Universal Sentence Encoder (TFLite) | 100-dim text vectors |
| **Vector DB** | ObjectBox with HNSW index | Fast nearest-neighbor search |
| **OCR** | Google ML Kit Text Recognition | Document text extraction |
| **Architecture** | MVVM + Coroutines | Reactive, lifecycle-aware |

---

## 📁 Project Structure

```
app/src/main/java/com/example/personalhealthcareapp/
├── 🏠 MainActivity.kt              # App entry point
├── 📱 navigation/
│   ├── Screen.kt                    # Route definitions
│   └── AppNavGraph.kt               # NavHost wiring
├── 🎨 uiux/
│   ├── MainScreen.kt                # Chat interface
│   └── UploadScreen.kt              # Document upload + management
├── 🧠 ViewModel/
│   └── ViewModel.kt                 # ChatViewModel (RAG orchestrator)
├── 💬 chat_managment/
│   └── Chat.kt                      # Chat message data class
├── 🤖 ai/
│   └── TextChunker.kt               # Medical-aware text chunking
├── 🔮 LLMinference/
│   └── llm.kt                       # Gemma 2B model manager
├── 📊 db/
│   ├── ObjectBox.kt                 # DB init, search, delete operations
│   ├── vectordb.kt                  # MedicalChunck entity (HNSW)
│   └── Embedding.kt                 # USE embedder
└── 👁️ vision/
    └── OCR.kt                       # ML Kit text recognition
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2+)
- **JDK 17+**
- **Android SDK 36**
- A device or emulator running **Android 7.0+** (API 24)

### Build & Run

```bash
# 1. Clone the repository
git clone https://github.com/bluedorsey/VitalVault.git
cd VitalVault

# 2. Open in Android Studio
#    File → Open → select the cloned directory

# 3. Sync Gradle (Ctrl+Shift+O)

# 4. Run on device/emulator (Shift+F10)
```

> [!IMPORTANT]
> The Gemma 2B model file (`.bin`) is not included in the repo due to its size. Place the model in the appropriate assets directory before running. See `CLAUDE.md` for model download instructions.

---

## 🔐 Security & Privacy

VitalVault is designed with **medical-grade privacy** in mind:

| Measure | Implementation |
|---------|---------------|
| 🌐 **No Internet** | `AndroidManifest.xml` has **zero** network permissions |
| ☁️ **No Cloud Backup** | `android:allowBackup="false"` prevents system backup of medical data |
| 📝 **No Data in Logs** | All `Log.d()` calls are sanitized — medical text is never printed |
| 🗑️ **Secure Deletion** | Deleting a document removes the record **and** all vector embeddings |
| 📱 **On-Device Only** | OCR, embeddings, vector search, and LLM all run locally |

---

## 📸 App Flow

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│             │     │                 │     │             │
│  Chat       │────▶│  Upload         │────▶│  Processing │
│  Screen     │  +  │  Screen         │     │  Pipeline   │
│             │     │                 │     │             │
│  • Ask      │     │  • Pick image   │     │  • OCR      │
│    questions │     │  • Give title   │     │  • Chunk    │
│  • Get AI   │     │  • Scan & Index │     │  • Embed    │
│    answers   │     │  • View saved   │     │  • Store    │
│  • Stream   │     │  • Delete docs  │     │             │
│    response  │     │                 │     │             │
└─────────────┘     └─────────────────┘     └─────────────┘
```

---

## 🤝 Contributing

This is a private project. Please contact the repository owner for contribution guidelines.

---

## 📄 License

This project is private and proprietary. All rights reserved.

---

<p align="center">
  <strong>Built with ❤️ for health data privacy</strong><br/>
  <sub>Your medical records. Your device. Your control.</sub>
</p>
