package storyflow.design;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Java services used by the StoryFlow Sirius modeler.
 *
 * The implementation is reflective on purpose: it does not import the generated
 * storyflow_hll classes. This keeps the design plug-in compilable as long as the
 * StoryFlow metamodel is available at runtime.
 */
public class Services {

    // =====================================================================
    // Visibility helpers for Sirius popup menu items
    // =====================================================================

    public boolean canEditChoiceDetails(EObject self) {
        return findContainingChoice(self) != null;
    }

    public boolean canAddEffect(EObject self) {
        return findContainingChoice(self) != null;
    }

    public boolean canRemoveThisEffect(EObject self) {
        return self != null && "Effect".equals(self.eClass().getName());
    }

    public boolean canRemoveCondition(EObject self) {
        EObject choice = findContainingChoice(self);
        return choice != null && getEObject(choice, "condition") != null;
    }

    public boolean canRemoveDiceRule(EObject self) {
        EObject choice = findContainingChoice(self);
        return choice != null && (getEObject(choice, "dicerule") != null || getEObject(choice, "diceRule") != null);
    }

    public boolean canClearEffects(EObject self) {
        EObject choice = findContainingChoice(self);
        return choice != null && effectsOf(choice) != null && !effectsOf(choice).isEmpty();
    }

    // =====================================================================
    // Pop-up edit dialogs
    // =====================================================================

    /**
     * Open a dialog to create / edit / remove the Condition owned by the
     * selected Choice. Empty input removes the condition.
     */
    public EObject editCondition(EObject self) {
        EObject choice = findContainingChoice(self);
        if (choice == null) {
            warn("Edit Condition", "Please select a Choice edge, or one of its condition/effect/dice nodes.");
            return self;
        }

        EReference conditionRef = reference(choice, "condition");
        if (conditionRef == null) {
            warn("Edit Condition", "The selected Choice has no 'condition' reference.");
            return self;
        }

        EObject condition = (EObject) choice.eGet(conditionRef);
        String current = condition == null ? "" : getText(condition, "expression");
        String initial = current == null || current.trim().isEmpty() ? "true" : current;

        String value = ask("Edit Condition",
                "Condition expression. Leave empty to remove the condition.",
                initial);

        if (value == null) {
            return self; // cancelled
        }

        value = value.trim();
        if (value.isEmpty()) {
            choice.eUnset(conditionRef);
            return choice;
        }

        if (condition == null) {
            condition = EcoreUtil.create(conditionRef.getEReferenceType());
            choice.eSet(conditionRef, condition);
        }

        setFromString(condition, "expression", value);
        return choice;
    }

    public EObject removeCondition(EObject self) {
        EObject choice = findContainingChoice(self);
        if (choice == null) {
            return self;
        }
        EReference conditionRef = reference(choice, "condition");
        if (conditionRef != null) {
            choice.eUnset(conditionRef);
        }
        return choice;
    }

