---
name: alchemancy-api
description: Reference for the base Alchemancy mod's Java API (net.cibernet.alchemancy) that this addon (AlchemancyStuck) builds on - the Property class, its hook methods, the AlchemancyProperties registry, InfusedPropertiesHelper, and the addon's own conventions for wiring new properties, grist-cost mixins, and codex/lang entries. Use whenever adding, modifying, or reasoning about a Property subclass, registering it, or touching anything under net.cibernet.alchemancy.
user-invocable: true
---

# Alchemancy API Reference

AlchemancyStuck (`org.wilkretawesomesauce.alchemancystuck`) is a NeoForge 1.21.1 addon for the
base mod **Alchemancy** (`net.cibernet.alchemancy`) and integrates with **Minestuck**
(`com.mraof.minestuck`). Alchemancy isn't on this machine as source inside the repo - it's a
compiled dependency at `libs/alchemancy-${alchemancy_version}-NeoForge-${minecraft_version}.jar`
(version pinned in `gradle.properties` as `alchemancy_version`, currently `2.1.3`). No source/javadoc
jar is attached.

A full decompiled/source checkout of Alchemancy has been found locally at
`C:\Users\Wilkret\Downloads\Alchemancy-master\alchemancy\src\main\java\net\cibernet\alchemancy\`.
Treat that path as a **machine-local convenience, not a guaranteed fixture** - it may not exist in
a future session/machine, and its version may drift from `alchemancy_version` in this repo. If it's
present, prefer reading real source there over guessing; if a method/field you recall from it turns
out not to exist against the actual jar, trust the jar (`javap`, or decompile it) over memory.

## The `Property` class

`net.cibernet.alchemancy.properties.Property` is an abstract class with ~80 overridable no-op
hook methods covering almost every relevant game event. You don't need to memorize them - grep the
downloaded source's `Property.java` for the event you need - but the broad categories are:

- **Combat**: `onAttack`, `onDamageReceived`, `onIncomingAttack`, `onIncomingDamageReceived`,
  `modifyCriticalAttack`/`onCriticalAttack`, `modifyKnockBackReceived`/`modifyKnockBackApplied`,
  `onKill`, `onUserDeath`, `modifyEnchantmentValue`
- **Equip/tick**: `onEquippedTick`, `onInventoryTick`, `onJump`, `onFall`, `applyAttributes`
  (`ItemAttributeModifierEvent`), `modifyWearableSlot`
- **Interaction**: `onRightClickItem`/`onRightClickItemPost`, `onRightClickBlock`,
  `onRightClickEntity`, `onActivation`/`shouldActivate` (the "on-hit-or-right-click" activation
  path used by many trigger properties)
- **Item lifecycle**: `consumeItem`, `damageItem`/`damageOrConsumeItem`,
  `modifyDurabilityConsumed`, `onItemRepaired`, `modifyDataComponent`
- **Projectiles**: `onProjectileTick`, `onProjectileImpact`, `allowArrowClipBlocks`
- **Rooted (planted) items**: `onRootedTick`, `onRootedAnimateTick`, `onRootedRightClick`
- **Display**: `getColor(ItemStack)` (abstract, required), `getDisplayText`, `getName`,
  `getLanguageKey` (defaults to `"property." + getKey().toLanguageKey()"`)
- **Priority**: `getPriority()` - see `Property.Priority` constants
  (`LOWEST`/`LOWER`/`LOW`/`NORMAL`(default)/`HIGH`/`HIGHER`/`HIGHEST`, `HIGHEST` = `Integer.MIN_VALUE`
  so it runs first)

`Property.simple(int color)` / `simpleSine` / `simpleFlashing` / `simpleInterpolated` are factory
helpers for trivial color-only properties that don't need a dedicated subclass - the base mod uses
these liberally for cosmetic-only properties (see `CHARRED`, `MUFFLED`, `FLIMSY`, etc. in
`AlchemancyProperties`).

## Registering a property

Base mod pattern (`net.cibernet.alchemancy.registries.AlchemancyProperties`):
```java
public static final DeferredRegister<Property> REGISTRY = DeferredRegister.create(KEY, Alchemancy.MODID);
public static final DeferredHolder<Property, MyProperty> MY_PROP = REGISTRY.register("my_prop", MyProperty::new);
```

