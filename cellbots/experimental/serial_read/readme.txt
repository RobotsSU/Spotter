This is the code that will allow python to read data from the serial port.

Instructions:

1.  start telnetd as root
2.  telnet to the ip address of the phone
3.  nohup sh cat_dev.sh &
4.  sh testfileread.sh

Simply send some data from the arduino to the phone and it should output to the
 file output.txt which you could tail to double check.  The python program polls
 this file for input.  I would like to use the formal approach of putting the
serial device /dev/ttyMSM2 in the select loop but android does not seem to 
support that so far.