    /**
     * Open a dialog to create / edit / remove the DiceRule owned by the
     * selected Choice. Expected format: min,max,successThreshold.
     * Empty input removes the dice rule.
     */
    public EObject editDiceRule(EObject self) {
        EObject choice = findContainingChoice(self);
        if (choice == null) {
            warn("Edit Dice Rule", "Please select a Choice edge, or one of its condition/effect/dice nodes.");
            return self;
        }

        EReference diceRef = reference(choice, "dicerule", "diceRule");
        if (diceRef == null) {
            warn("Edit Dice Rule", "The selected Choice has no 'dicerule' reference.");
            return self;
        }

        EObject diceRule = (EObject) choice.eGet(diceRef);
        String current = "1,6,4";
        if (diceRule != null) {
            current = getText(diceRule, "min") + "," + getText(diceRule, "max") + ","
                    + getText(diceRule, "successThreshold");
        }

        String value = ask("Edit Dice Rule",
                "Use format: min,max,successThreshold. Example: 1,6,4. Leave empty to remove.",
                current);
        if (value == null) {
            return self; // cancelled
        }

        value = value.trim();
        if (value.isEmpty()) {
            choice.eUnset(diceRef);
            return choice;
        }

        String[] parts = value.split("[,;\\s]+");
        if (parts.length != 3) {
            warn("Edit Dice Rule",
                    "Invalid format. Use exactly three numbers: min,max,successThreshold. Example: 1,6,4");
            return self;
        }

        try {
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            int successThreshold = Integer.parseInt(parts[2]);

            if (min >= max) {
                warn("Edit Dice Rule", "Invalid dice range: min must be lower than max.");
                return self;
            }
            if (successThreshold < min || successThreshold > max) {
                warn("Edit Dice Rule", "The success threshold should be inside the dice range.");
                return self;
            }

            if (diceRule == null) {
                diceRule = EcoreUtil.create(diceRef.getEReferenceType());
                choice.eSet(diceRef, diceRule);

                Object defaultTarget = getEObject(choice, "target");
                if (defaultTarget instanceof EObject) {
                    EReference onSuccess = reference(diceRule, "onSuccess");
                    if (onSuccess != null && diceRule.eGet(onSuccess) == null) {
                        diceRule.eSet(onSuccess, defaultTarget);
                    }
                    EReference onFailure = reference(diceRule, "onFailure");
                    if (onFailure != null && diceRule.eGet(onFailure) == null) {
                        diceRule.eSet(onFailure, defaultTarget);
                    }
                }
            }

            setFromString(diceRule, "min", Integer.toString(min));
            setFromString(diceRule, "max", Integer.toString(max));
            setFromString(diceRule, "successThreshold", Integer.toString(successThreshold));
        } catch (NumberFormatException ex) {
            warn("Edit Dice Rule", "All dice rule values must be integers.");
        }

        return choice;
    }

    public EObject removeDiceRule(EObject self) {
        EObject choice = findContainingChoice(self);
        if (choice == null) {
            return self;
        }
        EReference diceRef = reference(choice, "dicerule", "diceRule");
        if (diceRef != null) {
            choice.eUnset(diceRef);
        }
        return choice;
    }

    /**
     * Append a single Effect to the selected Choice.
     *
     * Supported inputs:
     *   ADD_ITEM=key
     *   REMOVE_ITEM=key
     *   SET_VARIABLE=visitedForest:true
     *   SET_VARIABLE=visitedForest      (sets the variable to "true")
     */
    @SuppressWarnings("unchecked")
    public EObject addEffect(EObject self) {
        EObject choice = findContainingChoice(self);
        if (choice == null) {
            warn("Add Effect", "Please select a Choice edge, or one of its condition/effect/dice nodes.");
            return self;
        }

        EReference effectsRef = reference(choice, "effects");
        if (effectsRef == null) {
            warn("Add Effect", "The selected Choice has no 'effects' containment reference.");
            return self;
        }

        Object raw = choice.eGet(effectsRef);
        if (!(raw instanceof EList<?>)) {
            warn("Add Effect", "The 'effects' reference is not a list.");
            return self;
        }

        String value = ask(
                "Add Effect",
                "Format examples:\n"
                        + "ADD_ITEM=key\n"
                        + "REMOVE_ITEM=key\n"
                        + "SET_VARIABLE=visitedForest:true\n"
                        + "SET_VARIABLE=visitedForest",
                "ADD_ITEM=");
        if (value == null) {
            return self; // cancelled
        }
        value = value.trim();
        if (value.isEmpty()) {
            return self;
        }

        EObject effect = parseEffect(choice, effectsRef, value, "Add Effect");
        if (effect != null) {
            ((EList<EObject>) raw).add(effect);
        }
        return choice;
    }

