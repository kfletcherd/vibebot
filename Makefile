SRC_DIR  := src
OUT_DIR  := out
CLASSES  := $(OUT_DIR)/classes
JAR_FILE := $(OUT_DIR)/vibebot.jar
MAIN_CLASS := app.Main

SOURCES := $(shell find $(SRC_DIR) -name "*.java")

.DEFAULT_GOAL := run

# Compile all Java source files to out/classes/
compile: $(SOURCES)
	mkdir -p $(CLASSES)
	javac -d $(CLASSES) $(SOURCES)

# Package compiled classes into a runnable JAR
jar: compile
	jar --create \
	    --file $(JAR_FILE) \
	    --main-class $(MAIN_CLASS) \
	    -C $(CLASSES) .

# Compile, package, and launch the application
run: jar
	java -jar $(JAR_FILE)

# Remove all generated output
clean:
	rm -rf $(OUT_DIR)

.PHONY: compile jar run clean
