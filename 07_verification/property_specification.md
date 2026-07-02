# StoryFlowDSL — Phase 7 Property Specification

**Verification tool:** NuSMV 2.7.1 (CTL / LTL model checking)

This document lists every property the generator emits, in natural language and in the
tool's formal notation.

---

## Properties

### P1 — Reachability of endings

**Natural language:** Every declared ending must be reachable from
the start node. A declared ending that no play-through can reach is a dead branch.

**Formal (one spec per accepting node):**

```
CTLSPEC EF node = n_<endingId>;
```

---

### P2 — No deadlock / no softlock before an ending

**Natural language:** From every reachable non-ending state the player must have at least
one move available. A non-ending scene with no enabled outgoing transition is a hard
dead-end where the player is stuck.

**Formal:**

```
CTLSPEC AG (!is_accepting -> can_move);
```

---

### P3 — Locked-scene safety (item-gating invariant)

**Natural language:** A scene that is reachable **only** through item-guarded edges must
never be occupied without that item.

**Formal (one spec per uniformly item-locked scene):**

```
INVARSPEC !(node = n_<sceneId> & !has_<itemId>);
```

If no scene is exclusively item-locked, the generator emits `CTLSPEC AG TRUE;` instead.

---

### P4 — No unintended infinite loops

**Natural language:** Intended navigation loops are allowed, but from **every** reachable
state some accepting ending must still be reachable. A loop the player can never leave (a
livelock) violates this even though the player can keep moving.

**Formal:**

```
CTLSPEC AG EF is_accepting;
```

---

### P5 — A good ending is reachable

**Natural language:** The story must be winnable: at least one `GOOD` ending is reachable
from the start.

**Formal:**

```
CTLSPEC EF is_good_ending;
```

If the model declares no `GOOD` ending, the generator emits `CTLSPEC EF FALSE;` so the
absence is reported as a (deliberate) failure.

---