    /**
     * Replace all effects owned by the selected Choice.
     * Format: TYPE=value; TYPE=value
     */
    @SuppressWarnings("unchecked")
    public EObject editEffects(EObject self) {
        EObject choice = findContainingChoice(self);
        if (choice == null) {
            warn("Replace All Effects", "Please select a Choice edge, or one of its condition/effect/dice nodes.");
            return self;
        }

        EReference effectsRef = reference(choice, "effects");
        if (effectsRef == null) {
            warn("Replace All Effects", "The selected Choice has no 'effects' reference.");
            return self;
        }

        Object raw = choice.eGet(effectsRef);
        if (!(raw instanceof EList<?>)) {
            warn("Replace All Effects", "The 'effects' reference is not a list.");
            return self;
        }

        EList<EObject> effects = (EList<EObject>) raw;
        String current = effectsToText(effects);
        String value = ask(
                "Replace All Effects",
                "REPLACES ALL effects on this choice.\n"
                        + "Use: TYPE=value; TYPE=value\n"
                        + "Examples: ADD_ITEM=key; REMOVE_ITEM=coin; SET_VARIABLE=visitedForest:true\n"
                        + "Leave empty to clear all.",
                current);

        if (value == null) {
            return self; // cancelled
        }

        value = value.trim();
        if (value.isEmpty()) {
            effects.clear();
            return choice;
        }

        // Parse first, then replace. This avoids clearing valid existing effects
        // if the new input contains a mistake.
        List<EObject> parsed = new ArrayList<>();
        for (String entry : value.split(";")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            EObject effect = parseEffect(choice, effectsRef, trimmed, "Replace All Effects");
            if (effect != null) {
                parsed.add(effect);
            } else {
                return self;
            }
        }

        effects.clear();
        effects.addAll(parsed);
        return choice;
    }

    public EObject clearEffects(EObject self) {
        EObject choice = findContainingChoice(self);
        if (choice == null) {
            return self;
        }
        EList<EObject> effects = effectsOf(choice);
        if (effects != null) {
            effects.clear();
        }
        return choice;
    }

    /**
     * Remove a single Effect from its parent Choice's effects list.
     * Bound to the "Remove This Effect" popup item that appears when the
     * user right-clicks an Effect node.
     */
    @SuppressWarnings("unchecked")
    public EObject removeThisEffect(EObject self) {
        if (self == null || !"Effect".equals(self.eClass().getName())) {
            warn("Remove This Effect", "Please right-click an Effect node.");
            return self;
        }

        EObject parent = self.eContainer();
        if (parent == null || !isChoice(parent)) {
            warn("Remove This Effect", "This Effect is not contained in a Choice.");
            return self;
        }

        EReference effectsRef = reference(parent, "effects");
        if (effectsRef == null) {
            return self;
        }

        Object raw = parent.eGet(effectsRef);
        if (raw instanceof EList<?>) {
            ((EList<EObject>) raw).remove(self);
        }
        return parent;
    }

    private EObject parseEffect(EObject choice, EReference effectsRef, String text, String dialogTitle) {
        String[] parts = text.split("=", 2);
        if (parts.length != 2) {
            warn(dialogTitle, "Invalid effect '" + text + "'. Use TYPE=value.");
            return null;
        }

        String typeText = parts[0].trim();
        String effectValue = parts[1].trim();

        if (!("ADD_ITEM".equals(typeText) || "REMOVE_ITEM".equals(typeText) || "SET_VARIABLE".equals(typeText))) {
            warn(dialogTitle,
                    "Unknown effect type '" + typeText + "'. Use ADD_ITEM, REMOVE_ITEM, or SET_VARIABLE.");
            return null;
        }

        EObject effect = EcoreUtil.create(effectsRef.getEReferenceType());
        if (!setFromString(effect, "type", typeText)) {
            warn(dialogTitle, "Could not set effect type '" + typeText + "'.");
            return null;
        }

        if ("SET_VARIABLE".equals(typeText)) {
            String variableName = effectValue;
            String assignedValue = "true";

            int separator = effectValue.indexOf(':');
            if (separator >= 0) {
                variableName = effectValue.substring(0, separator).trim();
                assignedValue = effectValue.substring(separator + 1).trim();
            }

            EObject variable = findStoryChildByNameOrId(choice, "variables", variableName);
            if (variable != null) {
                setReference(effect, "targetVar", variable);
            }
            setFromString(effect, "value", assignedValue.isEmpty() ? "true" : assignedValue);
        } else {
            EObject item = findStoryChildByNameOrId(choice, "items", effectValue);
            if (item != null) {
                setReference(effect, "targetItem", item);
            }
            setFromString(effect, "value", effectValue);
        }

        return effect;
    }

