# Java Coder Lint Rules

These rules are enforced on all code written by the `java-coder` agent. Read this file at the start of every task alongside the feedback log.

---

## Indentation: tabs, not spaces

Always use a single tab character for indentation. Never use spaces to indent. This applies to all Java source files without exception.

## One tab per indentation level

Each new indentation level adds exactly one tab. Never add two tabs to represent a single level increase. The nesting depth is always exactly equal to the number of leading tab characters on a line.

## Closing delimiters on their own line

When the contents of `()`, `{}`, or `[]` are split across multiple lines for readability, the closing delimiter must appear on its own dedicated line, aligned to the same indentation level as the line that opened it.

```java
// Correct
doSomething(
	argOne,
	argTwo,
	argThree
);

List<String> items = List.of(
	"alpha",
	"beta",
	"gamma"
);

// Wrong — closing delimiter on the last content line
doSomething(
	argOne,
	argTwo,
	argThree);
```

## No vertical alignment padding

Never use extra spaces to vertically align `=` signs, type names, or any other tokens. Use a single space only between each token. This applies to field declarations, variable declarations, assignments, and any other context where values could be made to line up visually.

```java
// Correct
static final Color BACKGROUND = new Color(18, 18, 18);
static final Color SURFACE = new Color(30, 30, 30);
static final Color TEXT = new Color(220, 220, 220);

// Wrong — extra spaces for alignment
static final Color BACKGROUND  = new Color(18, 18, 18);
static final Color SURFACE     = new Color(30, 30, 30);
static final Color TEXT        = new Color(220, 220, 220);
```

## No vertical alignment padding in docblock comments

The same no-alignment rule applies inside `/** ... */` Javadoc blocks. Do not pad `@param`, `@return`, `@throws`, or `@since` tag descriptions with extra spaces to make them line up. Use a single space after the parameter name.

```java
// Correct
/**
 * @param navItem the selected item
 * @param viewId the card key
 * @return the registered component
 */

// Wrong — padded to align
/**
 * @param navItem  the selected item
 * @param viewId   the card key
 * @return         the registered component
 */
```
