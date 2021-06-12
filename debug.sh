#!/bin/bash

./gradlew clean debugJar

version=1.16.5
plugins=(
    'https://github.com/monun/kotlin-plugin/releases/latest/download/Kotlin-1.5.10.jar'
    'https://github.com/dmulloy2/ProtocolLib/releases/latest/download/ProtocolLib.jar'
)

script=$(basename "$0")
server_folder=".${script%.*}"
mkdir -p "$server_folder"

cd "$server_folder"

server_script="paper.sh"
server_config="$server_script.conf"
wget -qc -N "https://raw.githubusercontent.com/monun/server-script/master/paper/paper.sh"

if [ ! -f "$server_config" ]
then
    cat << EOF > $server_config
version=$version
debug=true
debug_port=5005
backup=false
restart=false
memory=16
plugins=(
EOF
    for plugin in "${plugins[@]}"
    do
        echo "  \"$plugin\"" >> $server_config
    done
    echo ")" >> $server_config
fi

chmod +x ./$server_script
./$server_script