# APKTASK

A simple and efficient Android task manager for daily productivity

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)

---

## About

APKTASK is a lightweight Android application designed for managing daily tasks with a 24-hour lifecycle. Built with Java, it provides a clean and intuitive interface for adding, editing, and organizing tasks locally on your device.

The app focuses on simplicity and efficiency, helping you stay organized without unnecessary complexity.

---

## Features

### Core Functionality

- Add Tasks: Quickly create new tasks with validation to prevent empty entries
- Edit Tasks: Modify existing tasks with a simple toggle between edit and validate modes
- Delete Tasks: Remove completed or unwanted tasks safely using ID-based deletion
- Task Organization: Three zones for managing task states (In Progress, Completed, Deleted)
- Local Storage: All tasks are saved locally using SharedPreferences
- Daily Reset: Automatic task cleanup at midnight via MidnightReceiver

### User Experience

- Theme-Adaptive UI: Text colors automatically adjust based on light/dark system theme
- Input Validation: Prevents adding empty tasks or exceeding the 10-task limit
- State Management: EditText fields lock after validation to prevent accidental edits
- Smart Deletion: Only validated tasks can be deleted; empty lines are protected

---

## Architecture

### Project Structure

```
APKTASK/
├── app/
│   └── src/
│       └── main/
│           └── java/com/example/apktask/
│               ├── MainActivity.java        # Main activity and UI logic
│               ├── Task.java               # Task model class
│               └── MidnightReceiver.java   # Broadcast receiver for daily reset
├── LICENSE
└── README.md
```

### Key Components

**Task.java** - Task Model
- Properties: id, title, isDone, createdAt, status
- Serializable for SharedPreferences storage

**MainActivity.java** - Main Activity
- Dynamic task line creation using LinearLayout
- ArrayList-based task management
- ID-based task tracking using setTag()/getTag()
- Theme-adaptive text coloring

**MidnightReceiver.java** - Background Service
- Triggers automatic task cleanup at midnight
- Ensures daily task list reset

---

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Minimum SDK: API 21 (Android 5.0 Lollipop)
- Target SDK: API 34 (Android 14)
- Java Development Kit (JDK) 8 or higher

### Installation

1. Clone the repository

```bash
git clone https://github.com/Yolrie/APKTASK.git
cd APKTASK
```

2. Open in Android Studio
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned APKTASK folder

3. Build the project

```bash
./gradlew build
```

4. Run on device/emulator
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio

---

## Usage

### Adding a Task

1. Type your task description in the input field
2. Click the "Ajouter" (Add) button
3. The task is saved and the field locks automatically
4. A new empty line appears for the next task

### Editing a Task

1. Click the "Modifier" (Edit) button on a validated task
2. The input field unlocks for editing
3. Modify the text as needed
4. Click "Valider" (Validate) to save changes

### Deleting a Task

1. Click the "Supprimer" (Delete) button on any validated task
2. The task is removed from both the UI and storage
3. Empty (non-validated) lines cannot be deleted

### Task States

- Status 0: Draft (not yet validated)
- Status 1: In Progress (validated, active)
- Status 2: Completed (checked off)
- Status 3: Cancelled/Deleted

---

## Technical Highlights

### Smart Deletion System

Uses Android's setTag() mechanism to store task IDs directly in view elements, ensuring accurate deletion even with duplicate task names:

```java
nouvelleLigne.setTag(nouvelleTache.id);  // Store ID in view
int taskId = (Integer) nouvelleLigne.getTag();  // Retrieve for deletion
```

### Theme-Adaptive Coloring

Automatically adjusts text color based on system theme:

```java
editText.setTextColor(ContextCompat.getColor(this, R.color.task_text_color));
```

With separate color definitions in values/colors.xml and values-night/colors.xml.

### Validation Logic

Prevents common user errors:
- Empty task submission blocked at input level
- Maximum 10 concurrent tasks enforced
- Deletion restricted to validated tasks only

---

## Development

### Recent Updates

- Jan 29, 2026: Updated task text color logic for better theme support
- Jan 26, 2026: Implemented task cancellation and custom app icons
- Jan 23, 2026: Added task editing with validation toggle
- Jan 22, 2026: Implemented ID-based safe deletion

### Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (git checkout -b feature/AmazingFeature)
3. Commit your changes (git commit -m 'feat: Add AmazingFeature')
4. Push to the branch (git push origin feature/AmazingFeature)
5. Open a Pull Request

---

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

---

## Author

Quentin GERGAUD (@Yolrie)

---

## Acknowledgments

- Built as a learning project for Android development
- Inspired by the need for a simple, distraction-free task manager
- Special thanks to the Android development community