    // =====================================================================
    // Label helpers
    // =====================================================================

    /** Multi-line label for a Scene: name plus optional ending-type suffix. */
    public String sceneLabel(EObject scene) {
        if (scene == null) {
            return "";
        }
        String name = getText(scene, "name");
        boolean isEnding = Boolean.parseBoolean(getText(scene, "isEnding"));
        if (isEnding) {
            String type = getText(scene, "endingType");
            if (type != null && !type.trim().isEmpty()) {
                return name + "\n[" + type + "]";
            }
        }
        return name;
    }

    /** Compact edge label for a Choice. */
    public String choiceLabel(EObject choice) {
        if (choice == null) {
            return "";
        }

        String label = getText(choice, "label");
        StringBuilder sb = new StringBuilder(label == null ? "" : label);

        EObject condition = (EObject) getEObject(choice, "condition");
        if (condition != null) {
            String expr = getText(condition, "expression");
            if (expr != null && !expr.trim().isEmpty()) {
                sb.append("  [").append(expr).append("]");
            }
        }

        EObject dice = (EObject) getEObject(choice, "dicerule");
        if (dice == null) {
            dice = (EObject) getEObject(choice, "diceRule");
        }
        if (dice != null) {
            sb.append("  d").append(getText(dice, "min")).append("-").append(getText(dice, "max"))
                    .append("\u2265").append(getText(dice, "successThreshold"));
        }

        return sb.toString();
    }

