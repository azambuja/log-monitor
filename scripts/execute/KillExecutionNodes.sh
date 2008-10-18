#!/bin/sh
j=1 
        while [ $j -le 10 ] 
        do
        	ssh openbus@d$j  "killall -9 java"
                j=`expr $j + 1`
        done

