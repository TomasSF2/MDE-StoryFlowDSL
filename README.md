# StoryFlowDSL

Final grade: 18.5


**A Model-Driven Engineering Pipeline for Interactive Branching Narratives**

StoryFlowDSL is a complete MDE pipeline for authoring interactive branching narratives. A narrative author models their story graphically using the Sirius editor. The system automatically validates, transforms, formally verifies, and generates a self-contained browser-playable HTML game with zero manual coding.


## Prerequisites

| Tools 
|------
| Eclipse IDE 
| Sirius Workbench 
| Epsilon 
| EMF (Eclipse Modeling Framework) 
| Java 
| NuSMV 

### Eclipse plugins required

- `Epsilon` (EVL, ETL, EGL, EOL launchers)
- `Sirius` (graphical editor framework)
- `EMF — Eclipse Modeling Framework SDK`

---

## Import Instructions

### 1. Clone or extract the project

```bash
git clone <repo-url> storyflowdsl
```

### 2. Open Eclipse and set up the workspace


### 3. Import the Ecore metamodel projects

1. **File → Import → Existing Projects into Workspace**
2. **Browse Root Directory** and select **StoryFlowDSL** .
3. Eclipse will detect the StoryFlowDSL project.
4. Click **Finish**.
 

### 4. Register the metamodels in the Epsilon registry

> This step is required before running any EVL/ETL/EGL script.

1. Right click each .ecore file.
2. Select **Register EPackages**.

Alternatively, both metamodels are registered programmatically in the run configurations provided in each phase folder (see below).

---

## Pipeline Execution

The phases must be executed **in order**. Each phase's output is the next phase's input.

```
HLL model (.xmi)
      │
      ▼
[Phase 4] EVL validation ──── FAIL → fix model
      │ PASS
      ▼
[Phase 5] ETL transformation
      │
      ▼
  Pivot model (.xmi)
      │
      ├──▶ [Phase 6] EGL → HTML game
      │
      └──▶ [Phase 7] EGL → NuSMV → formal verification
```

---

### Phase 3 — Graphical Editor (Sirius)

> Use this phase to **author or edit** a story model. Skip to Phase 4 if you are using a pre-existing `.xmi`.

1. Follow the same runtime-Eclipse workflow used in the Sirius Lab 5 tutorial.
2. Import the sirius project from **03_sirius**.
3. The Sirius graphical editor opens. The palette on the right exposes: **Chapter**, **Scene**, **Choice**, **Variable**, **Item**, **Character**.
4. For advanced mechanics (Conditions, Effects, Dice Rules), right-click any Choice edge → **StoryFlow → Add Condition / Add Effect / Add Dice Rule** and fill in the popup.
5. Save with `Ctrl+S`. The underlying `.xmi` is updated automatically.

---

### Phase 4 — EVL Static Validation

> Validates the HLL model against 9 semantic constraints before any transformation.

1. Right-click `storyflow.evl` → **Run As → Run Configurations → EVL Validation** .
2. In the launch configuration dialog:
   - **Models tab**: add the target HLL `.xmi` file (e.g. `StoryBlackwellBreakIn_hll.xmi`). Set the metamodel URI to `storyflow_hll`.
   - Load/Store operations: Select only **Read on Load**
   - Leave all other defaults.
3. Click **Run**.

**Expected output (valid model):**

```
*(Empty Console)* 
```
In Epsilon, a successful EVL validation runs silently by default. If no errors, warnings, or stack traces are printed in the console, it means the model is perfectly valid and all constraints passed successfully.

**Expected output (invalid model):**

```
DiceRule has invalid configuration: Required: min < max and min <= successThreshold <= max
Story 'Broken' must have exactly one start scene, but where found:2
...
```

If any violation is reported, **fix the model before proceeding**. The pipeline will not produce correct output from an invalid model.

---

### Phase 5 — ETL Model Transformation

> Transforms a validated HLL `.xmi` into a Pivot `.xmi`.

1. Right-click `story_to_pivot.etl` → **Run As → ETL Transformation**.
2. In the launch configuration dialog:
   - **Models tab — Source**: select the HLL `.xmi` and set its metamodel URI to `storyflow_hll`.
   - **Models tab — Target**: create a new output file, e.g. `StoryBlackwellBreakIn_pivot.xmi`, and set its metamodel URI to `storyflow_pivot`.
   - Load/Store operations: Select **Read on Load** for the source and **Store on disposal** for the target.