    /** End-label for a Choice edge: shows side-effects, if any. */
    public String effectsLabel(EObject choice) {
        EList<EObject> effects = effectsOf(choice);
        if (effects == null || effects.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (EObject effect : effects) {
            parts.add(effectText(effect));
        }
        return String.join(", ", parts);
    }

    private String effectText(EObject effect) {
        String type = getText(effect, "type");
        String value = getText(effect, "value");

        if ("ADD_ITEM".equals(type)) {
            EObject item = (EObject) getEObject(effect, "targetItem");
            return "+" + (item != null ? displayName(item) : value);
        }
        if ("REMOVE_ITEM".equals(type)) {
            EObject item = (EObject) getEObject(effect, "targetItem");
            return "-" + (item != null ? displayName(item) : value);
        }
        if ("SET_VARIABLE".equals(type)) {
            EObject var = (EObject) getEObject(effect, "targetVar");
            return (var != null ? displayName(var) : "?") + ":=" + value;
        }

        return "?";
    }

    // =====================================================================
    // Reflective helpers
    // =====================================================================

    /** Walk up the containment chain until a Choice is found (or null). */
    private EObject findContainingChoice(EObject object) {
        EObject current = object;
        while (current != null) {
            if (isChoice(current)) {
                return current;
            }
            current = current.eContainer();
        }
        return null;
    }

    private boolean isChoice(EObject object) {
        return object != null && "Choice".equals(object.eClass().getName());
    }

    private EReference reference(EObject object, String... names) {
        EStructuralFeature feature = feature(object, names);
        return feature instanceof EReference ? (EReference) feature : null;
    }

    private EStructuralFeature feature(EObject object, String... names) {
        if (object == null) {
            return null;
        }
        EClass type = object.eClass();
        for (String name : names) {
            EStructuralFeature feature = type.getEStructuralFeature(name);
            if (feature != null) {
                return feature;
            }
        }
        return null;
    }

    private String getText(EObject object, String featureName) {
        EStructuralFeature feature = feature(object, featureName);
        if (feature == null) {
            return "";
        }
        Object value = object.eGet(feature);
        return value == null ? "" : value.toString();
    }

    private Object getEObject(EObject object, String featureName) {
        EStructuralFeature feature = feature(object, featureName);
        if (feature == null) {
            return null;
        }
        return object.eGet(feature);
    }

    private boolean setFromString(EObject object, String featureName, String text) {
        EStructuralFeature feature = feature(object, featureName);
        if (feature == null || !feature.isChangeable()) {
            return false;
        }

        try {
            EClassifier type = feature.getEType();
            Object value = text;
            if (type instanceof EDataType) {
                value = EcoreUtil.createFromString((EDataType) type, text);
            }
            object.eSet(feature, value);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void setReference(EObject object, String featureName, EObject target) {
        EReference ref = reference(object, featureName);
        if (ref != null && ref.isChangeable()) {
            object.eSet(ref, target);
        }
    }

    @SuppressWarnings("unchecked")
    private EList<EObject> effectsOf(EObject choice) {
        if (choice == null) {
            return null;
        }
        EReference effectsRef = reference(choice, "effects");
        if (effectsRef == null) {
            return null;
        }
        Object raw = choice.eGet(effectsRef);
        return raw instanceof EList<?> ? (EList<EObject>) raw : null;
    }

    private String effectsToText(EList<EObject> effects) {
        List<String> parts = new ArrayList<>();
        for (EObject effect : effects) {
            String type = getText(effect, "type");
            if ("SET_VARIABLE".equals(type)) {
                EObject var = (EObject) getEObject(effect, "targetVar");
                String name = var != null ? displayName(var) : "";
                String value = getText(effect, "value");
                parts.add("SET_VARIABLE=" + name + ":" + value);
            } else if ("ADD_ITEM".equals(type) || "REMOVE_ITEM".equals(type)) {
                EObject item = (EObject) getEObject(effect, "targetItem");
                String target = item != null ? displayName(item) : getText(effect, "value");
                parts.add(type + "=" + target);
            } else {
                parts.add(type + "=" + getText(effect, "value"));
            }
        }
        return String.join("; ", parts);
    }

    private EObject findStory(EObject object) {
        EObject current = object;
        EObject last = object;
        while (current != null) {
            last = current;
            if ("Story".equals(current.eClass().getName())) {
                return current;
            }
            current = current.eContainer();
        }
        return last;
    }

    @SuppressWarnings("unchecked")
    private EObject findStoryChildByNameOrId(EObject context, String featureName, String wanted) {
        if (wanted == null || wanted.trim().isEmpty()) {
            return null;
        }

        EObject story = findStory(context);
        EStructuralFeature feature = feature(story, featureName);
        if (feature == null) {
            return null;
        }

        Object raw = story.eGet(feature);
        if (!(raw instanceof EList<?>)) {
            return null;
        }

        String needle = wanted.trim();
        for (EObject child : (EList<EObject>) raw) {
            if (needle.equals(getText(child, "name")) || needle.equals(getText(child, "id"))) {
                return child;
            }
        }

        return null;
    }

    private String displayName(EObject object) {
        String name = getText(object, "name");
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        String id = getText(object, "id");
        return id == null ? "" : id;
    }

    private String ask(String title, String message, String initialValue) {
        final String[] result = new String[] { null };
        Runnable openDialog = () -> {
            Shell shell = activeShell();
            InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
            if (dialog.open() == Window.OK) {
                result[0] = dialog.getValue();
            }
        };

        Display display = Display.getDefault();
        if (display == null) {
            return null;
        }
        if (display.getThread() == Thread.currentThread()) {
            openDialog.run();
        } else {
            display.syncExec(openDialog);
        }
        return result[0];
    }

    private void warn(String title, String message) {
        Runnable openDialog = () -> MessageDialog.openWarning(activeShell(), title, message);
        Display display = Display.getDefault();
        if (display == null) {
            return;
        }
        if (display.getThread() == Thread.currentThread()) {
            openDialog.run();
        } else {
            display.asyncExec(openDialog);
        }
    }

    private Shell activeShell() {
        Display display = Display.getDefault();
        return display == null ? null : display.getActiveShell();
    }
}
