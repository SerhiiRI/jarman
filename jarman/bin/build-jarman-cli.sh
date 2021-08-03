#!/bin/bash

# TEST APPLICATION EXISTS

if ! command -v java &> /dev/null
then
    echo "Error! 'Java 11+' not found"
    exit
fi

if ! command -v zip &> /dev/null
then
    echo "Error! 'zip' not found"
    exit
fi

# PARAMETERS

CLI_TMP_PATH="target/jarman-box-builder"
CLI_JAR="./target/uberjar/jarman-cli.jar"
CLI_ZIP_NAME="jarman-box"

DOT_JARMAN_DATA=".jarman.data.clj"
DJD_1="src/jarman/managment/$DOT_JARMAN_DATA"
DJD_2="./$DOT_JARMAN_DATA"
DJD_3="~/$DOT_JARMAN_DATA"

if [ -f "$CLI_JAR" ]; then
    echo "Copy '$CLI_JAR'"
    mkdir -p $CLI_TMP_PATH/bin
    cp ./target/uberjar/jarman-cli.jar $CLI_TMP_PATH/bin/
    touch $CLI_TMP_PATH/jarman

    if [ -f "$DJD_1" ]; then
	echo "Copy '$DJD_1'"
	cp "$DJD_1" $CLI_TMP_PATH
    elif [ -f "$DJD_2" ]; then
	echo "Copy '$DJD_2'"
	cp "$DJD_2" $CLI_TMP_PATH
    elif [ -f "$DJD_3" ]; then
	echo "Copy '$DJD_3'"
	cp "$DJD_3" $CLI_TMP_PATH
    else
	echo "Error! Can't locate '$DOT_JARMAN_DATA' file"
	exit 1
    fi
    
    echo "#!/bin/bash" > "$CLI_TMP_PATH/jarman";
    echo "java -jar bin/jarman-cli.jar \$@" >> "$CLI_TMP_PATH/jarman";
    chmod +x $CLI_TMP_PATH/jarman
    echo "Create 'jarman'";

    echo "Create '${CLI_ZIP_NAME}.zip'";
    zip -q -r "target/${CLI_ZIP_NAME}.zip" "${CLI_TMP_PATH}/"

    # REMOVE all
    # rm "target/${CLI_ZIP_NAME}.zip"
    # rm "${CLI_TMP_PATH}"
else
    echo "Error! Zip cannot be created. File '$CLI_JAR' does not exists'"
    exit 1
fi

echo "done."