3. Click **Run**.

> **Traceability note:** Element IDs in the Pivot follow the convention `sceneName_choice_N` (e.g. `dana_room_choice_1`), making every Pivot element traceable to its HLL origin by inspection.

---

### Phase 6 — EGL Code Generation

> Generates a self-contained HTML/JS interactive story runner from the Pivot model.

1. Right-click `storyflow.egx` → **Run As → EGL Generator**.
2. In the launch configuration dialog:
   - **Models tab**: select the Pivot `.xmi` produced in Phase 5. Set metamodel URI to `storyflow_pivot`.
   - Load/Store operations: Select only **Read on Load**
   - **Output tab**: set the output folder to `/output/`.
3. Click **Run**.

**Expected output:**

```
Output generated to /model/output.html
Successfully wrote to /model/output.html

```

4. Open `output.html` in any modern browser (Chrome, Firefox, Edge, Safari).
   - No server required — the file is fully self-contained.

---

### Phase 7 — NuSMV Formal Verification

> Translates the Pivot model to NuSMV and verifies CTL safety properties.

#### Step 7a — Generate the `.smv` file

1. Right-click `generate_nusmv.egx` → **Run As → EGL Generator**.
2. In the launch configuration dialog:
   - **Models tab**: select the Pivot `.xmi`. Set metamodel URI to `storyflow_pivot`.
   - Load/Store operations: Select only **Read on Load**
   - **Output tab**: set the output folder to `/output/`.
3. Click **Run**.


#### Step 7b — Run NuSMV verification

Ensure `nusmv` is on your system `PATH`, then run from a terminal:

```bash
nusmv output/<filename>.smv
```

# Properties Verified

| ID | Natural language | CTL / INVAR formula |
|----|------------------|---------------------|
| P1 | Every declared ending is reachable from the start | `CTLSPEC EF node = n_<ending>;` (one per accepting node) |
| P2 | No non-ending scene is a deadlock (a move is always available) | `CTLSPEC AG (!is_accepting -> can_move);` |
| P3 | A locked scene is never entered without its required item | `INVARSPEC !(node = n_<scene> & !has_<item>);` (else `CTLSPEC AG TRUE;`) |
| P4 | No unintended infinite loop: an ending stays reachable from every state | `CTLSPEC AG EF is_accepting;` |
| P5 | At least one GOOD ending is reachable | `CTLSPEC EF is_good_ending;` |

**Expected output (all properties hold):**

```
-- specification EF node = n_ending_clarity  is true
-- specification EF node = n_ending_safe_together  is true
-- specification AG (!is_accepting -> can_move)  is true
-- specification AG (EF is_accepting)  is true
-- specification EF is_good_ending  is true
-- invariant !(node = n_lamp_room & !has_lighthouse_key)  is true
```

**Expected output (T4 semantic failure — property violated):**

```
-- specification EF node = n_ending_save_everyone  is false
-- as demonstrated by the following execution sequence
Trace Description: CTL Counterexample
Trace Type: Counterexample
-> State: 1.1 <-
  node = n_diner
  has_bunker_key = FALSE
  can_move = TRUE
  is_good_ending = FALSE
  is_accepting = FALSE
-- specification EF node = n_ending_storm  is true
-- specification AG (!is_accepting -> can_move)  is true
-- specification AG (EF is_accepting)  is true
-- specification EF is_good_ending  is false
-- invariant !(node = n_ending_save_everyone & !has_bunker_key)  is true
```

If a counterexample is returned for P1, a declared ending is permanently unreachable. Examine the counterexample trace to identify which guard configuration causes the lock.

---

## Running the Test Suite

The test suite covers four failure categories. Each has its own subfolder under `08_tests/`.

| Category | Model to use | How to run | Expected result |
|----------|-------------|-----------|----------------|
| **T1 — Positive** | `T1_Positive/*.xmi` | Run full pipeline (P4→P7) | All phases pass, HTML game generated, NuSMV properties hold |
| **T2 — Metamodel Failure** | `T2_Metamodel_Failure/*.xmi` | Open in Eclipse; check Problems view | EMF structural validation errors before EVL |
| **T3 — EVL Failure** | `T3_EVL_Failure/*.xmi` | Run Phase 4 (EVL) | 7 constraint violations, pipeline blocked |
| **T4 — Semantic Failure** | `T4_Semantic_Failure/*.xmi` | Run full pipeline (P4→P7) | EVL passes, NuSMV returns counterexample |


