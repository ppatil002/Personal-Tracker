# Vocabulary Module - High-Level Technical Overview

The Vocabulary module in Personal Tracker is a intelligent spaced-repetition learning system. It combines friction-free word ingestion via Android Share Intents, automated AI-powered definition generation using Google Gemini LLM, precise revision scheduling via `AlarmManager`, and a Machine Learning Half-Life decay engine for optimal memory retention.

---

## High-Level Architecture Flow

```
┌─────────────────────────┐
│ Share Intent / UI Input │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐      ┌─────────────────────────────────┐
│   Database Persistence  ├─────►│  LLM Auto-Completion (Gemini)   │
└────────────┬────────────┘      └─────────────────────────────────┘
             │
             ▼
┌─────────────────────────┐
│   Half-Life ML Engine   │ ──► Calculates optimal due date
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ AlarmManager Scheduler  │ ──► Triggers scheduled notification window
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ Interactive Notification│ ──► Captures 1–5 ★ user rating
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│  SGD Weight Adjustment  │ ──► Online model optimization
└─────────────────────────┘
```

---

## Feature Implementation Details

### 1. Ingestion & Share Intent Integration
Users can add vocabulary words directly within the app or share highlighted text from external Android apps (browsers, reader apps, e-books) using the system share menu (`Intent.ACTION_SEND`).

- **Entry Point**: `VocabularyShareActivity`
- **Smart Context Ingestion**:
  - **Single Word**: Inserts a new vocabulary entry initialized with baseline memory parameters.
  - **Part of Speech**: Text matching `"noun"`, `"verb"`, `"adjective"`, or `"adverb"` is automatically attached as the part of speech for the most recently added word.
  - **Definitions (>3 words)**: Text with more than three words is attached as the definition/meaning for the last added word.

---

### 2. LLM Enrichment & Auto-Completion
When vocabulary items have missing fields (part of speech, definition, or example sentence), the system leverages Google Gemini LLM to enrich them automatically.

- **Manager**: `VocabularyLlmManager`
- **Prompt Engine**: Formats structured prompts (`VocabularyPrompts.VOCABULARY_ENRICHMENT_PROMPT`) passing the word and existing context.
- **Modes of Execution**:
  - **Background Worker**: Periodically fetches incomplete words from the database and updates missing fields without user intervention.
  - **Interactive UI Batch**: Allows users to manually trigger batch auto-completion with real-time progress updates on screen.

---

### 3. Revision Scheduling & Notifications
To avoid overwhelming the user while ensuring timely reviews, revision notifications are queued into hardcoded daily time windows.

- **Scheduler**: `VocabularyScheduler`
- **Receiver**: `VocabularyReceiver`
- **Notification Helper**: `VocabularyNotificationHelper`
- **Daily Schedule Windows**: 12:30, 15:30 (3:30 PM), 17:30 (5:30 PM), 19:00 (7:00 PM), and 23:30 (11:30 PM).
- **Word Selection Strategy**: Queries the database for overdue vocabulary items based on target recall probability, falling back to the oldest reviewed items if no items are currently overdue.
- **Interactive Notification Cards**:
  1. Displays the vocabulary word.
  2. Provides an interactive option to reveal a **Hint**.
  3. Prompts for **Remembered** vs. **Forgotten**.
  4. Allows selecting a **1 to 5 Star Rating** directly from the notification shade to log recall difficulty.

---

### 4. Machine Learning & Half-Life Decay Model
The core memory engine (`HalfLifeEngine` & `HalfLifeUtils`) uses a Half-Life Regression (HLR) model to predict memory decay and determine the optimal revision time for each word.

#### Mathematical Foundation
1. **Half-Life Calculation**:
   The memory half-life $h$ (in days)—the duration until recall probability drops to $50\%$—is calculated based on correct attempts, incorrect attempts, and model weights $(w_0, w_1, w_2)$:
   $$h = 2^{(w_0 + w_1 \cdot \text{correctAttempts} + w_2 \cdot \text{incorrectAttempts})}$$

2. **Recall Probability $P(t)$**:
   The predicted probability of recalling a word after $\Delta t$ days since its last review is:
   $$P(t) = 2^{-\frac{\Delta t}{h}}$$

3. **Due Date Computation**:
   A word is scheduled for revision when its recall probability decays to the target retention threshold ($85\%$ or $0.85$):
   $$\Delta t_{\text{due}} = -h \cdot \log_2(0.85)$$
   $$\text{Timestamp}_{\text{due}} = \text{Timestamp}_{\text{lastReviewed}} + \Delta t_{\text{due}}$$

#### Review Rating to Performance Weights
User feedback (1 to 5 Stars) is mapped into fractional credit/penalty deltas:
- **1 Star (Complete failure)**: $+0.0$ correct, $+1.0$ incorrect
- **2 Stars (Forgot/Hard)**: $+0.0$ correct, $+0.5$ incorrect
- **3 Stars (Good/Pass)**: $+0.3$ correct, $+0.0$ incorrect
- **4 Stars (Great)**: $+0.7$ correct, $+0.0$ incorrect
- **5 Stars (Mastered)**: $+1.0$ correct, $+0.0$ incorrect

#### Stochastic Gradient Descent (SGD) Model Tuning
Model weights $(w_0, w_1, w_2)$ are updated online using Stochastic Gradient Descent (SGD) to minimize prediction error between predicted recall $P(t)$ and actual recall outcome $y$:
$$\text{Error} = P(t) - y$$
$$w_0 \leftarrow w_0 - \alpha \cdot \text{Error}$$
$$w_1 \leftarrow w_1 - \alpha \cdot \text{Error} \cdot \text{correctAttempts}$$
$$w_2 \leftarrow w_2 - \alpha \cdot \text{Error} \cdot \text{incorrectAttempts}$$
*(where learning rate $\alpha = 0.01$)*

---

## Summary for Engineers

1. **Ingestion**: Words are received from standard share intents or manual entry.
2. **Auto-Complete**: Missing fields are enriched via `VocabularyLlmManager` using Gemini LLM.
3. **Scheduling**: `VocabularyScheduler` schedules alarms for fixed daily windows and selects overdue words.
4. **Interactive Review**: User rates recall (1–5 Stars) directly from notifications or UI.
5. **Memory Update**: `HalfLifeEngine` updates half-life $h$, recalculates next revision date, logs review history, and optimizes model weights using SGD.