This addon's own registry lives at
`src/main/java/org/wilkretawesomesauce/alchemancystuck/registry/AddonProperties.java`, keyed off
`AlchemancyProperties.REGISTRY_KEY` so addon properties land in the same registry as base-mod ones:
```java
public static final DeferredRegister<Property> PROPERTIES =
    DeferredRegister.create(AlchemancyProperties.REGISTRY_KEY, Alchemancystuck.MODID);
public static final DeferredHolder<Property, Property> FOO = PROPERTIES.register("foo", FooProperty::new);
```
`AddonProperties.PROPERTIES.register(modEventBus)` is wired in `Alchemancystuck`'s constructor.
**Adding a new `properties/*.java` class is not enough** - it has no effect until it's also added
here. As of the last check, `AlchemizingProperty`, `CaptchaloguingProperty`, `MagicalProperty`,
`RageInducingProperty`, `CruxiteProperty`, `ArtifactProperty` are registered; `ShittyProperty`,
`BadassProperty`, `FeshPinceProperty` are still empty stubs that don't even `extend Property` yet -
don't assume a class under `properties/` is live without checking `AddonProperties.java`.

## `InfusedPropertiesHelper` (net.cibernet.alchemancy.item.components)

This is how you query/mutate what properties an `ItemStack` has - almost every addon `Property`
override needs it.

- `hasProperty(ItemStack, Holder<Property>)` - the one to use in general code. True if the stack has
  the property **infused**, or **innate** (subject to the Dead gotcha below), or **dormant+Awakened**.
- `hasInfusedProperty` / `hasInnateProperty` / `hasDormantProperty` - check one specific source only.
- `forEachProperty(stack, consumer)` - iterates every property currently "active" on the stack
  (infused + innate-unless-dead + dormant-if-awakened), respecting the Toggleable property.
- `addProperty` / `addProperties` / `removeProperty` - mutate the **infused** component.
- `storeProperties` / `createPropertyCapsule` / `createPropertyIngredient` - build a Property
  Capsule item pre-loaded with properties (used for creative-tab entries, loot, etc.).
- `getInfusionSlots` / `getRemainingInfusionSlots` / `truncateProperties` - infusion slot bookkeeping.

This addon's actual usage pattern (`alchemy/AlchemiterCostHelper.java`):
```java
if (InfusedPropertiesHelper.hasProperty(output, AddonProperties.CRUXITE)) { ... }
```

### Gotcha: Dead's "erases everything" behavior is NOT just its `Property` overrides

`DeadProperty` itself only overrides `applyAttributes` (clears attribute modifiers),
`onRightClickItemPost`/`onRightClickEntity` (cancels + passes through), `getPriority()`
(`HIGHEST`), and `getColor`. That's the *entire* class - four small overrides.

The part of Dead that actually suppresses an item's other properties lives **outside** `Property`
entirely, hardcoded into `InfusedPropertiesHelper` by identity comparison against the `DEAD` holder:
- `hasProperty(stack, property)`: an **innate** property only counts as present if
  `property == AlchemancyProperties.DEAD || !hasProperty(stack, AlchemancyProperties.DEAD)`.
- `forEachProperty(...)`: skips iterating **innate** properties at all if
  `hasProperty(stack, AlchemancyProperties.DEAD)` is true.

