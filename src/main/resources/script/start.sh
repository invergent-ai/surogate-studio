#!/bin/sh

printf " ▗▄▄▖▗▄▄▄▖▗▄▖▗▄▄▄▖▗▄▄▄▖▗▖  ▗▖▗▄▄▄▖ ▗▄▄▖▗▖ ▗▖"
printf "\n▐▌     █ ▐▌ ▐▌ █  ▐▌   ▐▛▚▞▜▌▐▌   ▐▌   ▐▌ ▐▌"
printf "\n ▝▀▚▖  █ ▐▛▀▜▌ █  ▐▛▀▀▘▐▌  ▐▌▐▛▀▀▘ ▝▀▚▖▐▛▀▜▌"
printf "\n▗▄▄▞▘  █ ▐▌ ▐▌ █  ▐▙▄▄▖▐▌  ▐▌▐▙▄▄▖▗▄▄▞▘▐▌ ▐▌"
printf "\n\n"
printf "Installing StateMesh: \n"
echo "---------------------"

if [ "$(id -u)" -ne 0 ]; then
    echo "You are not running as root."
    exit 1
fi

. /etc/os-release

if [ "$NAME" = "Ubuntu" ] && [ "$VERSION_ID" = "22.04" ]; then
    echo "Detected compatible OS..."
else
    echo "Only Ubuntu 22.04 is supported at this time."
    exit 1
fi

trap ' ' 2 15 20
curl -sfL https://register.statemesh.net | INSTALL_K3S_COMMIT=latest K3S_MESH_ID="[({smId})]" K3S_MESH_SERVER="[({smUrl})]" bash -s - agent --disable-apiserver-lb
trap - 2 15 20
