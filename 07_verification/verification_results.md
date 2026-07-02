# StoryFlowDSL — Phase 7 Verification Results

## 1. Positive models (T1) — all properties expected to hold

| Model                      | P1 (endings reachable)              | P2 (no deadlock) | P3 (locked scenes)                                                                      | P4 (no livelock) | P5 (good ending) |
|----------------------------|-------------------------------------|------------------|-----------------------------------------------------------------------------------------|------------------|------------------|
| The Lighthouse Vision      | hold (clarity, safe_together)       | hold             | hold — `!(n_lamp_room & !has_lighthouse_key)`                                           | hold             | hold             |
| The Blackwell Break-In     | hold (proof, caught)                | hold             | hold — `!(n_sealed_cabinet & !has_keycard)`                                             | hold             | hold             |
| The Train Tracks           | hold (saved, close_call, too_late)  | hold             | n/a — `AG TRUE` (no scene exclusively item-locked)                                      | hold             | hold             |
| The End of the World Party | hold (exposed, silenced, slip_away) | hold             | hold — `!(n_supply_closet & !has_flashlight)`, `!(n_confrontation & !has_hidden_drive)` | hold             | hold             |

Expected NuSMV verdict for every T1 spec: `... is true`.

---

## 2. Negative models (T4) — at least one counterexample required

### T4-01 — Unreachable GOOD ending

The GOOD ending `ending_save_everyone` is reachable only via a choice guarded by
`has_bunker_key`, and `bunker_key` is never granted by any action.

| Property                              | Verdict   | Why                                             |
|---------------------------------------|-----------|-------------------------------------------------|
| P1 `EF node = n_ending_save_everyone` | **false** | guard never satisfiable                         |
| P1 `EF node = n_ending_storm`         | true      | BAD ending reachable                            |
| P2 `AG (!is_accepting -> can_move)`   | true      | no dead-end (cliff always has the "watch" move) |
| P4 `AG EF is_accepting`               | true      | the BAD ending stays reachable everywhere       |
| P5 `EF is_good_ending`                | **false** | the only GOOD ending is unreachable             |

Analysis: a structurally valid, EVL-clean story can still be **unwinnable**. P1 pinpoints the
dead ending; P5 confirms no good outcome exists. Fix: add an `ADD_ITEM bunker_key` action on
a reachable edge before the guarded choice.

---

### T4-02 — Softlock / Deadlock  *(actual NuSMV run)*

Entering `dark_room` is free, but its only exit is guarded by `has_exit_code`, which is never
granted. Once inside, no transition is ever enabled.

| Property                                 | Verdict    | Why                                                                                                                                    |
|------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------|
| P1 `EF node = n_ending_escape`           | **false**  | the only edge into `ending_escape` is guarded by `has_exit_code`, which is never granted, so the ending is never reached               |
| P2 `AG (!is_accepting -> can_move)`      | **false**  | counterexample trace `corridor -> dark_room` ends in `node = n_dark_room` with `can_move = FALSE` while not accepting — a hard dead-end |
| P3 `!(n_ending_escape & !has_exit_code)` | true       | holds: `n_ending_escape` is never entered, so the forbidden state never occurs                                                |
| P4 `AG EF is_accepting`                  | **false**  | from the reachable `dark_room` state no accepting ending is ever reachable                                                             |
| P5 `EF is_good_ending`                   | **false**  | the only route to a GOOD ending passes through the stuck `dark_room`, so no good ending is

Analysis: the **defining** counterexample is P2, spec 2 — the trace `corridor -> dark_room`
ends in a state with `can_move = FALSE` while `is_accepting = FALSE`: a player permanently
stuck in a non-ending scene. P1/P4/P5 fail as consequences of the same dead-end.