Two consequences worth remembering:
1. This only suppresses **innate** properties (an item's built-in properties from its item type),
   not other **infused** properties on the same stack. Infusing Dead onto an item does not blank
   out its other infusions.
2. Because the check is a literal `== AlchemancyProperties.DEAD` / calls straight to the `DEAD`
   holder, no addon property can reproduce this suppression by overriding `Property` methods alone -
   that specific holder reference is baked into the base mod's helper class and isn't generic (e.g.
   not "any `Priority.HIGHEST` property" or a tag-driven check).
   `AlchemancyTags.DataComponents.DISABLED_BY_DEAD` is a related but separate, tag-driven mechanism
   (referenced in a commented-out `modifyDataComponent` override in `DeadProperty`, "moved to
   MixinUtils") for disabling specific data components, not properties.

**Reproducing it for an addon property anyway (done for `PerfectlyGenericProperty`)**: mixin
straight into `InfusedPropertiesHelper` and extend its choke points the same way the base mod
extends them for `DEAD`. See `mixin/InfusedPropertiesHelperMixin.java`:
- `@Inject` at `HEAD`, cancellable, on both `hasProperty` overloads (`Holder<Property>` and
  `TagKey<Property>`) - if the queried property/tag isn't Perfectly Generic itself and the stack
  *does* have Perfectly Generic, force-return `false`.
- `@Inject` at `HEAD`, cancellable, on `forEachProperty(ItemStack, Consumer, boolean)` - if the
  stack has Perfectly Generic, `consumer.accept` it alone and cancel, so no other property's hooks
  (`onEquippedTick`, `onAttack`, etc. - whatever event listener elsewhere drives them via this
  method) ever fire.
- Unlike real Dead, this suppresses infused/innate/dormant properties alike, since that's what was
  asked for here - Dead's own asymmetry (innate-only) wasn't the goal, just the technique.
- All three mixin targets were confirmed against the actual dependency jar with `javap -p -s` before
  trusting the method descriptors (`hasProperty(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Z`,
  the `TagKey` overload, and `forEachProperty(...;Z)V`) - always do this before writing a mixin
  target string from memory of the decompiled source, since descriptors must match exactly or the
  mixin silently fails to apply (or fails loudly at launch, depending on `injectors.defaultRequire`
  in `alchemancystuck.mixins.json`, which this repo has set to `1`, so a bad target should error out
  at game launch rather than silently no-op).

**Gotcha that actually shipped and crashed (`StackOverflowError` in-game, not caught by
`compileJava`)**: the "does the stack have Perfectly Generic" check inside the injected code must
**never** call the (mixed) `InfusedPropertiesHelper.hasProperty(...)` methods - build it from the
non-recursive primitives (`hasInfusedProperty`/`hasInnateProperty`/`hasDormantProperty`) instead. The
first version called `hasProperty(stack, PERFECTLY_GENERIC)` from inside the mixin, on the assumption
it would take the "property IS Perfectly Generic, don't suppress" branch and terminate after one extra
frame. It doesn't terminate: `hasProperty`'s own *original* body separately calls
`hasProperty(stack, AlchemancyProperties.DEAD)` as part of its innate-property check (vanilla Dead
logic, unrelated to our mixin), and since `DEAD != PERFECTLY_GENERIC`, *that* call re-enters our
suppression check, which calls back into `hasProperty(stack, PERFECTLY_GENERIC)`, which runs the
original body again, which checks `DEAD` again - an unbounded PERFECTLY_GENERIC&harr;DEAD cycle, not
the bounded one-level recursion it looked like on paper. It blew the stack on almost every
`hasProperty` call in practice (anything that isn't short-circuited by the infused check on the first
disjunct - i.e. most calls, most of the time). Mixin recursion-safety has to be reasoned through by
tracing what the *original, un-mixed* method body does internally, not just what your own injected
code does - a call that looks like it "obviously" terminates in one extra frame can still cycle if the
target method has its own internal self-calls for unrelated reasons.

This is the first mixin in this addon that targets Alchemancy's own classes rather than Minestuck's
- existing mixins only touch `com.mraof.minestuck.*`. No refmap/access-widener complications came up
(cross-mod mixin targets on non-obfuscated classes don't need remapping), consistent with how the
Minestuck-facing mixins already work here.

## Grist cost / Minestuck interop

`Minestuck-${minecraft_version}-${minestuck_version}.jar` (also under `libs/`) is the other
dependency. The addon's own grist-cost pattern, in
`alchemy/AlchemiterCostHelper.java` + `mixin/AlchemiterGristCostMixin.java` /
`mixin/MiniAlchemiterGristCostMixin.java`:
- Mixin into `AlchemiterBlockEntity`/`MiniAlchemiterBlockEntity`'s `getGristCost`
  (`@ModifyReturnValue`, `@At("RETURN")`) to post-process the `GristSet` Minestuck already computed.
- Inside, branch on `InfusedPropertiesHelper.hasProperty(output, AddonProperties.X)` and rescale via
  `MutableGristSet`/`GristAmount`/`GristType` (Minestuck's `com.mraof.minestuck.api.alchemy.*`).
- Applied in the order properties are checked, not simultaneously - e.g. Cruxite scales every
  `GristAmount` by `CruxiteProperty.GRIST_COST_SCALE` (0.65) *before* Artifact collapses the whole
  set into a single Artifact-grist total, so stacking both means Artifact sees the already-reduced
  total.

## Codex / lang conventions (this addon)

- Lang keys: `property.<modid>.<name>` (the property's display name) and
  `infusion_codex.<modid>:<name>.<subkey>` (flavor text + one key per mechanical blurb), in
  `assets/alchemancystuck/lang/en_us.json`.
- Codex entry JSON at `assets/alchemancystuck/alchemancy/codex_entries/<name>.json`:
  ```json
  { "flavor": { "translate": "infusion_codex.alchemancystuck:<name>.flavor" },
    "functions": ["change_grist_type"] }
  ```
  Each string in `"functions"` is a subkey that must have a matching
  `infusion_codex.<modid>:<name>.<subkey>` lang entry - it's how the in-game Alchemancer's Journal
  renders one bullet per mechanical effect.

## Tags (`net.cibernet.alchemancy.registries.AlchemancyTags`)

Nested nested by target type: `Items`, `Blocks`, `DamageTypes`, `EntityTypes`, `Dimensions`,
`Properties` (yes, tags *on properties themselves* - e.g. `SLOTLESS`, `DISABLED`,
`AFFECTED_BY_MAGNETIC`), `DataComponents`, `MobEffects`, `Enchantments`. Existing addon code already
uses `AlchemancyTags.DamageTypes.ARCANE_DAMAGE` (`MagicalProperty`) - check here before adding a new
tag if the effect you want ("this property is affected by X base-mod mechanic") might already be a
hook point the base mod exposes via a tag.
