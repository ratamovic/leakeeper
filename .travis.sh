#!/bin/bash
# Originally written by Ralf Kistner <ralf@embarkmobile.com>, but placed in the public domain

case "$1" in
    wait_for_emulator)
        set +e

        bootanim=""
        failcounter=0
        until [[ "$bootanim" =~ "stopped" ]]; do
           bootanim=`adb -e shell getprop init.svc.bootanim 2>&1`
           echo "$bootanim"
           if [[ "$bootanim" =~ "not found" ]]; then
              let "failcounter += 1"
              if [[ $failcounter -gt 15 ]]; then
                echo "Failed to start emulator"
                exit 1
              fi
           fi
           sleep 1
        done
        echo "Done"
        exit 0
        ;;

    *)
        echo "Wrong parameters passed to travis.sh"
        exit 1
        ;;
esac
